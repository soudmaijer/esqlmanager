package nl.errorsoft.esql.server.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JToggleButton;
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
	private final JButton showQuery = Forms.button("&Show query");
	private final JToggleButton pause = new JToggleButton("Pause");
	private final JCheckBox hideIdle = Forms.mnemonic(new JCheckBox(), "&Hide idle");
	private final JComboBox<Integer> interval = new JComboBox<>();
	/** The processes of the last refresh, the rows show the ones that pass the filter. */
	private List<ServerProcess> processes = List.of();
	/** The processes in the rows of the table. */
	private List<ServerProcess> shown = List.of();

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
		showQuery.setEnabled(false);
		kill.addActionListener(e -> killSelected(controller));
		showQuery.addActionListener(e -> showSelectedQuery());
		jtable.getSelectionModel().addListSelectionListener(e -> {
			kill.setEnabled(jtable.getSelectedRow() > -1);
			showQuery.setEnabled(jtable.getSelectedRow() > -1);
		});
		jtable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && jtable.getSelectedRow() > -1) {
					showSelectedQuery();
				}
			}
		});
		JButton close = Forms.button("&Close");
		close.addActionListener(e -> dispose());

		pause.setMnemonic('P');
		pause.addActionListener(e -> {
			controller.setPaused(pause.isSelected());
			pause.setText(pause.isSelected() ? "Resume" : "Pause");
		});
		hideIdle.addActionListener(e -> showProcesses(processes));
		for (int seconds : ProcesslistCC.INTERVALS) {
			interval.addItem(seconds);
		}
		interval.setSelectedItem(ProcesslistCC.DEFAULT_INTERVAL);
		interval.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
				return super.getListCellRendererComponent(list, value + " s", index, selected, focus);
			}
		});
		interval.addActionListener(e -> controller.setInterval((Integer) interval.getSelectedItem()));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0));
		north.add(pause);
		north.add(hideIdle);
		north.add(Forms.label("Refresh e&very:", interval));
		north.add(interval);

		JPanel south = new JPanel(new BorderLayout());
		south.add(lblInterval, BorderLayout.WEST);
		south.add(Forms.buttonRow(showQuery, kill, close), BorderLayout.EAST);

		JPanel root = Forms.padded(new JPanel(new BorderLayout(0, Forms.GAP)));
		root.add(north, BorderLayout.NORTH);
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

	private void showSelectedQuery() {
		int row = jtable.getSelectedRow();
		if (row > -1 && row < shown.size()) {
			ServerProcess process = shown.get(row);
			ProcessQueryDialog dialog = new ProcessQueryDialog(this, "process " + process.id() + " of " + process.user() + " on " + process.host(),
				process.info());
			dialog.showDialog();
		}
	}

	/** Replaces the rows in the same table model, so that the column widths stay and the process that was selected is still selected. */
	public void showProcesses(List<ServerProcess> processes) {
		this.processes = processes;
		int selected = jtable.getSelectedRow();
		Object selectedId = selected > -1 ? jtable.getValueAt(selected, 0) : null;
		DefaultTableModel model = (DefaultTableModel) jtable.getModel();
		model.setRowCount(0);
		shown = processes.stream().filter(p -> !hideIdle.isSelected() || !p.idle()).toList();
		for (ServerProcess p : shown) {
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

	public void showPaused() {
		lblInterval.setText("Paused");
	}
}
