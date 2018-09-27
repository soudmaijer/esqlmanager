package esql.gui;

import esql.control.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SettingsUI extends JDialog implements ActionListener
{
	private JTabbedPane jtp;
	
	private JButton btnOk = new JButton("Save");
	private JButton btnCancel = new JButton("Close");
	
	private JCheckBox update;
	private JTextField updateServer;
	private ESQLManagerCC jmcc;
	
	public SettingsUI( ESQLManagerCC jmcc, JFrame parent )
	{
		super(parent, true);
		this.jmcc = jmcc;
		
		JPanel p = new JPanel ();
		p.setPreferredSize(new Dimension(300,145));
		p.setLayout(null);
		
		jtp = new JTabbedPane();
		jtp.setBounds(5,5,290,110);
		
		p.add(jtp);
		
		this.getContentPane().add(p);
		
		JPanel jp2 = new JPanel();
		jp2.setLayout(null);
		jtp.addTab("Settings", jp2);
		
		update = new JCheckBox("Enable auto-update");
		update.setSelected(jmcc.getSettings().isUpdaterEnabled());
		update.setBounds(5,40, 280, 22);
		jp2.add(update);
		
		JLabel svr = new JLabel("Update server");
		svr.setBounds(10,15,110, 15);
		jp2.add(svr);
		
		updateServer = new JTextField(jmcc.getSettings().getUpdateServer());
		updateServer.setBounds(110,15,150,19);
		jp2.add(updateServer);
		
		btnOk.setBounds(130,120,80,23);
		p.add(btnOk);
		btnOk.addActionListener(this);
		
		btnCancel.setBounds(215, 120, 80, 23);
		p.add(btnCancel);
		btnCancel.addActionListener(this);
		
		this.pack();
		this.setTitle( "Preferences");
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setLocation(parent.getLocation().x + (int)((parent.getSize().width - this.getSize().width) / 2), parent.getLocation().y + (int)((parent.getSize().height - this.getSize().height) / 2));
		this.setVisible(true);
	}
	
	public void actionPerformed (ActionEvent e)
	{	
		if(e.getSource() == btnOk)
		{	
			jmcc.getSettings().setUpdaterEnabled(update.isSelected());
			jmcc.getSettings().setUpdateServer(updateServer.getText());
			jmcc.getSettings().saveSettings();
			this.dispose();
		}
		else if(e.getSource() == btnCancel)
		{
			this.dispose();
		}
	}
}