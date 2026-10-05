package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.DatabaseSelection;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.connection.control.ConnectionProfileCC;

import javax.swing.*;

import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
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

	private ESQLManagerUI jm;
	private ConnectionProfileCC cpcc;
	private JComboBox<Object> jc;
	private JComboBox<ServerType> jcServer;
	private JCheckBox chkAutoConnect;
	private JCheckBox chkSavePassword;
	private ServerType[] sta;
	private ServerType previousServerType;
	private boolean loadingProfile = false;
	private boolean testing = false;
	private final JTabbedPane tabs = new JTabbedPane();
	private final DatabasePicker picker;
	private static final int PICKER_TAB = 1;
	private static final String CHANGED_HINT = "Connection settings changed, test the connection again";
	private static final String TEST_FIRST_HINT = "Test the connection to choose databases and schemas";

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
		chkAutoConnect = Forms.mnemonic(new JCheckBox(), "&Auto-connect to this server on startup");

		chkSavePassword = Forms.mnemonic(new JCheckBox(), "Sa&ve password");
		chkSavePassword.setToolTipText("When off, the password is not written to profiles.xml and is asked for when connecting.");
		btnTest = Forms.button("&Test connection");
		btnDuplicate = Forms.button("Dupl&icate");

		Forms.Grid form = new Forms.Grid().row("&Profile:", jc).row("Server t&ype:", jcServer).row("&Host:", ip).row("P&ort:", pt)
			.row("&Username:", un).row("Pass&word:", pw).full(chkSavePassword).full(chkAutoConnect).full(testRow());

		picker = new DatabasePicker(this::loadSchemas, this::reloadDatabases);
		tabs.addTab("Connection", Forms.padded(form.done()));
		tabs.addTab("Databases and schemas", Forms.padded(picker));
		tabs.setEnabledAt(PICKER_TAB, false);
		tabs.setToolTipTextAt(PICKER_TAB, TEST_FIRST_HINT);
		picker.clear(TEST_FIRST_HINT);

		btnConnect = Forms.button("&Connect");
		btnSave = Forms.button("&Save");
		btnDelete = Forms.button("De&lete");
		btnClose = Forms.button("Close");

		JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		leading.add(btnDelete);
		leading.add(btnDuplicate);
		setLeadingButton(leading);
		layoutDialog(tabs, btnConnect, btnSave, btnClose);
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
		for (JTextComponent field : new JTextComponent[]{ip, pt, un, pw}) {
			field.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					settingsChanged();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					settingsChanged();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					settingsChanged();
				}
			});
		}
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				cpcc.invalidate();
			}
		});
	}

	/**
	 * The connection settings no longer match the tested ones: results that are on their way are ignored, the databases are cleared (the ticks stay) and
	 * the tab is disabled until the connection is tested again.
	 */
	private void settingsChanged() {
		cpcc.invalidate();
		boolean wasAvailable = tabs.isEnabledAt(PICKER_TAB);
		if (testing) {
			testing = false;
			testResult.setText(" ");
		}
		btnTest.setEnabled(true);
		tabs.setEnabledAt(PICKER_TAB, false);
		if (tabs.getSelectedIndex() == PICKER_TAB) {
			tabs.setSelectedIndex(0);
		}
		String hint = wasAvailable ? CHANGED_HINT : TEST_FIRST_HINT;
		tabs.setToolTipTextAt(PICKER_TAB, hint);
		picker.clear(hint);
		if (wasAvailable) {
			testResult.setForeground(UIManager.getColor("Label.foreground"));
			testResult.setText(CHANGED_HINT);
		}
	}

	private ServerType serverType() {
		return (ServerType) jcServer.getSelectedItem();
	}

	private void loadSchemas(String database) {
		cpcc.loadSchemas(database, schemas -> picker.showSchemas(database, schemas), () -> picker.showSchemaFailure(database));
	}

	private void reloadDatabases() {
		cpcc.reloadDatabases(databases -> picker.showDatabases(serverType().getDialect(), databases));
	}

	/** The Test connection button with its outcome on the line below. */
	private JPanel testRow() {
		JPanel buttonLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttonLine.add(btnTest);
		JPanel row = new JPanel(new BorderLayout(0, Forms.GAP));
		row.add(buttonLine, BorderLayout.NORTH);
		row.add(testResult, BorderLayout.CENTER);
		return row;
	}

	private void showTestResult(ConnectionProfileCC.TestResult result) {
		testing = false;
		Color color = UIManager.getColor(result.success() ? "Actions.Green" : "Actions.Red");
		if (color == null) {
			color = result.success() ? new Color(0x2e7d32) : Color.RED;
		}
		testResult.setForeground(color);
		testResult.setText(result.message());
		btnTest.setEnabled(true);
		if (result.success()) {
			picker.showDatabases(serverType().getDialect(), result.databases());
			tabs.setEnabledAt(PICKER_TAB, true);
			tabs.setToolTipTextAt(PICKER_TAB, null);
		}
		pack();
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
			picker.setSelection(DatabaseSelection.NONE);
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
				picker.setSelection(p[i].getSelection());
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
					picker.setSelection(p[0].getSelection());
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
			picker.setSelection(cp.getSelection());
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
		profile.setSelection(picker.getSelection());
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
				settingsChanged();
				btnTest.setEnabled(false);
				testing = true;
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
