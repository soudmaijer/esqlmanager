package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.ui.diagram.DatabaseCard;
import nl.errorsoft.esql.designer.ui.diagram.TableCard;

import nl.errorsoft.esql.designer.model.Model;

import javax.swing.*;

import nl.errorsoft.esql.ui.dialog.FormDialog;
import java.awt.*;

public class DesignerPropertiesDialog extends FormDialog {
	private Component currentPanel;
	private JPanel content;

	private nl.errorsoft.esql.connection.ServerType serverType;

	public DesignerPropertiesDialog(JFrame parent, nl.errorsoft.esql.connection.ServerType serverType) {
		super(parent, "Properties", true);
		this.serverType = serverType;
		setResizable(true);

		content = new JPanel(new BorderLayout());
		setOkCancel(content, "&Save", "Cancel");
		setValidator(() -> currentPanel == null ? null : ((PropertiesPanel) currentPanel).inputProblem());
		setOnAccept(() -> ((PropertiesPanel) currentPanel).saveProperties());
	}

	/** @param model the model the object is part of, so that edits to a table keep its foreign keys right. */
	public void showProperties(Object subject, Model model) {
		if (currentPanel != null) {
			content.remove(currentPanel);
		}
		if (subject instanceof TableCard object) {
			TablePropertiesPanel properties = new TablePropertiesPanel(object, serverType, model);
			this.setTitle("Properties of '" + object.getName() + "'");
			this.content.add(properties);
			currentPanel = properties;
		}
		if (subject instanceof DatabaseCard object1) {
			DatabasePropertiesPanel properties = new DatabasePropertiesPanel(object1, model);
			this.setTitle("Properties of '" + object1.getName() + "'");
			this.content.add(properties);
			currentPanel = properties;
		}
		if (subject instanceof nl.errorsoft.esql.designer.model.Model shown) {
			ModelPropertiesPanel modelProperties = new ModelPropertiesPanel(shown);
			this.setTitle("Properties of '" + shown.getName() + "'");
			this.content.add(modelProperties);
			currentPanel = modelProperties;
		}
		content.revalidate();
		pack();
		setLocationRelativeTo(getOwner());
	}
}
