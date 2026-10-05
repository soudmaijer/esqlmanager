package nl.errorsoft.esql.ui.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * A tree whose rows have a checkbox in front, which a click on the box or the space key toggles. The tree keeps no state of its own: a {@link CheckModel}
 * tells whether a node is ticked (or has no checkbox) and is told when the user toggles one, after which the tree paints again. The text and icon of a row
 * come from the renderer that is given.
 */
public class CheckBoxTree extends JTree {
	/** What the tree asks and tells about the checkboxes. */
	public interface CheckModel {
		/** True or false for a node with a checkbox, null for a node without one. */
		Boolean isChecked(DefaultMutableTreeNode node);

		/** The user toggled the box of the node to the given state. */
		void toggled(DefaultMutableTreeNode node, boolean checked);
	}

	private final CheckModel checks;

	public CheckBoxTree(TreeModel model, CheckModel checks, TreeCellRenderer labelRenderer) {
		super(model);
		this.checks = checks;
		setRootVisible(false);
		setShowsRootHandles(true);
		setToggleClickCount(0);
		setCellRenderer(new CheckRenderer(labelRenderer));
		getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "toggleCheck");
		getActionMap().put("toggleCheck", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggle(getSelectionPath());
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				TreePath path = getPathForLocation(e.getX(), e.getY());
				Rectangle bounds = path == null ? null : getPathBounds(path);
				if (bounds != null && e.getX() < bounds.x + boxWidth()) {
					toggle(path);
				}
			}
		});
	}

	private int boxWidth() {
		return new JCheckBox().getPreferredSize().width;
	}

	private void toggle(TreePath path) {
		if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode node) {
			Boolean checked = checks.isChecked(node);
			if (checked != null) {
				checks.toggled(node, !checked);
				repaint();
			}
		}
	}

	/** The checkbox and the row of the delegate renderer side by side. */
	private final class CheckRenderer extends JPanel implements TreeCellRenderer {
		private final TreeCellRenderer label;
		private final JCheckBox box = new JCheckBox();

		CheckRenderer(TreeCellRenderer label) {
			super(new BorderLayout());
			this.label = label;
			setOpaque(false);
			box.setOpaque(false);
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			removeAll();
			if (value instanceof DefaultMutableTreeNode node) {
				Boolean checked = checks.isChecked(node);
				if (checked != null) {
					box.setSelected(checked);
					add(box, BorderLayout.WEST);
				} else {
					// Rows without a box line up with the others.
					add(javax.swing.Box.createHorizontalStrut(boxWidth()), BorderLayout.WEST);
				}
			}
			add(label.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus), BorderLayout.CENTER);
			return this;
		}
	}
}
