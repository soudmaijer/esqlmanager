package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseCard;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;

public class DatabasePropertiesPanel extends JPanel implements PropertiesPanel {
	private JLabel nameLabel = new JLabel("Name:");
	private JLabel commentLabel = new JLabel("Description:");
	private JTextField nameField = new JTextField();
	private JTextArea commentField = new JTextArea();

	// Databaseobject
	private DatabaseCard databaseCard;

	/** The model the database is part of, so that the name can be checked against the other databases. */
	private final Model model;

	public DatabasePropertiesPanel(DatabaseCard databaseCard, Model model) {
		this.model = model;
		nameLabel.setLabelFor(nameField);
		nameLabel.setDisplayedMnemonic('N');
		nameField.setText(databaseCard.getName());
		commentField.setFont(nameField.getFont());
		commentField.setLineWrap(true);
		commentField.setWrapStyleWord(true);
		commentField.setText(databaseCard.getDescription());
		JScrollPane commentScroll = new JScrollPane(commentField);
		commentScroll.setPreferredSize(new Dimension(185, 185));

		JPanel general = Forms.padded(new Forms.Grid().row(nameLabel, nameField).area(commentLabel, commentScroll).panel());

		setLayout(new BorderLayout());
		add(general, BorderLayout.CENTER);

		this.databaseCard = databaseCard;
	}

	public String inputProblem() {
		return model == null ? ObjectNames.modelProblem(nameField.getText()) : ObjectNames.databaseProblem(model, databaseCard, nameField.getText());
	}

	public void saveProperties() {
		databaseCard.setName(nameField.getText());
		databaseCard.setDescription(commentField.getText());
	}
}
