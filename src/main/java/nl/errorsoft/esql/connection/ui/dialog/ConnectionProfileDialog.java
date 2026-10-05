package nl.errorsoft.esql.connection.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.ui.DatabasePickerPanel;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.connection.control.ConnectionProfileController;
import nl.errorsoft.esql.connection.ui.ServerIconRenderer;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.ui.dialog.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/**
 * Connect to server: the saved profiles in a list at the left (with New, Remove and Duplicate above it), and at the right the name of the selected
 * profile with the tabs "Connection" and "Databases and schemas". Test connection and its outcome are at the bottom left, Close, Save and Connect at the
 * bottom right. A profile made with New is only in the list until it is saved.
 */
public class ConnectionProfileDialog extends FormDialog {
	private static final int PICKER_TAB = 1;
	private static final int LIST_WIDTH = 200;
	private static final String CHANGED_HINT = "Connection settings changed, test the connection again";
	private static final String TEST_FIRST_HINT = "Test the connection to choose databases and schemas";
	private static final String EMPTY_LIST_HINT = "No profiles yet.\nAdd one with +";

	private final ConnectionProfileController connectionProfileController;
	private final ServerType[] serverTypes = ServerType.getServerTypes();

	private final DefaultListModel<ConnectionProfile> profiles = new DefaultListModel<>();
	private final JList<ConnectionProfile> profileList = new HintList(profiles);
	private final JButton newButton = toolbarButton("profileNew", "New profile");
	private final JButton removeButton = toolbarButton("profileRemove", "Remove profile");
	private final JButton duplicateButton = toolbarButton("profileDuplicate", "Duplicate profile");

	private final JTextField name = new JTextField();
	private final JComboBox<ServerType> serverType = new JComboBox<>(new DefaultComboBoxModel<>());
	private final JTextField host = new JTextField("", 28);
	private final JTextField port = new JTextField();
	private final JTextField username = new JTextField();
	private final JPasswordField password = new JPasswordField();
	private final JCheckBox savePassword = Forms.mnemonic(new JCheckBox(), "Sa&ve password");
	private final JCheckBox autoConnect = Forms.mnemonic(new JCheckBox(), "&Auto-connect to this server on startup");
	private final JTabbedPane tabs = new JTabbedPane();
	private final DatabasePickerPanel picker;
	private final JPanel connectionForm;

	private final JButton testButton = Forms.button("&Test connection");
	private final JLabel testResult = new JLabel(" ");
	private final JButton connectButton = Forms.button("&Connect");
	private final JButton saveButton = Forms.button("&Save");
	private final JButton closeButton = Forms.button("C&lose");

	/** The profile shown in the form, a draft while it is not saved. Null when the list is empty. */
	private ConnectionProfile current;
	private boolean currentIsDraft;
	/** What the form held when {@link #current} was loaded; the form differs from it when there are unsaved changes. */
	private ConnectionProfile loaded;
	private ServerType previousServerType;
	/** True while the form or the list is filled by the program, so that the change listeners stay quiet. */
	private boolean loadingProfile = false;
	private boolean testing = false;

	public ConnectionProfileDialog(MainWindow mainWindow, ConnectionProfileController connectionProfileController) {
		super(mainWindow, "Connect to server", false);
		this.connectionProfileController = connectionProfileController;

		for (ServerType type : serverTypes) {
			serverType.addItem(type);
		}
		serverType.setRenderer(new ServerIconRenderer());
		previousServerType = (ServerType) serverType.getSelectedItem();
		savePassword.setToolTipText("When off, the password is not written to profiles.xml and is asked for when connecting.");
		testResult.setPreferredSize(new Dimension(1, testResult.getPreferredSize().height));

		connectionForm = new Forms.Grid().row("Server t&ype:", serverType).row("&Host:", host).row("P&ort:", port)
			.row("&Username:", username).row("Pass&word:", password).full(savePassword).full(autoConnect).done();
		picker = new DatabasePickerPanel(this::loadSchemas, this::reloadDatabases);
		tabs.addTab("Connection", Forms.padded(connectionForm));
		tabs.addTab("Databases and schemas", Forms.padded(picker));
		tabs.setEnabledAt(PICKER_TAB, false);
		tabs.setToolTipTextAt(PICKER_TAB, TEST_FIRST_HINT);
		picker.clear(TEST_FIRST_HINT);

		JPanel testLine = new JPanel(new BorderLayout(Forms.GAP, 0));
		testLine.add(testButton, BorderLayout.WEST);
		testLine.add(testResult, BorderLayout.CENTER);
		setLeadingLine(testLine);
		layoutDialog(content(), connectButton, saveButton, closeButton);
		setInitialFocus(connectButton);
		setResizable(true);
		setMinimumSize(new Dimension(640, 440));
		setSize(760, 520);
		setLocationRelativeTo(mainWindow);

		wireListeners();
	}

