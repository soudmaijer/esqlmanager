package nl.errorsoft.esql.designer.ui;

import nl.errorsoft.esql.ui.dialog.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.ui.MainWindow;
import nl.errorsoft.esql.connection.ui.ConnectionWindow;
import nl.errorsoft.esql.designer.control.DesignerCanvasController;
import nl.errorsoft.esql.designer.export.DiagramExporter;
import nl.errorsoft.esql.designer.export.DiagramModel;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.dialog.GenerateDialog;
import nl.errorsoft.esql.designer.ui.ModelFileFilter;
import nl.errorsoft.esql.designer.ui.diagram.DesignerCanvas;
import nl.errorsoft.esql.designer.ui.dialog.DesignerPropertiesDialog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.event.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** The model designer, an internal frame on the desktop of the main window next to the connection windows. */
public class DesignerWindow extends JInternalFrame {
	private JMenuBar menu;
	private static final Logger log = LogManager.getLogger(DesignerWindow.class);

	private JMenu file = new JMenu("File");
	private JMenuItem newItem = new JMenuItem("New model...");
	private JMenuItem openItem = new JMenuItem("Open model...");
	private JMenuItem saveItem = new JMenuItem("Save model");
	private JMenuItem saveAsItem = new JMenuItem("Save model as...");
	private JMenuItem closeItem = new JMenuItem("Close");
	private JMenuItem plantUmlItem = new JMenuItem("Export as PlantUML...");
	private JMenuItem mermaidItem = new JMenuItem("Export as Mermaid...");

	private JMenu edit = new JMenu("Edit");
	private JMenuItem deleteItem = new JMenuItem("Delete selected");
	private JMenuItem selectAllItem = new JMenuItem("Select all");
	private JMenuItem deselectAllItem = new JMenuItem("Deselect all");

	private JMenu view = new JMenu("View");
	private JCheckBoxMenuItem gridItem = new JCheckBoxMenuItem("Show grid", true);
	private JMenuItem arrangeItem = new JMenuItem("Arrange automatically");

	private DesignerCanvas canvas;

	private DesignerPropertiesDialog properties;

	private MainWindow mainWindow;
	private ConnectionWindow connectionWindow;

	public DesignerWindow(MainWindow mainWindow, ConnectionWindow connectionWindow) {
		this(mainWindow, connectionWindow, null);
	}

