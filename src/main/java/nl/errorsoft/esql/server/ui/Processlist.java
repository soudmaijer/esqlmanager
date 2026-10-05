package nl.errorsoft.esql.server.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.server.control.ProcesslistCC;

/** The window of the process list. It shows what {@link ProcesslistCC} gives it. */
public class Processlist extends JDialog {
	private static final String[] COLUMNS = {"Id", "User", "Host", "Database", "Command", "Time", "Info"};
	private static final int[] WIDTHS = {60, 90, 110, 90, 80, 50, 420};

	private final JTable jtable;
	private final JLabel lblInterval = new JLabel();

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
		getContentPane().add(jsp, BorderLayout.CENTER);

		JButton btnKillProcess = new JButton("Kill process");
		btnKillProcess.addActionListener(e -> {
			int row = jtable.getSelectedRow();
			if (row > -1) {
				controller.killProcess(jtable.getValueAt(row, 0).toString());
			}
		});
		JPanel buttons = new JPanel(new FlowLayout());
		buttons.add(btnKillProcess);
		buttons.add(new JLabel("Refreshing in:"));
		buttons.add(lblInterval);
		getContentPane().add(buttons, BorderLayout.SOUTH);

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

	/** Replaces the rows, keeping the selected row. */
	public void showProcesses(List<ServerProcess> processes) {
		int selected = jtable.getSelectedRow();
		DefaultTableModel model = new DefaultTableModel(COLUMNS, 0);
		for (ServerProcess p : processes) {
			model.addRow(new Object[]{p.id(), p.user(), p.host(), p.database(), p.command(), p.time(), p.info()});
		}
		jtable.setModel(model);
		setColumnWidths();
		if (selected > -1 && selected < model.getRowCount()) {
			jtable.setRowSelectionInterval(selected, selected);
		}
	}

	private void setColumnWidths() {
		for (int i = 0; i < WIDTHS.length; i++) {
			jtable.getColumnModel().getColumn(i).setPreferredWidth(WIDTHS[i]);
		}
	}

	public void showCountdown(int seconds) {
		lblInterval.setText(Integer.toString(seconds));
	}
}
