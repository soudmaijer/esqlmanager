package esql.dbcreator.gui.component;

import javax.swing.*;

public class HistoryDialog extends JDialog
{
	private String name = "";
	private String activity = "";
	
	private JLabel lbl_name = new JLabel("Name ");
	private JLabel lbl_act = new JLabel("Activity ");
	
	public HistoryDialog ( JFrame parent, String name, String activity )
	{	
		super(parent, true);
		this.name = name;
		this.activity = activity;
		
		initComponents();
		
		this.setVisible(true);
	}
	
	public void initComponents()
	{
		JPanel jp = new JPanel();
		jp.setPreferredSize(new java.awt.Dimension(320,200));
		jp.setLayout(null);
		
		lbl_name.setBounds(10,10,75, 20);
		jp.add(lbl_name);
		
		this.setContentPane(jp);
	}
}