package nl.errorsoft.esql.connection.ui;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.connection.ServerService;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerProcess;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.data.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Processlist extends JDialog implements Runnable, ActionListener {
	private static final Logger log = LogManager.getLogger(Processlist.class);

	private ESQLManagerUI jm;
	private nl.errorsoft.esql.connection.ConnectionProfile cp;
	private JScrollPane jsp;
	private JTable jtable;
	private JButton btnKillProcess;
	private JLabel lblInterval;
	private boolean refresh = true;
	private DefaultTableModel dtm;
	ConnectionWindowCC cwcc;
	DatabaseConnection m;
	private ServerService servers;

	public Processlist(ConnectionWindowCC cwcc, JFrame parent) {
		super(parent, false);
		this.cwcc = cwcc;
		initComponents();

		this.cp = cwcc.getConnectionProfile();
		this.jm = jm;
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				refresh = false;
			}

		});
		this.setTitle(cp.getUsername() + "@" + cp.getHost() + " - active processes");
		this.setSize(400, 200);
		this.setLocation(parent.getLocation().x + (int) ((parent.getSize().width - this.getSize().width) / 2),
			parent.getLocation().y + (int) ((parent.getSize().height - this.getSize().height) / 2));
		this.setVisible(true);

		Thread.ofVirtual().name("process-list").start(this);
	}

	public void run() {
		try (DatabaseConnection connection = new DatabaseConnection()) {
			m = connection;
			m.connect(cp, "");
			servers = ApplicationContext.get().connection(m).servers();
			int selRow = 0;
			DefaultTableModel dtm = null;

			while (refresh) {
				if (!m.getConnection().isClosed()) {
					if (jtable.getSelectedRow() > 0) {
						selRow = jtable.getSelectedRow();
					}

					dtm = new DefaultTableModel();
					dtm.addColumn("Id");
					dtm.addColumn("User");
					dtm.addColumn("Host");
					dtm.addColumn("Database");
					dtm.addColumn("Command");
					dtm.addColumn("Time");
					dtm.addColumn("Info");

					// Get processes and add all.
					for (ServerProcess process : servers.getProcesses()) {
						dtm.addRow(new Object[]{process.id(), process.user(), process.host(), process.database(), process.command(),
							process.time(), process.info()});
					}
					jtable.setModel(dtm);
					jtable.setRowSelectionInterval(selRow, selRow);

					Runnable doAppend = () -> jtable.updateUI();
					SwingUtilities.invokeLater(doAppend);

					for (int i = 5; i > 0; i--) {
						this.lblInterval.setText(Integer.toString(i));
						Thread.sleep(1000);

					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			ApplicationContext.get().release(m);
		}
	}

	public void initComponents() {
		jtable = new JTable() {
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jtable.setAutoResizeMode(jtable.AUTO_RESIZE_OFF);

		jsp = new JScrollPane(jtable);
		jsp.getViewport().setBackground(UIManager.getColor("Table.background"));
		this.getContentPane().add(jsp, BorderLayout.CENTER);

		JPanel p = new JPanel();
		btnKillProcess = new JButton("Kill process");
		btnKillProcess.addActionListener(this);
		p.add(btnKillProcess);
		this.getContentPane().add(p, BorderLayout.SOUTH);

		JPanel p1 = new JPanel();
		JLabel lblIntervalMsg = new JLabel("Refreshing in:");
		p.add(lblIntervalMsg);
		lblInterval = new JLabel();
		p.add(lblInterval);
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == btnKillProcess) {
			DefaultTableModel d = (DefaultTableModel) jtable.getModel();

			if (jtable.getSelectedRow() > -1) {
				try {
					servers.killProcess(jtable.getValueAt(jtable.getSelectedRow(), 0).toString());
				} catch (Exception ae) {
					ApplicationContext.get().errors().report(this, "Kill process", ae);
				}
			}
		}
	}
}
