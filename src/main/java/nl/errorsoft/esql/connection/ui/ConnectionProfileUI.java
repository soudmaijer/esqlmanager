package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.connection.control.ConnectionProfileCC;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;
import java.awt.event.*;
import nl.errorsoft.esql.dialect.Dialect;

public class ConnectionProfileUI extends FormDialog implements ItemListener, ActionListener {
	private JButton btnConnect;
	private JButton btnSave;
	private JButton btnDelete;
	private JButton btnClose;

	private JPasswordField pw;
	private JTextField un;
	private JTextField ip;
	private JTextField pt;
	private JTextField dbs;

	private ESQLManagerUI jm;
	private ConnectionProfileCC cpcc;
	private JComboBox<Object> jc;
	private JComboBox<ServerType> jcServer;
	private JCheckBox chkAutoConnect;
	private ServerType[] sta;
	private ServerType previousServerType;
	private boolean loadingProfile = false;

	public ConnectionProfileUI(ESQLManagerUI jm, ConnectionProfileCC cpcc) {
		super(jm, "Connect to server", false);

		this.jm = jm;
		this.cpcc = cpcc;
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
		chkAutoConnect = Forms.mnemonic(new JCheckBox(), "&Auto-connect to this server on startup");

		Forms.Grid form = new Forms.Grid().row("&Profile:", jc).row("Server &type:", jcServer).row("&Host:", ip).row("P&ort:", pt)
			.row("&Username:", un).row("Pass&word:", pw).row("&Database(s):", dbs).full(chkAutoConnect);
		dbs.setToolTipText("Comma separated, for example db1,db2,db3. The first one is connected to.");

		btnConnect = Forms.button("&Connect");
		btnSave = Forms.button("&Save");
		btnDelete = Forms.button("De&lete");
		btnClose = Forms.button("Close");

		setLeadingButton(btnDelete);
		layoutDialog(form.done(), btnConnect, btnSave, btnClose);
		setInitialFocus(btnConnect);

		btnSave.addActionListener(this);
		btnConnect.addActionListener(this);
		btnDelete.addActionListener(this);
		btnClose.addActionListener(this);

		for (int t = 0; t < sta.length; t++) {
			jcServer.addItem(sta[t]);
		}

		previousServerType = (ServerType) jcServer.getSelectedItem();

		pack();
		setLocationRelativeTo(jm);

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
		return jc.getSelectedItem() == null ? "" : jc.getSelectedItem().toString();
	}

	public String getPortAsString() {
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

	/**
	 * A profile made of what is typed in the form, the saved profiles are not changed. A blank port becomes the default port of the server type, null (after
	 * a message) when the port is not a number.
	 */
	private ConnectionProfile profileFromForm() {
		ServerType serverType = (ServerType) jcServer.getSelectedItem();
		String port = getPortAsString();

		if (port.isEmpty()) {
			port = serverType.getDialect().getDefaultPort();
		}
		try {
			int number = Integer.parseInt(port);
			if (number < 1 || number > 65535) {
				throw new NumberFormatException(port);
			}
		} catch (NumberFormatException e) {
			Dialogs.warn(this, getTitle(), "The port must be a number between 1 and 65535.");
			return null;
		}

		ConnectionProfile profile = new ConnectionProfile();
		String name = getName();
		profile.setName(name == null || name.isBlank() ? "" : name.trim());
		profile.setServerType(serverType);
		profile.setHost(getAddress());
		profile.setPort(port);
		profile.setUsername(getUsername());
		profile.setPassword(getPassword());
		profile.setDatabases(getDatabases());
		profile.setAutoConnect(chkAutoConnect.isSelected());
		return profile;
	}

	public void actionPerformed(java.awt.event.ActionEvent event) {
		Object object = event.getSource();

		if (object == btnConnect) {
			ConnectionProfile typed = profileFromForm();
			if (typed != null) {
				if (typed.getName().isBlank()) {
					typed.setName(typed.getHost());
				}
				cpcc.connect(typed);
			}
		} else if (object == btnSave) {
			ConnectionProfile typed = profileFromForm();
			if (typed == null) {
				return;
			}
			if (typed.getName().isBlank()) {
				Dialogs.warn(this, getTitle(), "Enter a name for the profile.");
			} else if (jc.getSelectedItem() instanceof ConnectionProfile saved) {
				cpcc.editProfile(saved, typed.getName(), typed.getServerType(), typed.getHost(), typed.getPort(), typed.getUsername(), typed.getPassword(),
					typed.getDatabases(), typed.isAutoConnect());
			} else {
				cpcc.addProfile(typed.getName(), typed.getServerType(), typed.getHost(), typed.getPort(), typed.getUsername(), typed.getPassword(),
					typed.getDatabases(), typed.isAutoConnect());
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
