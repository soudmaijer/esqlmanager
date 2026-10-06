package nl.errorsoft.esql.server.ui.dialog;

import nl.errorsoft.esql.ui.util.MouseClicks;

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

import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.server.ServerProcess;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.server.control.ProcessListController;

/** The window of the process list. It shows what {@link ProcessListController} gives it. */
public class ProcessListDialog extends JDialog {
	private static final String[] COLUMNS = {"Id", "User", "Host", "Database", "Command", "Time", "Info"};
	private static final int[] WIDTHS = {60, 90, 110, 90, 80, 50, 420};

	private final JTable processTable;
	private final JLabel intervalLabel = new JLabel();
	private final JButton kill = Forms.button("&Kill");
	private final JButton showQuery = Forms.button("&Show query");
	private final JToggleButton pause = new JToggleButton("Pause");
	private final JCheckBox hideIdle = Forms.mnemonic(new JCheckBox(), "&Hide idle");
	private final JComboBox<Integer> interval = new JComboBox<>();
	/** The processes of the last refresh, the rows show the ones that pass the filter. */
	private List<ServerProcess> processes = List.of();
	/** The processes in the rows of the table. */
	private List<ServerProcess> shown = List.of();
	/** False once the list lost its connection; the rows stay, but nothing can be killed or refreshed. */
	private boolean connected = true;

	public ProcessListDialog(ProcessListController controller, JFrame parent, String title) {
		super(parent, title, false);
		processTable = new JTable(new DefaultTableModel(COLUMNS, 0)) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		processTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		JScrollPane scroll = new JScrollPane(processTable);
		scroll.getViewport().setBackground(UIManager.getColor("Table.background"));

		kill.setEnabled(false);
		showQuery.setEnabled(false);
		kill.addActionListener(e -> killSelected(controller));
		showQuery.addActionListener(e -> showSelectedQuery());
		processTable.getSelectionModel().addListSelectionListener(e -> {
			kill.setEnabled(connected && processTable.getSelectedRow() > -1);
			showQuery.setEnabled(processTable.getSelectedRow() > -1);
		});
		processTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (MouseClicks.isDoubleClick(e) && processTable.getSelectedRow() > -1) {
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
		for (int seconds : ProcessListController.INTERVALS) {
			interval.addItem(seconds);
		}
		interval.setSelectedItem(ProcessListController.DEFAULT_INTERVAL);
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
		south.add(intervalLabel, BorderLayout.WEST);
		south.add(Forms.buttonRow(showQuery, kill, close), BorderLayout.EAST);

		JPanel root = Forms.padded(new JPanel(new BorderLayout(0, Forms.GAP)));
		root.add(north, BorderLayout.NORTH);
		root.add(scroll, BorderLayout.CENTER);
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

	private void killSelected(ProcessListController controller) {
		int row = processTable.getSelectedRow();
		if (row > -1) {
			String id = processTable.getValueAt(row, 0).toString();
			String description = "process " + id + " of " + processTable.getValueAt(row, 1) + " on " + processTable.getValueAt(row, 2);
			if (Dialogs.confirmDestructive(this, "Kill process", "Kill " + description + "? Its running statement is stopped.", "Kill")) {
				controller.killProcess(id);
			}
		}
	}

	private void showSelectedQuery() {
		int row = processTable.getSelectedRow();
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
		int selected = processTable.getSelectedRow();
		Object selectedId = selected > -1 ? processTable.getValueAt(selected, 0) : null;
		DefaultTableModel model = (DefaultTableModel) processTable.getModel();
		model.setRowCount(0);
		shown = processes.stream().filter(p -> !hideIdle.isSelected() || !p.idle()).toList();
		for (ServerProcess p : shown) {
			model.addRow(new Object[]{p.id(), p.user(), p.host(), p.database(), p.command(), p.time(), p.info()});
		}
		for (int row = 0; selectedId != null && row < model.getRowCount(); row++) {
			if (selectedId.equals(model.getValueAt(row, 0))) {
				processTable.setRowSelectionInterval(row, row);
			}
		}
	}

	private void setColumnWidths() {
		for (int i = 0; i < WIDTHS.length; i++) {
			processTable.getColumnModel().getColumn(i).setPreferredWidth(WIDTHS[i]);
		}
	}

	public void showCountdown(int seconds) {
		intervalLabel.setText("Refreshing in " + seconds + " s");
	}

	public void showPaused() {
		intervalLabel.setText("Paused");
	}

	/** The list lost its connection: the last rows stay for reading, refreshing and killing stop. */
	public void showDisconnected() {
		connected = false;
		intervalLabel.setText("Disconnected, the list is no longer refreshed");
		intervalLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
		kill.setEnabled(false);
		pause.setEnabled(false);
		interval.setEnabled(false);
	}
}
