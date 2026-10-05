package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.model.Model;
import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class ModelPropertiesPanel extends JPanel implements PropertiesPanel {
	private Model model;

	private JLabel nameLabel = new JLabel("Name:");
	private JLabel commentLabel = new JLabel("Description:");
	private JLabel authorLabel = new JLabel("Author:");

	private JTextField nameField = new JTextField();
	private JTextField authorField = new JTextField();
	private JTextArea commentField = new JTextArea();

	public ModelPropertiesPanel(Model model) {
		nameLabel.setLabelFor(nameField);
		nameLabel.setDisplayedMnemonic('N');
		this.model = model;

		nameField.setText(model.getName());
		authorField.setText(model.getAuthor());
		commentField.setFont(nameField.getFont());
		commentField.setLineWrap(true);
		commentField.setWrapStyleWord(true);
		commentField.setText(model.getComment());
		JScrollPane commentScroll = new JScrollPane(commentField);
		commentScroll.setPreferredSize(new Dimension(185, 135));

		JPanel general = Forms.padded(new Forms.Grid().row(nameLabel, nameField).row(authorLabel, authorField).area(commentLabel, commentScroll).panel());

		setLayout(new BorderLayout());
		add(general, BorderLayout.CENTER);
	}

	public String inputProblem() {
		return ObjectNames.modelProblem(nameField.getText());
	}

	public void saveProperties() {
		model.setAuthor(authorField.getText());
		model.setName(nameField.getText());
		model.setComment(commentField.getText());
	}
}
