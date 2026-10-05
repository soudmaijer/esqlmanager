package nl.errorsoft.esql.designer.ui.dialog;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;

public class HistoryDialog extends JDialog {
	private String name = "";
	private String activity = "";

	private JLabel lbl_name = new JLabel("Name ");
	private JLabel lbl_act = new JLabel("Activity ");

	public HistoryDialog(JFrame parent, String name, String activity) {
		super(parent, true);
		this.name = name;
		this.activity = activity;

		initComponents();

		this.setVisible(true);
	}

	public void initComponents() {
		JPanel jp = Forms.padded(new JPanel(new java.awt.BorderLayout()));
		jp.setPreferredSize(new java.awt.Dimension(320, 200));
		jp.add(lbl_name, java.awt.BorderLayout.NORTH);

		this.setContentPane(jp);
	}
}
