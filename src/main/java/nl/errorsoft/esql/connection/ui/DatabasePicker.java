package nl.errorsoft.esql.connection.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.DatabaseSelection;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.ui.util.CheckBoxTree;
import nl.errorsoft.esql.ui.util.Forms;

/**
 * The "Databases and schemas" tab of the connect dialog: a tree with a checkbox for every database of the server and, on servers with schemas, for the
 * schemas of each database, which are loaded when the database is opened. What is ticked is kept in a {@link DatabaseSelection} that outlives the tree, so
 * ticks stay when the tree is cleared because the connection settings changed and are shown again after the next successful test.
 */
class DatabasePicker extends JPanel implements CheckBoxTree.CheckModel {
	private static final String LOADING = "Loading...";
	private static final String RETRY = "Loading failed, click to retry";

	private sealed interface Item permits DatabaseItem, SchemaItem, MessageItem {
	}

	private record DatabaseItem(String name) implements Item {
		@Override
		public String toString() {
			return name;
		}
	}

	private record SchemaItem(String database, String name) implements Item {
		@Override
		public String toString() {
			return name;
		}
	}

	/** A row without checkbox: Loading... or the failure to retry (then {@code database} is the one to load again). */
	private record MessageItem(String text, String database) implements Item {
		@Override
		public String toString() {
			return text;
		}
	}

	private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private final DefaultTreeModel model = new DefaultTreeModel(root);
	private final CheckBoxTree tree = new CheckBoxTree(model, this, new LabelRenderer());
	private final JLabel status = new JLabel(" ");
	private final Consumer<String> schemaLoader;

	private DatabaseSelection selection = DatabaseSelection.NONE;
	private Dialect dialect;
	/** The databases the server has, null while the tree is not loaded. Ticks of other databases are dropped when the selection is read. */
	private Set<String> known;
	private final Set<String> requested = new HashSet<>();