	private JPanel content() {
		JPanel left = new JPanel(new BorderLayout());
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.add(newButton);
		toolbar.add(removeButton);
		toolbar.add(duplicateButton);
		profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		profileList.setCellRenderer(new ServerIconRenderer());
		left.add(toolbar, BorderLayout.NORTH);
		left.add(new JScrollPane(profileList), BorderLayout.CENTER);
		left.setPreferredSize(new Dimension(LIST_WIDTH, 0));

		JPanel nameRow = new Forms.Grid().row("Na&me:", name).panel();
		JPanel right = new JPanel(new BorderLayout(0, Forms.GAP));
		right.add(nameRow, BorderLayout.NORTH);
		right.add(tabs, BorderLayout.CENTER);

		JPanel content = new JPanel(new BorderLayout(Forms.PADDING, 0));
		content.add(left, BorderLayout.WEST);
		content.add(right, BorderLayout.CENTER);
		return content;
	}

	private static JButton toolbarButton(String icon, String tooltip) {
		JButton button = new JButton(ApplicationContext.get().imageLoader().getIcon(icon));
		button.setToolTipText(tooltip);
		button.getAccessibleContext().setAccessibleName(tooltip);
		return button;
	}

	private void wireListeners() {
		profileList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && !loadingProfile) {
				selectionChanged();
			}
		});
		newButton.addActionListener(e -> newProfile());
		removeButton.addActionListener(e -> removeProfile());
		duplicateButton.addActionListener(e -> duplicateProfile());
		testButton.addActionListener(e -> testConnection());
		connectButton.addActionListener(e -> connect());
		saveButton.addActionListener(e -> saveCurrent());
		closeButton.addActionListener(e -> dispose());
		serverType.addItemListener(this::serverTypeChanged);

		for (JTextComponent field : new JTextComponent[]{host, port, username, password}) {
			field.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					settingsChanged();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					settingsChanged();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					settingsChanged();
				}
			});
		}
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				connectionProfileController.invalidate();
			}
		});
	}

	// ---- the list ----

	/**
	 * Shows the saved profiles and selects the one called {@code selectedName}, else the last used one, else the first. Unsaved changes of the form are
	 * dropped, the caller asks before.
	 */
	public void loadProfiles(ConnectionProfile[] saved, String selectedName) {
		loadingProfile = true;
		profiles.clear();
		ConnectionProfile selected = null;
		for (ConnectionProfile profile : saved) {
			profiles.addElement(profile);
			if (selectedName != null && profile.getName().equalsIgnoreCase(selectedName)) {
				selected = profile;
			} else if (selectedName == null && profile.isLastUsed() && selected == null) {
				selected = profile;
			}
		}
		if (selected == null && saved.length > 0) {
			selected = saved[0];
		}
		show(selected, false);
		loadingProfile = false;
	}

	/** The user clicked another profile: asks about unsaved changes, then shows it, or goes back to the one that was selected. */
	private void selectionChanged() {
		ConnectionProfile target = profileList.getSelectedValue();
		if (target == null || target == current) {
			return;
		}
		String targetName = target.getName();
		if (!leaveCurrent()) {
			loadingProfile = true;
			profileList.setSelectedValue(current, true);
			loadingProfile = false;
			return;
		}
		loadingProfile = true;
		dropDraft();
		ConnectionProfile now = find(targetName);
		show(now, false);
		loadingProfile = false;
	}

	private ConnectionProfile find(String profileName) {
		for (int i = 0; i < profiles.size(); i++) {
			if (profiles.get(i).getName().equalsIgnoreCase(profileName)) {
				return profiles.get(i);
			}
		}
		return null;
	}

	/** Takes an unsaved profile out of the list. */
	private void dropDraft() {
		if (currentIsDraft && current != null) {
			profiles.removeElement(current);
		}
		currentIsDraft = false;
	}

	/** Puts a profile in the form and selects it in the list (the caller silences the listeners). Null empties and disables the form. */
	private void show(ConnectionProfile profile, boolean draft) {
		current = profile;
		currentIsDraft = draft;
		if (profile == null) {
			profileList.clearSelection();
		} else {
			profileList.setSelectedValue(profile, true);
		}
		setEditable(profile != null);
		fillForm(profile == null ? emptyProfile() : profile);
		loaded = readForm();
	}

	private ConnectionProfile emptyProfile() {
		ConnectionProfile empty = ConnectionProfile.plain();
		empty.setServerType(serverTypes[0]);
		return empty;
	}

	/**
	 * True when the form holds no unsaved changes or the user chose to save or throw them away. Saving needs a valid form: when it fails the user stays.
	 */
	private boolean leaveCurrent() {
		if (current == null || readForm().sameSettings(loaded)) {
			return true;
		}
		return switch (Dialogs.askSave(this, "Unsaved changes", "Save the changes to '" + current.getName() + "'?")) {
			case SAVE -> saveCurrent();
			case DISCARD -> true;
			case CANCEL -> false;
		};
	}

	private void newProfile() {
		if (!leaveCurrent()) {
			return;
		}
		ConnectionProfile draft = ConnectionProfile.plain();
		ServerType type = serverTypes[0];
		Dialect dialect = type.getDialect();
		draft.setName(connectionProfileController.newProfileName());
		draft.setServerType(type);
		draft.setHost("localhost");
		draft.setPort(dialect.getDefaultPort());
		draft.setUsername(dialect.getDefaultUsername());

		loadingProfile = true;
		dropDraft();
		profiles.addElement(draft);
		show(draft, true);
		loadingProfile = false;
		name.requestFocusInWindow();
		name.selectAll();
	}

	private void removeProfile() {
		if (current == null) {
			return;
		}
		if (currentIsDraft) {
			loadingProfile = true;
			dropDraft();
			show(profiles.isEmpty() ? null : profiles.get(0), false);
			loadingProfile = false;
		} else if (Dialogs.confirmDestructive(this, "Remove profile", "Remove profile '" + current.getName() + "'? This cannot be undone.", "Remove")) {
			connectionProfileController.deleteProfile(current);
		}
	}

	private void duplicateProfile() {
		if (current == null || !leaveCurrent()) {
			return;
		}
		if (currentIsDraft) {
			Dialogs.info(this, "Duplicate profile", "Save the profile first, then duplicate it.");
		} else {
			connectionProfileController.duplicateProfile(current);
		}
	}

	// ---- the form ----

	private void setEditable(boolean editable) {
		name.setEnabled(editable);
		enableAll(connectionForm, editable);
		testButton.setEnabled(editable);
		connectButton.setEnabled(editable);
		saveButton.setEnabled(editable);
		removeButton.setEnabled(editable);
		duplicateButton.setEnabled(editable);
		if (!editable) {
			tabs.setSelectedIndex(0);
			tabs.setEnabledAt(PICKER_TAB, false);
		}
	}

	private static void enableAll(Component component, boolean enabled) {
		component.setEnabled(enabled);
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				enableAll(child, enabled);
			}
		}
	}

	private void fillForm(ConnectionProfile profile) {
		boolean wasLoading = loadingProfile;
		loadingProfile = true;
		name.setText(profile.getName());
		host.setText(profile.getHost());
		port.setText(profile.getPort());
		username.setText(profile.getUsername());
		password.setText(profile.getPassword());
		picker.setSelection(profile.getSelection());
		autoConnect.setSelected(profile.isAutoConnect());
		savePassword.setSelected(profile.isSavePassword());
		for (ServerType type : serverTypes) {
			if (profile.getServerType() != null && profile.getServerType().getType() == type.getType()) {
				serverType.setSelectedItem(type);
			}
		}
		previousServerType = (ServerType) serverType.getSelectedItem();
		loadingProfile = wasLoading;
		settingsChanged();
		testResult.setText(" ");
		testResult.setToolTipText(null);
	}

	/** What is typed in the form, as it is typed (the port may be blank or wrong). */
	private ConnectionProfile readForm() {
		ConnectionProfile profile = ConnectionProfile.plain();
		profile.setName(name.getText().trim());
		profile.setServerType((ServerType) serverType.getSelectedItem());
		profile.setHost(host.getText().trim());
		profile.setPort(port.getText().trim());
		profile.setUsername(username.getText().trim());
		profile.setPassword(new String(password.getPassword()).trim());
		profile.setSelection(picker.getSelection());
		profile.setAutoConnect(autoConnect.isSelected());
		profile.setSavePassword(savePassword.isSelected());
		return profile;
	}

	/** The form as a profile ready to use: a blank port becomes the default of the server type, null (after a message) when the port is not a number. */
	private ConnectionProfile profileFromForm() {
		ConnectionProfile profile = readForm();
		if (profile.getPort().isEmpty()) {
			profile.setPort(profile.getServerType().getDialect().getDefaultPort());
		}
		try {
			int number = Integer.parseInt(profile.getPort());
			if (number < 1 || number > 65535) {
				throw new NumberFormatException(profile.getPort());
			}
		} catch (NumberFormatException e) {
			Dialogs.warn(this, getTitle(), "The port must be a number between 1 and 65535.");
			return null;
		}
		return profile;
	}

	private void serverTypeChanged(ItemEvent e) {
		ServerType selected = (ServerType) serverType.getSelectedItem();
		if (e.getStateChange() == ItemEvent.SELECTED && !loadingProfile && selected != null) {
			applyServerDefaults(selected);
			settingsChanged();
		}
		if (selected != null) {
			previousServerType = selected;
		}
	}

	/** Fills in the default port and username of the chosen server type, but only where the field is empty or still holds the default of the previous one. */
	private void applyServerDefaults(ServerType selected) {
		Dialect previous = previousServerType == null ? null : previousServerType.getDialect();
		Dialect dialect = selected.getDialect();

		if (isEmptyOrDefault(port, previous == null ? null : previous.getDefaultPort())) {
			port.setText(dialect.getDefaultPort());
		}
		if (isEmptyOrDefault(username, previous == null ? null : previous.getDefaultUsername())) {
			username.setText(dialect.getDefaultUsername());
		}
	}

	private boolean isEmptyOrDefault(JTextField field, String previousDefault) {
		String text = field.getText().trim();
		return text.isEmpty() || text.equals(previousDefault);
	}

	// ---- actions ----

	/** Saves the form over the selected profile (or as a new one). False when the form is not valid or the name is taken. */
	private boolean saveCurrent() {
		ConnectionProfile typed = profileFromForm();
		if (typed == null) {
			return false;
		}
		if (typed.getName().isBlank()) {
			Dialogs.warn(this, getTitle(), "Enter a name for the profile.");
			return false;
		}
		return currentIsDraft || current == null
			? connectionProfileController.addProfile(typed)
			: connectionProfileController.editProfile(current.getName(), typed);
	}

	private void connect() {
		ConnectionProfile typed = profileFromForm();
		if (typed != null) {
			if (typed.getName().isBlank()) {
				typed.setName(typed.getHost());
			}
			connectionProfileController.connect(typed);
		}
	}

	private void testConnection() {
		ConnectionProfile typed = profileFromForm();
		if (typed != null) {
			settingsChanged();
			testButton.setEnabled(false);
			testing = true;
			testResult.setForeground(UIManager.getColor("Label.foreground"));
			testResult.setText("Connecting...");
			connectionProfileController.testConnection(typed, this::showTestResult);
		}
	}

	/**
	 * The connection settings no longer match the tested ones: results that are on their way are ignored, the databases are cleared (the ticks stay) and
	 * the tab is disabled until the connection is tested again.
	 */
	private void settingsChanged() {
		connectionProfileController.invalidate();
		boolean wasAvailable = tabs.isEnabledAt(PICKER_TAB);
		if (testing) {
			testing = false;
			testResult.setText(" ");
		}
		testButton.setEnabled(current != null);
		tabs.setEnabledAt(PICKER_TAB, false);
		if (tabs.getSelectedIndex() == PICKER_TAB) {
			tabs.setSelectedIndex(0);
		}
		String hint = wasAvailable ? CHANGED_HINT : TEST_FIRST_HINT;
		tabs.setToolTipTextAt(PICKER_TAB, hint);
		picker.clear(hint);
		if (wasAvailable) {
			showMessage(CHANGED_HINT, UIManager.getColor("Label.foreground"));
		}
	}

	private void showTestResult(ConnectionProfileController.TestResult result) {
		testing = false;
		showMessage(result.message(), result.success() ? Forms.successColor() : Forms.errorColor());
		testButton.setEnabled(true);
		if (result.success()) {
			picker.showDatabases(serverType().getDialect(), result.databases());
			tabs.setEnabledAt(PICKER_TAB, true);
			tabs.setToolTipTextAt(PICKER_TAB, null);
		}
	}

	/** The outcome next to the test button: one line that is cut off when it is too long, the whole text is the tooltip. */
	private void showMessage(String message, Color color) {
		testResult.setForeground(color);
		testResult.setText(message);
		testResult.setToolTipText(message.isBlank() ? null : "<html><body style='width: 380px'>" + escape(message) + "</body></html>");
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
	}

	private ServerType serverType() {
		return (ServerType) serverType.getSelectedItem();
	}

	private void loadSchemas(String database) {
		connectionProfileController.loadSchemas(database, schemas -> picker.showSchemas(database, schemas), () -> picker.showSchemaFailure(database));
	}

	private void reloadDatabases() {
		connectionProfileController.reloadDatabases(databases -> picker.showDatabases(serverType().getDialect(), databases));
	}

	/** A list that shows a hint in the middle while it is empty. */
	private static final class HintList extends JList<ConnectionProfile> {
		HintList(DefaultListModel<ConnectionProfile> model) {
			super(model);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (getModel().getSize() > 0) {
				return;
			}
			g.setColor(UIManager.getColor("Label.disabledForeground"));
			FontMetrics metrics = g.getFontMetrics();
			String[] lines = EMPTY_LIST_HINT.split("\n");
			int y = getHeight() / 3;
			for (String line : lines) {
				g.drawString(line, (getWidth() - metrics.stringWidth(line)) / 2, y);
				y += metrics.getHeight();
			}
		}
	}
}
