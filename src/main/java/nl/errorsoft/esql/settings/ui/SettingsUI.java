package nl.errorsoft.esql.settings.ui;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.settings.Appearance;
import nl.errorsoft.esql.settings.Settings;
import nl.errorsoft.esql.app.control.ESQLManagerCC;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
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

		update = new JCheckBox("Enable auto-update");
		update.setSelected(ApplicationContext.get().settings().isUpdaterEnabled());
		updateServer = new JTextField(ApplicationContext.get().settings().getUpdateServer(), 16);
		appearance = new JComboBox<>(Appearance.values());
		appearance.setSelectedItem(ApplicationContext.get().settings().getAppearance());

		JPanel jp2 = Forms.padded(new Forms.Grid().row(new JLabel("Update server"), updateServer).full(update)
			.row(new JLabel("Appearance"), appearance).done());
		jtp = new JTabbedPane();
		jtp.addTab("Settings", jp2);

		btnOk.addActionListener(this);
		btnCancel.addActionListener(this);
		JPanel p = Forms.padded(new JPanel(new BorderLayout()));
		p.add(jtp, BorderLayout.CENTER);
		p.add(Forms.buttonRow(btnOk, btnCancel), BorderLayout.SOUTH);
		setContentPane(p);
		getRootPane().setDefaultButton(btnOk);

		this.pack();
		this.setTitle("Preferences");
		this.setResizable(false);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setLocationRelativeTo(parent);
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