	DatabasePicker(Consumer<String> schemaLoader, Runnable reload) {
		super(new BorderLayout(0, Forms.GAP));
		this.schemaLoader = schemaLoader;

		JScrollPane scroll = new JScrollPane(tree);
		scroll.setPreferredSize(new Dimension(360, 200));
		add(scroll, BorderLayout.CENTER);

		JButton selectNone = Forms.button("Select &none");
		JButton reloadButton = Forms.button("&Reload");
		selectNone.addActionListener(e -> {
			selection = DatabaseSelection.NONE;
			tree.repaint();
			updateStatus();
		});
		reloadButton.addActionListener(e -> reload.run());

		JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, Forms.GAP, 0));
		buttons.add(selectNone);
		buttons.add(reloadButton);
		JPanel south = new JPanel(new BorderLayout(0, Forms.GAP));
		south.add(status, BorderLayout.NORTH);
		south.add(buttons, BorderLayout.SOUTH);
		add(south, BorderLayout.SOUTH);

		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillExpand(TreeExpansionEvent event) {
				if (event.getPath().getLastPathComponent() instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof DatabaseItem database) {
					loadSchemas(node, database.name());
				}
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) {
				// Nothing to do, the schemas stay loaded.
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof MessageItem message
					&& message.database() != null) {
					loadSchemas((DefaultMutableTreeNode) node.getParent(), message.database());
				}
			}
		});
	}

	/** The ticks to show, for example those of the profile that was chosen. */
	void setSelection(DatabaseSelection selection) {
		this.selection = selection;
		tree.repaint();
		updateStatus();
	}

	/** What is ticked, without the databases the server turned out not to have. */
	DatabaseSelection getSelection() {
		return known == null ? selection : selection.onlyDatabases(known::contains);
	}

	/** Empties the tree and shows the reason. The ticks are kept. */
	void clear(String message) {
		known = null;
		requested.clear();
		root.removeAllChildren();
		model.reload();
		status.setText(message);
		status.setToolTipText(message);
	}

	/** Lists the databases of the server, each expandable when the server has schemas. */
	void showDatabases(Dialect dialect, List<String> databases) {
		this.dialect = dialect;
		known = new HashSet<>(databases);
		requested.clear();
		root.removeAllChildren();
		boolean schemas = dialect.supports(Dialect.Feature.SCHEMAS);
		for (String name : databases) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(new DatabaseItem(name), schemas);
			if (schemas) {
				node.add(new DefaultMutableTreeNode(new MessageItem(LOADING, null), false));
			}
			root.add(node);
		}
		model.reload();
		updateStatus();
	}

	/** Shows the schemas that were asked for with the schema loader. */
	void showSchemas(String database, List<String> schemas) {
		DefaultMutableTreeNode node = databaseNode(database);
		if (node != null) {
			node.removeAllChildren();
			for (String name : schemas) {
				node.add(new DefaultMutableTreeNode(new SchemaItem(database, name), false));
			}
			model.nodeStructureChanged(node);
		}
	}

	/** Replaces the loading row of the database by a row that loads again when it is clicked. */
	void showSchemaFailure(String database) {
		DefaultMutableTreeNode node = databaseNode(database);
		if (node != null) {
			requested.remove(database);
			node.removeAllChildren();
			node.add(new DefaultMutableTreeNode(new MessageItem(RETRY, database), false));
			model.nodeStructureChanged(node);
		}
	}

	private void loadSchemas(DefaultMutableTreeNode node, String database) {
		if (!requested.add(database)) {
			return;
		}
		node.removeAllChildren();
		node.add(new DefaultMutableTreeNode(new MessageItem(LOADING, null), false));
		model.nodeStructureChanged(node);
		tree.expandPath(new TreePath(node.getPath()));
		schemaLoader.accept(database);
	}

	private DefaultMutableTreeNode databaseNode(String name) {
		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
			if (node.getUserObject() instanceof DatabaseItem item && item.name().equals(name)) {
				return node;
			}
		}
		return null;
	}

	@Override
	public Boolean isChecked(DefaultMutableTreeNode node) {
		return switch (node.getUserObject()) {
			case DatabaseItem database -> selection.contains(database.name());
			case SchemaItem schema -> selection.schemasOf(schema.database()).contains(schema.name());
			case null, default -> null;
		};
	}

	@Override
	public void toggled(DefaultMutableTreeNode node, boolean checked) {
		switch (node.getUserObject()) {
			case DatabaseItem database -> {
				if (checked && !DatabaseSelection.isStorable(database.name())) {
					status.setText("A name with a comma cannot be ticked");
					return;
				}
				selection = selection.withDatabase(database.name(), checked);
			}
			case SchemaItem schema -> {
				if (checked && !DatabaseSelection.isStorable(schema.database())) {
					status.setText("A name with a comma cannot be ticked");
					return;
				}
				selection = selection.withSchema(schema.database(), schema.name(), checked);
			}
			case null, default -> {
				return;
			}
		}
		updateStatus();
	}

	private void updateStatus() {
		if (known == null) {
			return;
		}
		String database = dialect == null ? "database" : dialect.databaseTerm();
		String text;
		if (known.isEmpty()) {
			text = "No " + database + "s found";
		} else if (selection.isEmpty()) {
			text = "Nothing ticked: all are shown";
		} else {
			int count = getSelection().databases().size();
			text = count + " " + database + (count == 1 ? "" : "s") + " ticked" + (selection.hasSchemaFilter() ? ", with chosen schemas" : "");
		}
		status.setText(text);
		status.setToolTipText(text + ". A " + database + " without ticked schemas shows all its schemas.");
	}

	/** The icon and text of a row. */
	private static final class LabelRenderer extends DefaultTreeCellRenderer {
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			Object item = ((DefaultMutableTreeNode) value).getUserObject();
			Icon icon = null;
			if (item instanceof DatabaseItem) {
				icon = ApplicationContext.get().imageLoader().getIcon("dbimg");
			} else if (item instanceof SchemaItem) {
				icon = ApplicationContext.get().imageLoader().getIcon("schemaimg");
			}
			setIcon(icon);
			setFont(tree.getFont());
			if (item instanceof MessageItem && !sel) {
				setForeground(UIManager.getColor("Label.disabledForeground"));
				setFont(getFont().deriveFont(Font.ITALIC));
			}
			setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
			return this;
		}
	}
}
