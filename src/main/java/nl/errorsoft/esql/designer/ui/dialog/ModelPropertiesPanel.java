package nl.errorsoft.esql.designer.ui.dialog;

import nl.errorsoft.esql.designer.model.Model;
import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class ModelPropertiesPanel extends JPanel implements PropertiesPanel {
	private Model m;

	private JLabel lbl_name = new JLabel("Name:");
	private JLabel lbl_comm = new JLabel("Description:");
	private JLabel lbl_author = new JLabel("Author:");

	private JTextField txt_name = new JTextField();
	private JTextField txt_author = new JTextField();
	private JTextArea txt_comm = new JTextArea();

	public ModelPropertiesPanel(Model m) {
		lbl_name.setLabelFor(txt_name);
		lbl_name.setDisplayedMnemonic('N');
		this.m = m;

		txt_name.setText(m.getName());
		txt_author.setText(m.getAuthor());
		txt_comm.setFont(txt_name.getFont());
		txt_comm.setLineWrap(true);
		txt_comm.setWrapStyleWord(true);
		txt_comm.setText(m.getComment());
		JScrollPane jsp = new JScrollPane(txt_comm);
		jsp.setPreferredSize(new Dimension(185, 135));

		JPanel general = Forms.padded(new Forms.Grid().row(lbl_name, txt_name).row(lbl_author, txt_author).area(lbl_comm, jsp).panel());

		setLayout(new BorderLayout());
		add(general, BorderLayout.CENTER);
	}

	public String inputProblem() {
		return ObjectNames.modelProblem(txt_name.getText());
	}

	public void saveProperties() {
		m.setAuthor(txt_author.getText());
		m.setName(txt_name.getText());
		m.setComment(txt_comm.getText());
	}
}
