package nl.errorsoft.esql.database.ui;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableColumn;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;

import nl.errorsoft.esql.ui.icon.ImageLoader;

import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;

class DatabaseTreeCellRenderer extends DefaultTreeCellRenderer {
	private ImageLoader imgldr;
	private String serverIcon;

	public DatabaseTreeCellRenderer(ImageLoader imgldr, String serverIcon) {
		this.imgldr = imgldr;
		this.serverIcon = serverIcon;
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

		if (node.getUserObject() instanceof Database) {
			if (hasFocus) {
				setIcon(imgldr.getIcon("dbimgsel"));
			} else {
				setIcon(imgldr.getIcon("dbimg"));
			}
		} else if (node.getUserObject() instanceof Schema) {
			setIcon(imgldr.getIcon(hasFocus ? "schemaimgsel" : "schemaimg"));
		} else if (node.getUserObject() instanceof Table) {
			if (hasFocus) {
				setIcon(imgldr.getIcon("tbimgsel"));
			} else {
				setIcon(imgldr.getIcon("tbimg"));
			}
		} else if (node.getUserObject() instanceof nl.errorsoft.esql.table.TableColumn) {
			TableColumn temp = (TableColumn) node.getUserObject();

			if (temp.isPrimary()) {
				setIcon(imgldr.getIcon("keyimg"));
			} else if (temp.hasIndex()) {
				setIcon(imgldr.getIcon("imgHasIndex"));
			} else {
				setIcon(imgldr.getIcon("fldimg"));
			}
		} else {
			setIcon(imgldr.getIcon(hasFocus ? serverIcon + "sel" : serverIcon));
		}

		return this;
	}
}
