package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.designer.model.Model;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;
import java.awt.event.*;

public class Properties extends JDialog implements ActionListener {
	private Component cur;
	private JPanel cont;

	private nl.errorsoft.esql.connection.ServerType serverType;
	private JButton ok = new JButton("OK");
	private JButton cancel = new JButton("Cancel");

	public Properties(JFrame jm, nl.errorsoft.esql.connection.ServerType serverType) {
		super(jm, true);
		this.serverType = serverType;
		this.setTitle("Properties");
		this.setResizable(true);

		cont = new JPanel(new BorderLayout());
		cont.setPreferredSize(new Dimension(270, 300));
		JPanel jp = Forms.padded(new JPanel(new BorderLayout()));
		jp.add(cont, BorderLayout.CENTER);
		jp.add(Forms.buttonRow(ok, cancel), BorderLayout.SOUTH);
		this.setContentPane(jp);

		ok.addActionListener(this);
		cancel.addActionListener(this);

		getRootPane().setDefaultButton(ok);
		this.pack();
		this.setLocationRelativeTo(jm);
	}

	/** @param model the model the object is part of, so that edits to a table keep its foreign keys right. */
	public void showProperties(Object obj, Model model) {
		if (cur != null) {
			cont.remove(cur);
		}
		if (obj instanceof TableObject object) {
			TableProperties tp = new TableProperties(object, serverType, model);
			this.setTitle("Properties for '" + object.getName() + "'");
			this.cont.add(tp);
			cur = tp;
		}
		if (obj instanceof DatabaseObject object1) {
			DatabaseProperties tp = new DatabaseProperties(object1);
			this.setTitle("Properties for '" + object1.getName() + "'");
			this.cont.add(tp);
			cur = tp;
		}
		if (obj instanceof nl.errorsoft.esql.designer.model.Model shown) {
			ModelProperties mp = new ModelProperties(shown);
			this.setTitle("Properties for '" + shown.getName() + "'");
			this.cont.add(mp);
			cur = mp;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == ok) {
			PropertiesInterface pi = (PropertiesInterface) cur;
			pi.saveProperties();
			this.setVisible(false);
		} else {
			this.setVisible(false);
		}
	}
}
