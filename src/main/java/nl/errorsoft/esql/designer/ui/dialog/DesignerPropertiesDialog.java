package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.designer.model.Model;

import javax.swing.*;

import nl.errorsoft.esql.ui.dialog.FormDialog;
import java.awt.*;

public class DesignerPropertiesDialog extends FormDialog {
	private Component cur;
	private JPanel cont;

	private nl.errorsoft.esql.connection.ServerType serverType;

	public DesignerPropertiesDialog(JFrame parent, nl.errorsoft.esql.connection.ServerType serverType) {
		super(parent, "Properties", true);
		this.serverType = serverType;
		setResizable(true);

		cont = new JPanel(new BorderLayout());
		setOkCancel(cont, "&Save", "Cancel");
		setValidator(() -> cur == null ? null : ((PropertiesPanel) cur).inputProblem());
		setOnAccept(() -> ((PropertiesPanel) cur).saveProperties());
	}

	/** @param model the model the object is part of, so that edits to a table keep its foreign keys right. */
	public void showProperties(Object obj, Model model) {
		if (cur != null) {
			cont.remove(cur);
		}
		if (obj instanceof TableObject object) {
			TablePropertiesPanel properties = new TablePropertiesPanel(object, serverType, model);
			this.setTitle("Properties of '" + object.getName() + "'");
			this.cont.add(properties);
			cur = properties;
		}
		if (obj instanceof DatabaseObject object1) {
			DatabasePropertiesPanel properties = new DatabasePropertiesPanel(object1, model);
			this.setTitle("Properties of '" + object1.getName() + "'");
			this.cont.add(properties);
			cur = properties;
		}
		if (obj instanceof nl.errorsoft.esql.designer.model.Model shown) {
			ModelPropertiesPanel modelProperties = new ModelPropertiesPanel(shown);
			this.setTitle("Properties of '" + shown.getName() + "'");
			this.cont.add(modelProperties);
			cur = modelProperties;
		}
		cont.revalidate();
		pack();
		setLocationRelativeTo(getOwner());
	}
}
