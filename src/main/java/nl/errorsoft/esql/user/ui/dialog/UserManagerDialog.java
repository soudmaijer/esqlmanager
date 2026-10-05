package nl.errorsoft.esql.user.ui.dialog;

import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.util.Validation;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.ui.MainWindow;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import nl.errorsoft.esql.user.control.UserManagerController;
import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.user.GrantChange;
import nl.errorsoft.esql.user.GrantTarget;
import nl.errorsoft.esql.user.PrivilegeGroup;

/** Manages the accounts of the server and the privileges they have on the server, a database or a table. */
public class UserManagerDialog extends JDialog {
	private static final String LOADING = "Loading...";

	private final UserManagerController userManagerController;
	private final DefaultListModel<DatabaseUser> userModel = new DefaultListModel<>();
	private final JList<DatabaseUser> users = new JList<>(userModel);
	private final DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode(GrantTarget.global()));
	private final JTree tree = new JTree(treeModel);
	private final JPanel privilegePanel = new JPanel();
	private final List<JCheckBox> privilegeBoxes = new ArrayList<>();
	private final JTextField search = new JTextField(12);
	private final List<DatabaseUser> allUsers = new ArrayList<>();
	private final JPanel grantedPanel = Forms.titled(new JPanel(new BorderLayout(0, 6)), "Granted");
	private final JButton apply = Forms.button("Appl&y");
	private final JButton selectAll = Forms.button("Select a&ll");
	private final JButton selectNone = Forms.button("Select &none");
	private final JLabel message = new JLabel(" ");
	private final JButton changePassword = Forms.button("&Password...");
	private final JButton delete = Forms.button("&Drop");

	// What the privilege panel shows: the user and target it was filled for, and the privileges the server has granted.
	private DatabaseUser shownUser;
	private GrantTarget shownTarget;
	private TreePath shownPath;
	private Set<String> shownGrants = Set.of();
	/** True while the code changes the selection back, so that the listeners ignore it. */
	private boolean reverting;

	public UserManagerDialog(MainWindow owner, UserManagerController userManagerController) throws Exception {
		super(owner, "User manager", false);
		this.userManagerController = userManagerController;

		JPanel userPanel = new JPanel(new BorderLayout(0, 6));
		Forms.titled(userPanel, "Users");
		JPanel searchRow = new JPanel(new BorderLayout(Forms.GAP, 0));
		searchRow.add(Forms.label("&Find:", search), BorderLayout.WEST);
		searchRow.add(search, BorderLayout.CENTER);
		userPanel.add(searchRow, BorderLayout.NORTH);
		userPanel.add(new JScrollPane(users), BorderLayout.CENTER);
		userPanel.add(userButtons(), BorderLayout.SOUTH);

		JPanel privileges = new JPanel(new BorderLayout(0, 6));
		Forms.titled(privileges, "Privileges");
		privileges.add(new JScrollPane(tree), BorderLayout.CENTER);

		JPanel boxes = grantedPanel;
		privilegePanel.setLayout(new BoxLayout(privilegePanel, BoxLayout.Y_AXIS));
		JPanel selectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		selectRow.add(selectAll);
		selectRow.add(selectNone);
		// Keeps the checkboxes together at the top instead of spreading them over the height.
		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.add(selectRow, BorderLayout.NORTH);
		top.add(privilegePanel, BorderLayout.CENTER);
		boxes.add(top, BorderLayout.NORTH);
		boxes.add(Forms.buttonRow(apply), BorderLayout.SOUTH);

		JSplitPane right = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, privileges, boxes);
		right.setResizeWeight(0.5);
		JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userPanel, right);
		main.setResizeWeight(0.25);
		main.setBorder(BorderFactory.createEmptyBorder(Forms.PADDING, Forms.PADDING, 0, Forms.PADDING));

		message.setBorder(BorderFactory.createEmptyBorder(Forms.GAP, Forms.PADDING, Forms.GAP, Forms.PADDING));
		getContentPane().add(main, BorderLayout.CENTER);
		getContentPane().add(message, BorderLayout.SOUTH);

		initTree();
		users.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		users.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && !reverting) {
				if (hasUnappliedChanges() && !confirmDiscard()) {
					reverting = true;
					users.setSelectedValue(shownUser, true);
					reverting = false;
					return;
				}
				userSelected();
			}
		});
		tree.addTreeSelectionListener((TreeSelectionEvent e) -> {
			if (reverting) {
				return;
			}
			if (hasUnappliedChanges() && !confirmDiscard()) {
				reverting = true;
				tree.setSelectionPath(shownPath);
				reverting = false;
				return;
			}
			showGrants();
		});
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				filterUsers();
			}

			public void removeUpdate(DocumentEvent e) {
				filterUsers();
			}

			public void changedUpdate(DocumentEvent e) {
				filterUsers();
			}
		});
		apply.addActionListener(e -> applyGrants());
		selectAll.addActionListener(e -> setAllPrivileges(true));
		selectNone.addActionListener(e -> setAllPrivileges(false));
		loadUsers();
		showGrants();

		setSize(900, 480);
		main.setDividerLocation(270);
		right.setDividerLocation(250);
		setLocationRelativeTo(owner);
	}

	private JPanel userButtons() {
		JButton add = Forms.button("&Add...");
		add.addActionListener(e -> addUser());
		changePassword.addActionListener(e -> changePassword());
		delete.addActionListener(e -> deleteUser());

		JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 0));
		buttons.add(add);
		buttons.add(changePassword);
		buttons.add(delete);
		return buttons;
	}

	/** The root is the server, databases are added below it and load their tables when they are opened. */
	private void initTree() throws Exception {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();

		for (String database : userManagerController.getDatabaseNames()) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(GrantTarget.database(database));
			node.add(new DefaultMutableTreeNode(LOADING));
			root.add(node);
		}

		tree.setRootVisible(true);
		tree.expandRow(0);
		tree.setSelectionRow(0);
		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			public void treeWillExpand(TreeExpansionEvent event) {
				loadTables((DefaultMutableTreeNode) event.getPath().getLastPathComponent());
			}

			public void treeWillCollapse(TreeExpansionEvent event) {
			}
		});
	}

	private void loadTables(DefaultMutableTreeNode databaseNode) {
		if (databaseNode.getChildCount() != 1 || !LOADING.equals(((DefaultMutableTreeNode) databaseNode.getFirstChild()).getUserObject())) {
			return;
		}

		try {
			String database = ((GrantTarget) databaseNode.getUserObject()).database();
			databaseNode.removeAllChildren();

			for (String table : userManagerController.getTableNames(database)) {
				databaseNode.add(new DefaultMutableTreeNode(GrantTarget.table(database, table)));
			}

			treeModel.nodeStructureChanged(databaseNode);
		} catch (Exception e) {
			showError("Load tables", e);
		}
	}

	private void loadUsers() {
		DatabaseUser selected = users.getSelectedValue();
		allUsers.clear();

		try {
			allUsers.addAll(userManagerController.listUsers());
		} catch (Exception e) {
			showError("Load users", e);
		}

		reverting = true;
		fillUserList(selected);
		reverting = false;
		userSelected();
	}

	/** Shows the users that contain the search text; the selected user stays in the list so that typing never changes the selection. */
	private void filterUsers() {
		DatabaseUser selected = users.getSelectedValue();
		reverting = true;
		fillUserList(selected);
		reverting = false;
	}

	private void fillUserList(DatabaseUser selected) {
		String wanted = search.getText().trim().toLowerCase(Locale.ROOT);
		userModel.clear();
		for (DatabaseUser user : allUsers) {
			if (wanted.isEmpty() || user.equals(selected) || user.toString().toLowerCase(Locale.ROOT).contains(wanted)) {
				userModel.addElement(user);
			}
		}
		if (selected != null) {
			users.setSelectedValue(selected, true);
		}
	}

	private void userSelected() {
		boolean selected = users.getSelectedValue() != null;
		changePassword.setEnabled(selected);
		delete.setEnabled(selected);
		showGrants();
	}

	/** The target selected in the tree, null when there is none or it is a placeholder. */
	private GrantTarget selectedTarget() {
		TreePath path = tree.getSelectionPath();

		if (path == null) {
			return null;
		}

		Object object = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		return object instanceof GrantTarget gt ? gt : null;
	}

	private void showGrants() {
		privilegePanel.removeAll();
		privilegeBoxes.clear();
		DatabaseUser user = users.getSelectedValue();
		GrantTarget target = selectedTarget();
		shownUser = user;
		shownTarget = target;
		shownPath = tree.getSelectionPath();
		shownGrants = Set.of();
		selectAll.setEnabled(user != null && target != null);
		selectNone.setEnabled(user != null && target != null);

		if (user != null && target != null) {
			try {
				shownGrants = userManagerController.getGrants(user, target);

				Map<PrivilegeGroup, JPanel> groups = new EnumMap<>(PrivilegeGroup.class);
				for (String privilege : userManagerController.getPrivileges(target.scope())) {
					JCheckBox box = new JCheckBox(privilege, shownGrants.contains(privilege));
					box.addActionListener(e -> updateChanged());
					privilegeBoxes.add(box);
					groups.computeIfAbsent(PrivilegeGroup.of(privilege), group -> Forms.titled(new JPanel(new GridLayout(0, 2, 8, 4)), group.title())).add(box);
				}
				// One group needs no heading of its own, the box around all privileges says enough.
				for (JPanel group : groups.values()) {
					if (groups.size() == 1) {
						group.setBorder(BorderFactory.createEmptyBorder());
					}
					privilegePanel.add(group);
				}

				message.setText(user + " on " + describe(target));
			} catch (Exception e) {
				showError("Read privileges", e);
			}
		}
		updateChanged();
		privilegePanel.revalidate();
		privilegePanel.repaint();
	}

	private Set<String> selectedPrivileges() {
		Set<String> selected = new LinkedHashSet<>();

		for (JCheckBox box : privilegeBoxes) {
			if (box.isSelected()) {
				selected.add(box.getText());
			}
		}
		return selected;
	}

	private void setAllPrivileges(boolean selected) {
		for (JCheckBox box : privilegeBoxes) {
			box.setSelected(selected);
		}
		updateChanged();
	}

	/** True when the checkboxes differ from what the server has granted. */
	private boolean hasUnappliedChanges() {
		return shownUser != null && shownTarget != null && !selectedPrivileges().equals(shownGrants);
	}

	/** Marks the changes that are not applied yet in the title of the group and enables Apply only for them. */
	private void updateChanged() {
		boolean changed = hasUnappliedChanges();
		apply.setEnabled(changed);
		if (grantedPanel.getBorder() instanceof CompoundBorder compound && compound.getOutsideBorder() instanceof TitledBorder titled) {
			titled.setTitle(changed ? "Granted (not applied yet)" : "Granted");
			grantedPanel.repaint();
		}
	}

	private boolean confirmDiscard() {
		return Dialogs.confirmDestructive(this, "Discard changes?",
			"The privileges of '" + shownUser + "' on " + describe(shownTarget) + " are not applied. Discard them?",
			"Discard");
	}

	private void applyGrants() {
		Set<String> selected = selectedPrivileges();

		try {
			DatabaseUser user = users.getSelectedValue();
			// The confirmation shows the plan that runs, read from the server once.
			GrantChange change = userManagerController.planGrants(user, selectedTarget(), selected);
			Set<String> revoked = change.revoked();

			if (!revoked.isEmpty() && !Dialogs.confirmDestructive(this, "Revoke privileges",
				"Revoke " + String.join(", ", revoked) + " from '" + user + "' on " + describe(selectedTarget()) + "?", "Revoke")) {
				return;
			}
			userManagerController.applyGrants(change);
			showGrants();
			message.setText("Privileges saved for " + users.getSelectedValue() + " on " + describe(selectedTarget()));
		} catch (Exception e) {
			showError("Save privileges", e);
		}
	}

	private void addUser() {
		JTextField name = new JTextField(16);
		JTextField host = new JTextField("%", 16);
		JPasswordField password = new JPasswordField(16);
		JPasswordField repeat = new JPasswordField(16);
		Forms.Grid grid = new Forms.Grid().row("&Name:", name);

		if (userManagerController.usesHost()) {
			grid.row("&Host:", host);
		}
		grid.row("&Password:", password).row("&Repeat password:", repeat);

		boolean created = Dialogs.form(this, "Add user", grid.panel(), "Create", name, () -> Validation.first(Validation.required("a name", name.getText()),
			userManagerController.usesHost() ? Validation.required("a host", host.getText()) : null,
			Validation.same(password.getPassword(), repeat.getPassword(), "The passwords are not the same.")));
		if (!created) {
			return;
		}

		try {
			DatabaseUser user = new DatabaseUser(name.getText().trim(), userManagerController.usesHost() ? host.getText().trim() : null);
			userManagerController.createUser(user, new String(password.getPassword()));
			loadUsers();
			message.setText("Created user " + user);
		} catch (Exception e) {
			showError("Add user", e);
		}
	}

	private void changePassword() {
		JPasswordField password = new JPasswordField(16);
		JPasswordField repeat = new JPasswordField(16);
		JPanel form = new Forms.Grid().row("&New password:", password).row("&Repeat password:", repeat).panel();

		if (!Dialogs.form(this, "Change password of " + users.getSelectedValue(), form, "Change", password,
			() -> Validation.same(password.getPassword(), repeat.getPassword(), "The passwords are not the same."))) {
			return;
		}

		try {
			userManagerController.changePassword(users.getSelectedValue(), new String(password.getPassword()));
			message.setText("Changed the password of " + users.getSelectedValue());
		} catch (Exception e) {
			showError("Change password", e);
		}
	}

	private void deleteUser() {
		DatabaseUser user = users.getSelectedValue();

		if (!Dialogs.confirmDestructive(this, "Drop user", "Drop user '" + user + "'? This cannot be undone.", "Drop")) {
			return;
		}

		try {
			userManagerController.dropUser(user);
			loadUsers();
			message.setText("Dropped user " + user);
		} catch (Exception e) {
			showError("Drop user", e);
		}
	}

	private String describe(GrantTarget target) {
		return switch (target.scope()) {
			case GLOBAL -> "the server";
			case DATABASE -> "database " + target.database();
			default -> "table " + target.database() + "." + target.table();
		};
	}

	/** The error is shown once, in the dialog of the error handler; the status line only gets cleared. */
	private void showError(String action, Exception e) {
		message.setText(" ");
		ApplicationContext.get().errors().report(this, action, e);
	}
}
