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
	private JMenuItem file_new = new JMenuItem("New model...");
	private JMenuItem file_opn = new JMenuItem("Open model...");
	private JMenuItem file_sav = new JMenuItem("Save model");
	private JMenuItem file_sva = new JMenuItem("Save model as...");
	private JMenuItem file_ext = new JMenuItem("Close");
	private JMenuItem file_plantuml = new JMenuItem("Export as PlantUML...");
	private JMenuItem file_mermaid = new JMenuItem("Export as Mermaid...");

	private JMenu edit = new JMenu("Edit");
	private JMenuItem edit_del = new JMenuItem("Delete selected");
	private JMenuItem edit_sla = new JMenuItem("Select all");
	private JMenuItem edit_dsa = new JMenuItem("Deselect all");

	private JMenu view = new JMenu("View");
	private JCheckBoxMenuItem view_grid = new JCheckBoxMenuItem("Show grid", true);
	private JMenuItem view_arrange = new JMenuItem("Arrange automatically");

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

		JScrollPane jsp = new JScrollPane(canvas);

		this.getContentPane().add(jsp);
		jsp.getViewport().setBackground(UIManager.getColor("Panel.background"));
		jsp.setBorder(null);

		menu = new JMenuBar();
		this.setJMenuBar(menu);

		file.add(file_new);
		file.add(file_opn);
		file.addSeparator();
		file.add(file_sav);
		file.add(file_sva);
		file.addSeparator();
		file.add(file_plantuml);
		file.add(file_mermaid);
		file.addSeparator();
		file.add(file_ext);

		int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		file.setMnemonic('F');
		file_new.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, menuKey));
		file_new.setMnemonic('N');
		file_opn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuKey));
		file_opn.setMnemonic('O');
		file_sav.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuKey));
		file_sav.setMnemonic('S');
		file_sva.setMnemonic('A');
		file_sva.setDisplayedMnemonicIndex(file_sva.getText().indexOf(" as") + 1);
		file_plantuml.setMnemonic('P');
		file_mermaid.setMnemonic('M');
		file_ext.setMnemonic('C');

		edit.add(edit_sla);
		edit.add(edit_dsa);
		edit.addSeparator();
		edit.add(edit_del);

		edit.setMnemonic('E');
		edit_sla.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuKey));
		edit_sla.setMnemonic('S');
		edit_dsa.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, menuKey));
		edit_dsa.setMnemonic('D');
		edit_del.setMnemonic('L');
		edit_del.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

		// Action listeners, so that the keyboard (mnemonics and the accelerators above) works as well as the mouse.
		file_new.addActionListener(e -> andUpdateTitle(this::newModel));
		file_opn.addActionListener(e -> andUpdateTitle(this::openWithSave));
		file_sav.addActionListener(e -> andUpdateTitle(() -> saveCurrentModel(true)));
		file_sva.addActionListener(e -> andUpdateTitle(() -> saveCurrentModel(false)));
		file_ext.addActionListener(e -> close());
		edit_sla.addActionListener(e -> {
			canvas.getModel().selectAll();
			canvas.repaint();
		});
		edit_dsa.addActionListener(e -> {
			canvas.getModel().deselectAll();
			canvas.repaint();
		});
		edit_del.addActionListener(e -> canvas.deleteSelection());

		file_plantuml.addActionListener(e -> exportDiagram("PlantUML", "puml", DiagramExporter::plantUml));
		file_mermaid.addActionListener(e -> exportDiagram("Mermaid", "mmd", DiagramExporter::mermaid));

		view.add(view_grid);
		view.setMnemonic('V');
		view_grid.setMnemonic('G');
		view_arrange.setMnemonic('A');
		view_grid.addActionListener(e -> canvas.setShowGrid(view_grid.isSelected()));
		// The context menu of the canvas can switch the grid too.
		canvas.addPropertyChangeListener("showGrid", e -> view_grid.setSelected(canvas.showsGrid()));
		view.addSeparator();
		view.add(view_arrange);
		view_arrange.addActionListener(e -> arrangeAutomatically());

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

		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Open existing model");

		ModelFileFilter mf = new ModelFileFilter("edm", "eSQLManager Database Models (*.edm)");
		jfc.addChoosableFileFilter(mf);

		File f = jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? jfc.getSelectedFile() : null;
		if (f != null && f.exists()) {
			try {
				Model m = canvas.getModel().loadModel(f);
				if (m != null) {
					canvas.setModel(m);
					m.setFile(f);
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
			JFileChooser jfc = new JFileChooser();
			jfc.setDialogTitle("Save model as");
			jfc.addChoosableFileFilter(new ModelFileFilter("edm", "eSQLManager Database Models (*.edm)"));
			jfc.setSelectedFile(file != null ? file : new File(canvas.getModel().getName() + ".edm"));
			if (jfc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION || jfc.getSelectedFile() == null) {
				return false;
			}
			file = jfc.getSelectedFile();
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
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Export model as " + format);
		jfc.setSelectedFile(new File(canvas.getModel().getName() + "." + extension));

		if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				Files.writeString(jfc.getSelectedFile().toPath(), exporter.apply(canvas.getModel().toDiagram()),
					StandardCharsets.UTF_8);
				log.info("Model exported as {} to {}", format, jfc.getSelectedFile());
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
