package nl.errorsoft.esql.designer.ui.dialog;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.designer.DesignedModel;
import nl.errorsoft.esql.designer.ModelCheck;
import nl.errorsoft.esql.designer.control.GenerateController;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.ui.util.Forms;

/** Checks the designer model step by step and generates it on the server of the connection. */
public class GenerateDialog extends JDialog {
	private static final Logger log = LogManager.getLogger(GenerateDialog.class);
	private static final ModelCheck[] STEPS = ModelCheck.values();

	private final GenerateController controller;
	/** The model as it was when the dialog opened, read on the event thread; checking and generating use only this. */
	private final DesignedModel snapshot;
	private final JLabel[] checks = new JLabel[STEPS.length];
	private final JTextArea problems = new JTextArea(6, 36);
	private final JProgressBar progress = new JProgressBar();
	private final JButton generate = Forms.button("&Generate");
	private final JButton close = Forms.button("&Close");
	/** True while checking or generating, the dialog cannot be closed then. */
	private boolean busy;

	public GenerateDialog(MainWindow mainWindow, ConnectionWindow connectionWindow, Model m) {
		super((JFrame) mainWindow, "Analyze / generate model", true);
		this.controller = new GenerateController(connectionWindow.getController());
		this.snapshot = controller.snapshot(m);

		initComponents();
		setLocationRelativeTo(mainWindow);
		startChecking();
		setVisible(true);
	}
	private void initComponents() {
		setResizable(false);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!busy) {
					dispose();
				}
			}
		});

		JPanel steps = new JPanel();
		steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
		for (int i = 0; i < STEPS.length; i++) {
			checks[i] = new JLabel(STEPS[i].label(), ApplicationContext.get().imageLoader().getIcon("check_pending"), SwingConstants.LEADING);
			checks[i].setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
			steps.add(checks[i]);
		}
		steps.add(Box.createVerticalStrut(Forms.GAP));
		progress.setAlignmentX(LEFT_ALIGNMENT);
		steps.add(progress);
		Forms.titled(steps, "Checking the model");

		problems.setEditable(false);
		problems.setLineWrap(true);
		problems.setWrapStyleWord(true);
		JScrollPane problemScroll = new JScrollPane(problems);
		Forms.titled(problemScroll, "Problems");

		close.addActionListener(e -> dispose());
		close.setEnabled(false);
		generate.addActionListener(e -> generateModel());
		generate.setEnabled(false);

		JPanel root = Forms.padded(new JPanel(new BorderLayout(0, Forms.GAP)));
		root.add(steps, BorderLayout.NORTH);
		root.add(problemScroll, BorderLayout.CENTER);
		root.add(Forms.buttonRow(generate, close), BorderLayout.SOUTH);
		setContentPane(root);
		getRootPane().setDefaultButton(generate);
		pack();
	}

	/** Checks the model on a virtual thread, the dialog is updated on the event thread. */
	private void startChecking() {
		setBusy(true);
		progress.setMaximum(STEPS.length);
		Thread.ofVirtual().name("generate-check").start(() -> {
			List<List<String>> found = new ArrayList<>();
			for (int step = 0; step < STEPS.length; step++) {
				found.add(controller.problems(STEPS[step], snapshot));
				int done = step;
				List<String> stepProblems = found.get(step);
				SwingUtilities.invokeLater(() -> {
					checks[done].setIcon(ApplicationContext.get().imageLoader().getIcon(stepProblems.isEmpty() ? "check_good" : "check_error"));
					progress.setValue(done + 1);
				});
			}
			boolean ok = found.stream().allMatch(List::isEmpty);
			String text = String.join("\n", found.stream().flatMap(List::stream).toList());
			SwingUtilities.invokeLater(() -> {
				problems.setText(text);
				setBusy(false);
				generate.setEnabled(ok);
			});
		});
	}

	private void setBusy(boolean busy) {
		this.busy = busy;
		close.setEnabled(!busy);
	}

	/** Creates the databases and tables of the snapshot on a virtual thread. */
	private void generateModel() {
		progress.setValue(0);
		progress.setMaximum(snapshot.generationSteps());
		generate.setEnabled(false);
		setBusy(true);

		Thread.ofVirtual().name("generate-model").start(() -> {
			Exception failure = null;
			try {
				controller.generate(snapshot, () -> SwingUtilities.invokeLater(() -> progress.setValue(progress.getValue() + 1)));
			} catch (Exception e) {
				log.debug("Model generation failed", e);
				failure = e;
			}
			Exception error = failure;
			SwingUtilities.invokeLater(() -> {
				setBusy(false);
				controller.reloadTree();
				if (error == null) {
					dispose();
				} else {
					generate.setEnabled(true);
					ApplicationContext.get().errors().report(this, "Generate model", error);
				}
			});
		});
	}
}
