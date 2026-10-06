package nl.errorsoft.esql.database.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import nl.errorsoft.esql.connection.ConnectionNode;
import nl.errorsoft.esql.connection.ProfileNode;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;
import nl.errorsoft.esql.ui.icon.ImageLoader;

/**
 * Draws the nodes of a database tree: connections and saved profiles with the icon of their server (a profile that is not connected in grey),
 * databases, schemas, tables, and columns with their type in grey after the name.
 */
public class DatabaseTreeCellRenderer extends DefaultTreeCellRenderer {
	private final ImageLoader imgldr;
	private final String serverIcon;

	/** @param serverIcon the icon of a root node that is not a connection or profile (the server of a dialog's tree), may be null */
	public DatabaseTreeCellRenderer(ImageLoader imgldr, String serverIcon) {
		this.imgldr = imgldr;
		this.serverIcon = serverIcon;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		setToolTipText(null);
		Object object = value instanceof DefaultMutableTreeNode node ? node.getUserObject() : value;
		// Without the focus FlatLaf paints the selection in pale grey: the white selection icons would vanish on it.
		selected = selected && tree.isFocusOwner();

		switch (object) {
			case ConnectionNode connection -> setIcon(icon(connection.getProfile().getServerType().iconName(), selected));
			case ProfileNode profile -> {
				Icon icon = imgldr.getIcon(profile.profile().getServerType().iconName());
				setIcon(icon == null ? null : UIManager.getLookAndFeel().getDisabledIcon(tree, icon));
				if (!selected) {
					setForeground(UIManager.getColor("Label.disabledForeground"));
				}
				setToolTipText("Double-click to connect");
			}
			case Database database -> setIcon(icon("dbimg", selected));
			case Schema schema -> setIcon(icon("schemaimg", selected));
			case Table table -> setIcon(icon("tbimg", selected));
			case TableColumn column -> showColumn(column, selected);
			case null, default -> setIcon(serverIcon == null ? null : icon(serverIcon, selected));
		}
		return this;
	}

	/** The name, the type in grey behind it, and the key or index icon; the tooltip tells the rest. */
	private void showColumn(TableColumn column, boolean selected) {
		if (column.isPrimary()) {
			setIcon(icon("keyimg", selected));
		} else if (column.isForeignKey()) {
			setIcon(icon("fkimg", selected));
		} else if (column.hasIndex()) {
			setIcon(icon(selected ? "imgHasIndexSel" : "imgHasIndex", false));
		} else {
			setIcon(icon("fldimg", selected));
		}

		String type = column.getTypeLabel();
		if (!type.isEmpty()) {
			Color grey = selected ? getTextSelectionColor() : UIManager.getColor("Label.disabledForeground");
			setText("<html>" + escape(column.getName()) + "&nbsp;&nbsp;<span style='color:" + hex(grey) + "'>" + escape(type) + "</span></html>");
		}
		setToolTipText(columnDetails(column));
	}

	/** Not null, default, auto increment and the key, for the tooltip of a column; null when there is nothing to tell. */
	static String columnDetails(TableColumn column) {
		List<String> details = new ArrayList<>();
		if (column.isPrimary()) {
			details.add("primary key");
		}
		if (column.isForeignKey()) {
			details.add("references " + column.getReferences());
		}
		if (!column.isNullable()) {
			details.add("not null");
		}
		if (column.getDefault() != null) {
			details.add("default " + column.getDefault());
		}
		if (column.isGenerated()) {
			details.add("auto increment");
		}
		return details.isEmpty() ? null : column.getName() + " " + column.getTypeLabel() + ": " + String.join(", ", details);
	}

	/** The icon, or its variant in the selection colour ({@code ...sel}) on a selected row. */
	private Icon icon(String name, boolean selected) {
		return imgldr.getIcon(selected ? name + "sel" : name);
	}

	private static String hex(Color color) {
		Color c = color == null ? UIManager.getColor("Label.foreground") : color;
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
