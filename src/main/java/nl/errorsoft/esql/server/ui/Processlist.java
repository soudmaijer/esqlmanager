package nl.errorsoft.esql.server.ui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import nl.errorsoft.esql.error.Dialogs;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.server.control.ProcesslistCC;

/** The window of the process list. It shows what {@link ProcesslistCC} gives it. */
public class Processlist extends JDialog {
	private static final String[] COLUMNS = {"Id", "User", "Host", "Database", "Command", "Time", "Info"};
	private static final int[] WIDTHS = {60, 90, 110, 90, 80, 50, 420};

	private final JTable jtable;
	private final JLabel lblInterval = new JLabel();
	private final JButton kill = Forms.button("&Kill");

	public Processlist(ProcesslistCC controller, JFrame parent, String title) {
		super(parent, title, false);
		jtable = new JTable(new DefaultTableModel(COLUMNS, 0)) {
			@Override
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		JScrollPane jsp = new JScrollPane(jtable);
		jsp.getViewport().setBackground(UIManager.getColor("Table.background"));

		kill.setEnabled(false);
		kill.addActionListener(e -> killSelected(controller));
		jtable.getSelectionModel().addListSelectionListener(e -> kill.setEnabled(jtable.getSelectedRow() > -1));
		JButton close = Forms.button("&Close");
		close.addActionListener(e -> dispose());

		JPanel south = new JPanel(new BorderLayout());
		south.add(lblInterval, BorderLayout.WEST);
		south.add(Forms.buttonRow(kill, close), BorderLayout.EAST);

		JPanel root = Forms.padded(new JPanel(new BorderLayout(0, Forms.GAP)));
		root.add(jsp, BorderLayout.CENTER);
		root.add(south, BorderLayout.SOUTH);
		setContentPane(root);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				controller.stop();
			}

			@Override
			public void windowClosed(WindowEvent we) {
				controller.stop();
			}
		});
		setColumnWidths();
		setSize(760, 320);
		setLocationRelativeTo(parent);
	}

	private void killSelected(ProcesslistCC controller) {
		int row = jtable.getSelectedRow();
		if (row > -1) {
			String id = jtable.getValueAt(row, 0).toString();
			String description = "process " + id + " of " + jtable.getValueAt(row, 1) + " on " + jtable.getValueAt(row, 2);
			if (Dialogs.confirmDestructive(this, "Kill process", "Kill " + description + "? Its running statement is stopped.", "Kill")) {
				controller.killProcess(id);
			}
		}
	}

	/** Replaces the rows in the same table model, so that the column widths stay and the process that was selected is still selected. */
	public void showProcesses(List<ServerProcess> processes) {
		int selected = jtable.getSelectedRow();
		Object selectedId = selected > -1 ? jtable.getValueAt(selected, 0) : null;
		DefaultTableModel model = (DefaultTableModel) jtable.getModel();
		model.setRowCount(0);
		for (ServerProcess p : processes) {
			model.addRow(new Object[]{p.id(), p.user(), p.host(), p.database(), p.command(), p.time(), p.info()});
		}
		for (int row = 0; selectedId != null && row < model.getRowCount(); row++) {
			if (selectedId.equals(model.getValueAt(row, 0))) {
				jtable.setRowSelectionInterval(row, row);
			}
		}
	}

	private void setColumnWidths() {
		for (int i = 0; i < WIDTHS.length; i++) {
			jtable.getColumnModel().getColumn(i).setPreferredWidth(WIDTHS[i]);
		}
	}

	public void showCountdown(int seconds) {
		lblInterval.setText("Refreshing in " + seconds + " s");
	}
}
