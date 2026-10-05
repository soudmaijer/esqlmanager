package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.designer.model.Model;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.FormDialog;
import java.awt.*;

public class Properties extends FormDialog {
	private Component cur;
	private JPanel cont;

	private nl.errorsoft.esql.connection.ServerType serverType;

	public Properties(JFrame jm, nl.errorsoft.esql.connection.ServerType serverType) {
		super(jm, "Properties", true);
		this.serverType = serverType;
		setResizable(true);

		cont = new JPanel(new BorderLayout());
		cont.setPreferredSize(new Dimension(270, 300));
		setOkCancel(cont, "&Save", "Cancel");
		setOnAccept(() -> ((PropertiesInterface) cur).saveProperties());
	}

	/** @param model the model the object is part of, so that edits to a table keep its foreign keys right. */
	public void showProperties(Object obj, Model model) {
		if (cur != null) {
			cont.remove(cur);
		}
		if (obj instanceof TableObject object) {
			TableProperties tp = new TableProperties(object, serverType, model);
			this.setTitle("Properties of '" + object.getName() + "'");
			this.cont.add(tp);
			cur = tp;
		}
		if (obj instanceof DatabaseObject object1) {
			DatabaseProperties tp = new DatabaseProperties(object1);
			this.setTitle("Properties of '" + object1.getName() + "'");
			this.cont.add(tp);
			cur = tp;
		}
		if (obj instanceof nl.errorsoft.esql.designer.model.Model shown) {
			ModelProperties mp = new ModelProperties(shown);
			this.setTitle("Properties of '" + shown.getName() + "'");
			this.cont.add(mp);
			cur = mp;
		}
		cont.revalidate();
		pack();
		setLocationRelativeTo(getOwner());
	}
}
