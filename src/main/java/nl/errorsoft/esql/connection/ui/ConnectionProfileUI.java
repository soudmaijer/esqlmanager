package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.connection.control.ConnectionProfileCC;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;
import java.awt.event.*;
import nl.errorsoft.esql.dialect.Dialect;

public class ConnectionProfileUI extends JDialog implements ItemListener, ActionListener {
	private JButton btnConnect;
	private JButton btnSave;
	private JButton btnDelete;
	private JButton btnClose;

	private JPasswordField pw;
	private JTextField un;
	private JTextField ip;
	private JTextField pt;
	private JTextField dbs;
	private String ipt = "";
	private String ptt = "";
	private String unt = "";

	private ESQLManagerUI jm;
	private ConnectionProfileCC cpcc;
	private JComboBox<Object> jc;
	private JComboBox<ServerType> jcServer;
	private JCheckBox chkAutoConnect;
	private boolean useAutoConnect = true;
	private ServerType[] sta;
	private ServerType previousServerType;
	private boolean loadingProfile = false;

	public ConnectionProfileUI(ESQLManagerUI jm, ConnectionProfileCC cpcc) {
		super((JFrame) jm, false);

		this.jm = jm;
		this.cpcc = cpcc;
		this.setTitle("Connect to SQL server...");
		this.setResizable(false);
		sta = ServerType.getServerTypes();

		jc = new JComboBox<>(new DefaultComboBoxModel<>());
		jc.setEditable(true);
		jcServer = new JComboBox<>(new DefaultComboBoxModel<>());
		jcServer.setEditable(false);
		jc.setRenderer(new ServerIconRenderer());
		jcServer.setRenderer(new ServerIconRenderer());
		ip = new JTextField("", 28);
		un = new JTextField("");
		pw = new JPasswordField();
		pt = new JTextField("");
		dbs = new JTextField("");
		chkAutoConnect = new JCheckBox("Auto-connect to this server on startup");

		Forms.Grid form = new Forms.Grid().row(new JLabel("Profile: "), jc).row(new JLabel("Server type: "), jcServer).row(new JLabel("Host: "), ip)
			.row(new JLabel("Username: "), un).row(new JLabel("Password: "), pw).row(new JLabel("Port: "), pt)
			.full(new JLabel("Database(s), comma separated (example: db1,db2,db3)")).full(dbs).full(chkAutoConnect);

		btnConnect = new JButton("Connect");
		btnSave = new JButton("Save");
		btnDelete = new JButton("Delete");
		btnClose = new JButton("Close");

		JPanel root = Forms.padded(new JPanel(new BorderLayout()));
		root.add(form.done(), BorderLayout.CENTER);
		root.add(Forms.buttonRow(btnConnect, btnSave, btnDelete, btnClose), BorderLayout.SOUTH);
		setContentPane(root);
		getRootPane().setDefaultButton(btnConnect);

		btnSave.addActionListener(this);
		btnConnect.addActionListener(this);
		btnDelete.addActionListener(this);
		btnClose.addActionListener(this);

		for (int t = 0; t < sta.length; t++) {
			jcServer.addItem(sta[t]);
		}

		previousServerType = (ServerType) jcServer.getSelectedItem();

		this.pack();
		this.setLocationRelativeTo(jm);

		jc.addItemListener(this);
		jcServer.addItemListener(this);
	}

	public void loadProfiles(ConnectionProfile[] p) {
		loadingProfile = true;
		DefaultComboBoxModel<Object> dcm = new DefaultComboBoxModel<>();
		boolean foundLastUsed = false;

		if (p == null || p.length == 0) {
			ip.setText("");
			pt.setText("");
			pw.setText("");
			un.setText("");
			dbs.setText("");
		}

		for (int i = 0; i < p.length; i++) {
			dcm.addElement(p[i]);

			if (p[i].isLastUsed()) {
				foundLastUsed = true;
				dcm.setSelectedItem(p[i]);
				pt.setText(p[i].getPort());
				ip.setText(p[i].getHost());
				un.setText(p[i].getUsername());
				pw.setText(p[i].getPassword());
				dbs.setText(p[i].getDatabases());
				chkAutoConnect.setSelected(p[i].isAutoConnect());

				for (int t = 0; t < sta.length; t++) {
					if (p[i].getServerType().getType() == sta[t].getType()) {
						jcServer.setSelectedItem(sta[t]);
					}
				}
			} else {
				if (!foundLastUsed && i == p.length - 1) {
					pt.setText(p[0].getPort());
					ip.setText(p[0].getHost());
					un.setText(p[0].getUsername());
					pw.setText(p[0].getPassword());
					dbs.setText(p[0].getDatabases());
					chkAutoConnect.setSelected(p[0].isAutoConnect());
				}
			}
		}

		jc.setModel(dcm);
		loadingProfile = false;
		btnConnect.setRequestFocusEnabled(true);
		btnConnect.requestFocus(true);
	}

