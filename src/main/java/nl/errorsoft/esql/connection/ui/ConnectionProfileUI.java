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
	private JButton btnTest;
	private JButton btnDuplicate;
	private final JLabel testResult = new JLabel(" ");

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
	private JCheckBox chkSavePassword;
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

		chkSavePassword = Forms.mnemonic(new JCheckBox(), "Sa&ve password");
		chkSavePassword.setToolTipText("When off, the password is not written to profiles.xml and is asked for when connecting.");
		btnTest = Forms.button("&Test connection");
		btnDuplicate = Forms.button("D&uplicate");

		Forms.Grid form = new Forms.Grid().row("&Profile:", jc).row("Server &type:", jcServer).row("&Host:", ip).row("P&ort:", pt)
			.row("&Username:", un).row("Pass&word:", pw).full(chkSavePassword).row("&Databases:", dbs).full(databasesHint()).full(chkAutoConnect)
			.full(testRow());
		dbs.setToolTipText("Comma separated, for example db1,db2,db3. The first one is connected to.");

		btnConnect = Forms.button("&Connect");
		btnSave = Forms.button("&Save");
		btnDelete = Forms.button("De&lete");
		btnClose = Forms.button("Close");

		JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		leading.add(btnDelete);
		leading.add(btnDuplicate);
		setLeadingButton(leading);
		layoutDialog(form.done(), btnConnect, btnSave, btnClose);
		setInitialFocus(btnConnect);

		btnSave.addActionListener(this);
		btnConnect.addActionListener(this);
		btnDelete.addActionListener(this);
		btnTest.addActionListener(this);
		btnDuplicate.addActionListener(this);
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

	/** The Test connection button with the outcome next to it. */
	private JPanel testRow() {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		row.add(btnTest);
		row.add(testResult);
		return row;
	}

	private void showTestResult(ConnectionProfileCC.TestResult result) {
		Color color = UIManager.getColor(result.success() ? "Actions.Green" : "Actions.Red");
		if (color == null) {
			color = result.success() ? new Color(0x2e7d32) : Color.RED;
		}
		testResult.setForeground(color);
		testResult.setText(result.message());
		btnTest.setEnabled(true);
		pack();
	}

	/** The explanation of the databases field in the colour of disabled text. */
	private static JLabel databasesHint() {
		JLabel hint = new JLabel("Comma separated, for example db1,db2. The first one is connected to.");
		java.awt.Color disabled = UIManager.getColor("Label.disabledForeground");
		if (disabled != null) {
			hint.setForeground(disabled);
		}
		hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));
		return hint;
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
				chkSavePassword.setSelected(p[i].isSavePassword());

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
					chkSavePassword.setSelected(p[0].isSavePassword());
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
			chkSavePassword.setSelected(cp.isSavePassword());
			testResult.setText(" ");

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
		profile.setSavePassword(chkSavePassword.isSelected());
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
				cpcc.editProfile(saved, typed);
			} else {
				cpcc.addProfile(typed);
			}
		} else if (object == btnTest) {
			ConnectionProfile typed = profileFromForm();
			if (typed != null) {
				btnTest.setEnabled(false);
				testResult.setForeground(UIManager.getColor("Label.foreground"));
				testResult.setText("Connecting...");
				cpcc.testConnection(typed, this::showTestResult);
			}
		} else if (object == btnDuplicate) {
			if (jc.getSelectedItem() instanceof ConnectionProfile saved) {
				cpcc.duplicateProfile(saved);
			} else {
				Dialogs.info(this, "Duplicate profile", "Save the profile first, then duplicate it.");
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