	/**
	 * Opens the designer.
	 * @param model a model to show, such as one read from an existing database, which is then arranged automatically; null for a new model.
	 */
	public DesignerWindow(MainWindow mainWindow, ConnectionWindow connectionWindow, Model model) {
		super("eSQLDesigner", true, true, true, true);
		this.setFrameIcon(ApplicationContext.get().imageLoader().getIcon("imgDesigner"));
		this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		this.addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameClosing(InternalFrameEvent e) {
				close();
			}
		});

		this.mainWindow = mainWindow;
		this.connectionWindow = connectionWindow;

		this.setSize(640, 480);

		canvas = new DesignerCanvas(new DesignerCanvasController(this));

		JScrollPane canvasScroll = new JScrollPane(canvas);

		this.getContentPane().add(canvasScroll);
		canvasScroll.getViewport().setBackground(UIManager.getColor("Panel.background"));
		canvasScroll.setBorder(null);

		menu = new JMenuBar();
		this.setJMenuBar(menu);

		file.add(newItem);
		file.add(openItem);
		file.addSeparator();
		file.add(saveItem);
		file.add(saveAsItem);
		file.addSeparator();
		file.add(plantUmlItem);
		file.add(mermaidItem);
		file.addSeparator();
		file.add(closeItem);

		int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		file.setMnemonic('F');
		newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, menuKey));
		newItem.setMnemonic('N');
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuKey));
		openItem.setMnemonic('O');
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuKey));
		saveItem.setMnemonic('S');
		saveAsItem.setMnemonic('A');
		saveAsItem.setDisplayedMnemonicIndex(saveAsItem.getText().indexOf(" as") + 1);
		plantUmlItem.setMnemonic('P');
		mermaidItem.setMnemonic('M');
		closeItem.setMnemonic('C');

		edit.add(selectAllItem);
		edit.add(deselectAllItem);
		edit.addSeparator();
		edit.add(deleteItem);

		edit.setMnemonic('E');
		selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuKey));
		selectAllItem.setMnemonic('S');
		deselectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, menuKey));
		deselectAllItem.setMnemonic('D');
		deleteItem.setMnemonic('L');
		deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

		// Action listeners, so that the keyboard (mnemonics and the accelerators above) works as well as the mouse.
		newItem.addActionListener(e -> andUpdateTitle(this::newModel));
		openItem.addActionListener(e -> andUpdateTitle(this::openWithSave));
		saveItem.addActionListener(e -> andUpdateTitle(() -> saveCurrentModel(true)));
		saveAsItem.addActionListener(e -> andUpdateTitle(() -> saveCurrentModel(false)));
		closeItem.addActionListener(e -> close());
		selectAllItem.addActionListener(e -> {
			canvas.getModel().selectAll();
			canvas.repaint();
		});
		deselectAllItem.addActionListener(e -> {
			canvas.getModel().deselectAll();
			canvas.repaint();
		});
		deleteItem.addActionListener(e -> canvas.deleteSelection());

		plantUmlItem.addActionListener(e -> exportDiagram("PlantUML", "puml", DiagramExporter::plantUml));
		mermaidItem.addActionListener(e -> exportDiagram("Mermaid", "mmd", DiagramExporter::mermaid));

		view.add(gridItem);
		view.setMnemonic('V');
		gridItem.setMnemonic('G');
		arrangeItem.setMnemonic('A');
		gridItem.addActionListener(e -> canvas.setShowGrid(gridItem.isSelected()));
		// The context menu of the canvas can switch the grid too.
		canvas.addPropertyChangeListener("showGrid", e -> gridItem.setSelected(canvas.showsGrid()));
		view.addSeparator();
		view.add(arrangeItem);
		arrangeItem.addActionListener(e -> arrangeAutomatically());

		buildMenu();

		canvas.setShowTableTypes(connectionWindow.getController().getConnectionProfile().getServerType().getDialect().getTableTypes().length > 0);

		properties = new DesignerPropertiesDialog(mainWindow, connectionWindow.getController().getConnectionProfile().getServerType());

		if (model != null) {
			this.setSize(1024, 720);
			canvas.setModel(model);
			arrangeAutomatically();
		}

		this.updateTitle();

		mainWindow.addDesignerWindow(this);
	}

	/** The connection window the designer was opened from; generating the model runs on its connection. */
	public ConnectionWindow getConnectionWindow() {
		return connectionWindow;
	}

	/**
	 * Asks to save the model and closes the designer.
	 * @return false when the user cancelled
	 */
	public boolean close() {
		Dialogs.SaveChoice choice = Dialogs.askSave(this, "Close designer", "Save model '" + canvas.getModel().getName() + "' first?");
		if (choice == Dialogs.SaveChoice.CANCEL) {
			return false;
		}
		if (choice == Dialogs.SaveChoice.SAVE && !saveCurrentModel(true)) {
			return false;
		}
		mainWindow.removeDesignerWindow(this);
		return true;
	}

	@Override
	public String toString() {
		return getTitle();
	}

	/** Places the tables with the automatic layout, referenced tables left of the tables that refer to them. */
	private void arrangeAutomatically() {
		canvas.arrangeAutomatically();
	}

	public void updateTitle() {
		this.setTitle("eSQLDesigner - '" + canvas.getModel().getName() + "'");
	}

	public void buildMenu() {
		menu.removeAll();
		menu.add(file);
		menu.add(edit);
		menu.add(view);
		menu.add(canvas.getModelMenu());

		this.getContentPane().add(canvas.getToolbar(), BorderLayout.NORTH);
	}

	public void generate() {
		new GenerateDialog(mainWindow, connectionWindow, canvas.getModel());
	}

	public void showProperties(Object src) {
		properties.showProperties(src, canvas.getModel());
		properties.setVisible(true);
	}

	public void openModel() {
		if (canvas.isInPlaceMode()) {
			canvas.exitPlaceMode();
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Open existing model");

		ModelFileFilter modelFilter = new ModelFileFilter("edm", "eSQLManager Database Models (*.edm)");
		fileChooser.addChoosableFileFilter(modelFilter);

		File selected = fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
		if (selected != null && selected.exists()) {
			try {
				Model loaded = canvas.getModel().loadModel(selected);
				if (loaded != null) {
					canvas.setModel(loaded);
					loaded.setFile(selected);
				}
			} catch (Exception ex) {
				ApplicationContext.get().errors().report(this, "Open model", ex);
			}
		}

		canvas.resize();
	}

	/**
	 * Saves the model, to its file when it has one and {@code auto} is set, else to a file the user chooses.
	 * @return false when the user cancelled or the model could not be written
	 */
	public boolean saveCurrentModel(boolean auto) {
		File file = canvas.getModel().getFile();
		if (!auto || file == null) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Save model as");
			fileChooser.addChoosableFileFilter(new ModelFileFilter("edm", "eSQLManager Database Models (*.edm)"));
			fileChooser.setSelectedFile(file != null ? file : new File(canvas.getModel().getName() + ".edm"));
			if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
				return false;
			}
			file = fileChooser.getSelectedFile();
		}
		try {
			Files.writeString(file.toPath(), canvas.getModel().getModelXML() + System.lineSeparator(), StandardCharsets.UTF_8);
			canvas.getModel().setFile(file);
			return true;
		} catch (Exception ex) {
			ApplicationContext.get().errors().report(this, "Save model", ex);
			return false;
		}
	}

	/** Writes the model as a text diagram to a file the user chooses. */
	private void exportDiagram(String format, String extension, java.util.function.Function<DiagramModel, String> exporter) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Export model as " + format);
		fileChooser.setSelectedFile(new File(canvas.getModel().getName() + "." + extension));

		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				Files.writeString(fileChooser.getSelectedFile().toPath(), exporter.apply(canvas.getModel().toDiagram()),
					StandardCharsets.UTF_8);
				log.info("Model exported as {} to {}", format, fileChooser.getSelectedFile());
			} catch (Exception ex) {
				ApplicationContext.get().errors().report(this, "Export as " + format, ex);
			}
		}
	}

	public void newModel() {
		Dialogs.SaveChoice choice = Dialogs.askSave(this, "New model", "Save model '" + canvas.getModel().getName() + "' first?");
		if (choice == Dialogs.SaveChoice.CANCEL || (choice == Dialogs.SaveChoice.SAVE && !saveCurrentModel(true))) {
			return;
		}
		canvas.resetModel();
	}

	/** File > Open: asks to save the current model first. */
	private void openWithSave() {
		Dialogs.SaveChoice choice = Dialogs.askSave(this, "Open model", "Save model '" + canvas.getModel().getName() + "' first?");
		if (choice == Dialogs.SaveChoice.DISCARD || (choice == Dialogs.SaveChoice.SAVE && saveCurrentModel(true))) {
			openModel();
		}
	}

	private void andUpdateTitle(Runnable action) {
		action.run();
		updateTitle();
	}
}