	public void setSelectedProfile(ConnectionProfile cp) {
		DefaultComboBoxModel<Object> dcm = (DefaultComboBoxModel<Object>) jc.getModel();
		dcm.setSelectedItem(cp);
	}

	public void itemStateChanged(ItemEvent e) {
		Object src = e.getSource();

		if (src == jc && jc.getSelectedItem() != null && jc.getSelectedItem() instanceof ConnectionProfile) {
			ConnectionProfile cp = (ConnectionProfile) jc.getSelectedItem();
			pt.setText(cp.getPort());
			ip.setText(cp.getHost());
			un.setText(cp.getUsername());
			pw.setText(cp.getPassword());
			dbs.setText(cp.getDatabases());
			chkAutoConnect.setSelected(cp.isAutoConnect());

			loadingProfile = true;

			for (int i = 0; i < jcServer.getItemCount(); i++) {
				ServerType temp = (ServerType) jcServer.getItemAt(i);

				if (temp.getType() == cp.getServerType().getType()) {
					jcServer.setSelectedIndex(i);
				}
			}

			loadingProfile = false;
		} else if (src == jcServer && e.getStateChange() == ItemEvent.SELECTED && !loadingProfile) {
			applyServerDefaults((ServerType) jcServer.getSelectedItem());
		}

		if (src == jcServer && jcServer.getSelectedItem() != null) {
			previousServerType = (ServerType) jcServer.getSelectedItem();
		}
	}

	/*
	 * @description: Fills in the default port and username of the chosen server type,
	 * but only where the field is empty or still holds the default of the previous server type.
	 */
	private void applyServerDefaults(ServerType selected) {
		Dialect previous = (previousServerType == null) ? null : previousServerType.getDialect();
		Dialect dialect = selected.getDialect();

		if (isEmptyOrDefault(pt, previous == null ? null : previous.getDefaultPort())) {
			pt.setText(dialect.getDefaultPort());
		}

		if (isEmptyOrDefault(un, previous == null ? null : previous.getDefaultUsername())) {
			un.setText(dialect.getDefaultUsername());
		}
	}

	private boolean isEmptyOrDefault(JTextField field, String previousDefault) {
		String text = field.getText().trim();
		return text.length() == 0 || text.equals(previousDefault);
	}

	public String getDatabases() {
		return dbs.getText().trim();
	}

	public String getName() {
		if (jc.getSelectedItem() instanceof ConnectionProfile) {
			return ((ConnectionProfile) jc.getSelectedItem()).getName();
		}
		return (String) jc.getSelectedItem();
	}

	public int getPort() {
		if (pt.getText().trim().length() == 0) {
			return -1;
		}

		return Integer.parseInt(pt.getText());
	}

	public String getPortAsString() {
		if (pt.getText().trim().length() == 0) {
			return "";
		}

		return pt.getText().trim();
	}

	public String getAddress() {
		if (ip.getText().trim().length() == 0) {
			return "";
		}

		return ip.getText().trim();
	}

	public String getUsername() {
		if (un.getText().trim().length() == 0) {
			return "";
		}

		return un.getText().trim();
	}

	public String getPassword() {
		if (new String(pw.getPassword()).trim().length() == 0) {
			return "";
		}
		return new String(pw.getPassword()).trim();
	}

	public void actionPerformed(java.awt.event.ActionEvent event) {
		Object object = event.getSource();

		if (object == btnConnect) {
			if (jc.getSelectedItem() != null) {
				ConnectionProfile temp = (ConnectionProfile) jc.getSelectedItem();
				temp.setHost(this.getAddress());
				temp.setPort(Integer.toString(this.getPort()));
				temp.setUsername(this.getUsername());
				temp.setPassword(this.getPassword());
				temp.setServerType((ServerType) jcServer.getSelectedItem());
				temp.setDatabases(this.getDatabases());
				cpcc.connect(temp);
			}
		} else if (object == btnSave) {
			if (jc.getSelectedItem() instanceof ConnectionProfile) {
				cpcc.editProfile((ConnectionProfile) jc.getSelectedItem(), getName(), (ServerType) jcServer.getSelectedItem(), getAddress(),
					getPortAsString(), getUsername(), getPassword(), getDatabases(), chkAutoConnect.isSelected());
			} else {
				cpcc.addProfile(getName(), (ServerType) jcServer.getSelectedItem(), getAddress(), getPortAsString(), getUsername(), getPassword(),
					getDatabases(), chkAutoConnect.isSelected());
			}
		} else if (object == btnClose) {
			this.dispose();
		} else if (object == btnDelete) {
			if (jc.getSelectedIndex() > -1
				&& Dialogs.confirmDestructive(this, "Delete profile", "Delete profile '" + jc.getSelectedItem() + "'?", "Delete")) {
				cpcc.deleteProfile((ConnectionProfile) jc.getSelectedItem());
			}
		}
	}

}
