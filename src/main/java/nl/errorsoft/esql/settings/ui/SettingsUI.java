package nl.errorsoft.esql.settings.ui;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.settings.Appearance;
import nl.errorsoft.esql.settings.Settings;
import nl.errorsoft.esql.app.control.ESQLManagerCC;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SettingsUI extends JDialog implements ActionListener {
	private JTabbedPane jtp;

	private JButton btnOk = new JButton("Save");
	private JButton btnCancel = new JButton("Cancel");

	private JCheckBox update;
	private JTextField updateServer;
	private JComboBox<Appearance> appearance;
	private ESQLManagerCC jmcc;

	public SettingsUI(ESQLManagerCC jmcc, JFrame parent) {
		super(parent, true);
		this.jmcc = jmcc;

		JPanel p = new JPanel();
		p.setPreferredSize(new Dimension(300, 175));
		p.setLayout(null);

		jtp = new JTabbedPane();
		jtp.setBounds(5, 5, 290, 140);

		p.add(jtp);

		this.getContentPane().add(p);

		JPanel jp2 = new JPanel();
		jp2.setLayout(null);
		jtp.addTab("Settings", jp2);

		update = new JCheckBox("Enable auto-update");
		update.setSelected(ApplicationContext.get().settings().isUpdaterEnabled());
		update.setBounds(5, 40, 280, 22);
		jp2.add(update);

		JLabel svr = new JLabel("Update server");
		svr.setBounds(10, 15, 110, 15);
		jp2.add(svr);

		updateServer = new JTextField(ApplicationContext.get().settings().getUpdateServer());
		updateServer.setBounds(110, 15, 150, 19);
		jp2.add(updateServer);

		JLabel look = new JLabel("Appearance");
		look.setBounds(10, 70, 110, 15);
		jp2.add(look);

		appearance = new JComboBox<>(Appearance.values());
		appearance.setSelectedItem(ApplicationContext.get().settings().getAppearance());
		appearance.setBounds(110, 67, 150, 22);
		jp2.add(appearance);

		btnOk.setBounds(130, 150, 80, 23);
		p.add(btnOk);
		btnOk.addActionListener(this);

		btnCancel.setBounds(215, 150, 80, 23);
		p.add(btnCancel);
		btnCancel.addActionListener(this);

		this.pack();
		this.setTitle("Preferences");
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setLocation(parent.getLocation().x + (int) ((parent.getSize().width - this.getSize().width) / 2),
			parent.getLocation().y + (int) ((parent.getSize().height - this.getSize().height) / 2));
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnOk) {
			ApplicationContext.get().settings().setUpdaterEnabled(update.isSelected());
			ApplicationContext.get().settings().setUpdateServer(updateServer.getText());
			ApplicationContext.get().settings().setAppearance((Appearance) appearance.getSelectedItem());
			ApplicationContext.get().settings().saveSettings();
			ApplicationContext.get().settings().getAppearance().apply();
			this.dispose();
		} else if (e.getSource() == btnCancel) {
			this.dispose();
		}
	}
}
