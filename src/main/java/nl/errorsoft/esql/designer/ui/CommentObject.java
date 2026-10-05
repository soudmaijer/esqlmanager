package nl.errorsoft.esql.designer.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;

public class CommentObject extends ModelObject implements MouseListener, FocusListener, AdjustmentListener {
	private String comment;

	private JTextArea jt = new JTextArea();
	private JScrollPane jsp = new JScrollPane(jt, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JButton jb = new JButton();

	public CommentObject(String comment, int identifier) {
		this.comment = comment;
		this.setOpaque(false);
		this.setIdentifier(identifier);

		jsp.setBorder(null);
		jsp.getVerticalScrollBar().setPreferredSize(new Dimension(8, jsp.getVerticalScrollBar().getSize().height));

		jt.setLineWrap(true);
		jt.setWrapStyleWord(true);

		jb.setVisible(false);
		this.add(jb);

		this.addMouseListener(this);
		jsp.getVerticalScrollBar().addAdjustmentListener(this);

		jt.setFont(DesignerTheme.small());
		jt.setForeground(DesignerTheme.text());
		jt.setOpaque(false);
		jt.addFocusListener(this);
		jt.setText(comment);

		jt.setBackground(DesignerTheme.note());

		jsp.getViewport().setOpaque(false);
		jsp.setOpaque(false);
		this.add(jsp);

		this.setCardSize(150, 80);
	}

	/** The text area fills the note below the folded corner. */
	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		int m = DesignerTheme.SHADOW;
		jsp.setBounds(m + 8, m + 8, width - 2 * m - 16, height - 2 * m - 12);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// The fields are null while the superclass constructor runs.
		if (jt != null) {
			jt.setFont(DesignerTheme.small());
			jt.setForeground(DesignerTheme.text());
			jt.setBackground(DesignerTheme.note());
		}
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		DesignerTheme.smooth(g2);

		Rectangle card = localCard();
		int fold = 14;
		Path2D note = new Path2D.Float();
		note.moveTo(card.x + 6, card.y);
		note.lineTo(card.x + card.width - 1 - fold, card.y);
		note.lineTo(card.x + card.width - 1, card.y + fold);
		note.lineTo(card.x + card.width - 1, card.y + card.height - 7);
		note.quadTo(card.x + card.width - 1, card.y + card.height - 1, card.x + card.width - 7, card.y + card.height - 1);
		note.lineTo(card.x + 6, card.y + card.height - 1);
		note.quadTo(card.x, card.y + card.height - 1, card.x, card.y + card.height - 7);
		note.lineTo(card.x, card.y + 6);
		note.quadTo(card.x, card.y, card.x + 6, card.y);
		note.closePath();

		paintShadow(g2, note);
		g2.setColor(DesignerTheme.note());
		g2.fill(note);

		Path2D corner = new Path2D.Float();
		corner.moveTo(card.x + card.width - 1 - fold, card.y);
		corner.lineTo(card.x + card.width - 1 - fold, card.y + fold - 3);
		corner.quadTo(card.x + card.width - 1 - fold, card.y + fold, card.x + card.width - 1 - fold + 3, card.y + fold);
		corner.lineTo(card.x + card.width - 1, card.y + fold);
		corner.closePath();
		g2.setColor(DesignerTheme.noteBorder());
		g2.fill(corner);

		g2.setColor(isSelected() ? DesignerTheme.accent() : DesignerTheme.noteBorder());
		g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
		g2.draw(note);
		g2.dispose();
	}

	public String getComment() {
		return jt.getText();
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		jt.requestFocus();
		jt.setOpaque(true);
		jt.repaint();
	}

	public void focusLost(FocusEvent e) {
		jt.setOpaque(false);
		jt.repaint();
	}

	public void focusGained(FocusEvent e) {
		jt.setOpaque(true);
		jt.repaint();
	}

	public void mousePressed(MouseEvent e) {
		jb.requestFocus();
		this.repaint();
	}

	public void mouseClicked(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
}
