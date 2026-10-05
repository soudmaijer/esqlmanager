package nl.errorsoft.esql.designer.ui.diagram;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;

public class NoteCard extends ModelCard implements MouseListener, FocusListener, AdjustmentListener {
	private String comment;

	private JTextArea textArea = new JTextArea();
	private JScrollPane textScroll = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JButton focusButton = new JButton();

	public NoteCard(String comment, int identifier) {
		this.comment = comment;
		this.setOpaque(false);
		this.setIdentifier(identifier);

		textScroll.setBorder(null);
		textScroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, textScroll.getVerticalScrollBar().getSize().height));

		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);

		focusButton.setVisible(false);
		this.add(focusButton);

		this.addMouseListener(this);
		textScroll.getVerticalScrollBar().addAdjustmentListener(this);

		textArea.setFont(DesignerTheme.small());
		textArea.setForeground(DesignerTheme.text());
		textArea.setOpaque(false);
		textArea.addFocusListener(this);
		textArea.setText(comment);

		textArea.setBackground(DesignerTheme.note());

		textScroll.getViewport().setOpaque(false);
		textScroll.setOpaque(false);
		this.add(textScroll);

		this.setCardSize(150, 80);
	}

	/** The text area fills the note below the folded corner. */
	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		int shadow = DesignerTheme.SHADOW;
		textScroll.setBounds(shadow + 8, shadow + 8, width - 2 * shadow - 16, height - 2 * shadow - 12);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		// The fields are null while the superclass constructor runs.
		if (textArea != null) {
			textArea.setFont(DesignerTheme.small());
			textArea.setForeground(DesignerTheme.text());
			textArea.setBackground(DesignerTheme.note());
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
		return textArea.getText();
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
		textArea.requestFocus();
		textArea.setOpaque(true);
		textArea.repaint();
	}

	public void focusLost(FocusEvent e) {
		textArea.setOpaque(false);
		textArea.repaint();
	}

	public void focusGained(FocusEvent e) {
		textArea.setOpaque(true);
		textArea.repaint();
	}

	/** Puts the caret at the end of the text so the user can type. */
	public void startEditing() {
		textArea.requestFocusInWindow();
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public void mousePressed(MouseEvent e) {
		focusButton.requestFocus();
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
