package nl.errorsoft.esql.designer.ui;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.ui.util.EscapeToClose;
import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.ui.ConnectionWindowUI;
import nl.errorsoft.esql.designer.control.ModelViewerControl;
import nl.errorsoft.esql.designer.export.DiagramExporter;
import nl.errorsoft.esql.designer.export.DiagramModel;
import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.dialog.Generate;
import nl.errorsoft.esql.designer.ui.diagram.ModelArranger;
import nl.errorsoft.esql.designer.ui.diagram.ModelBrowser;
import nl.errorsoft.esql.designer.ui.diagram.ModelFilter;
import nl.errorsoft.esql.designer.ui.diagram.ModelViewer;
import nl.errorsoft.esql.designer.ui.dialog.Properties;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import java.io.*;

public class DBCreator extends JDialog implements MouseListener {
	private JMenuBar menu;
	private static final Logger log = LogManager.getLogger(DBCreator.class);

	private JMenu file = new JMenu("File");
	private JMenuItem file_new = new JMenuItem("New Model...");
	private JMenuItem file_opn = new JMenuItem("Open Model...");
	private JMenuItem file_sav = new JMenuItem("Save Model");
	private JMenuItem file_sva = new JMenuItem("Save Model As...");
	private JMenuItem file_ext = new JMenuItem("Close");
	private JMenuItem file_plantuml = new JMenuItem("Export as PlantUML...");
	private JMenuItem file_mermaid = new JMenuItem("Export as Mermaid...");

	private JMenu edit = new JMenu("Edit");
	private JMenuItem edit_del = new JMenuItem("Delete Selected");
	private JMenuItem edit_sla = new JMenuItem("Select All");
	private JMenuItem edit_dsa = new JMenuItem("Deselect All");

	private JMenu view = new JMenu("View");
	private JCheckBoxMenuItem view_grid = new JCheckBoxMenuItem("Show Grid", true);
	private JMenuItem view_arrange = new JMenuItem("Arrange Automatically");

	private ModelBrowser mb;
	private ModelViewer mv;

	private Properties properties;

	private ESQLManagerUI eui;
	private ConnectionWindowUI cwui;

	public DBCreator(ESQLManagerUI eui, ConnectionWindowUI cwui) {
		this(eui, cwui, null);
	}

	/**
	 * Opens the designer.
	 * @param model a model to show, such as one read from an existing database, which is then arranged automatically; null for a new model.
	 */
	public DBCreator(ESQLManagerUI eui, ConnectionWindowUI cwui, Model model) {
		super(eui, true);
		// Esc does not close this window: a work window with a model that may have unsaved changes.
		getRootPane().putClientProperty(EscapeToClose.DISABLED, true);

		this.eui = eui;
		this.cwui = cwui;

		this.setSize(640, 480);
		this.setTitle("eSQLDesigner");

		mv = new ModelViewer(new ModelViewerControl(this));

		JScrollPane jsp = new JScrollPane(mv);

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

		file.setMnemonic('F');
		file_new.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		file_new.setMnemonic('N');
		file_opn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		file_new.setMnemonic('O');
		file_sav.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		file_new.setMnemonic('S');
		file_new.setMnemonic('A');
		file_ext.setMnemonic('X');

		edit.add(edit_sla);
		edit.add(edit_dsa);
		edit.addSeparator();
		edit.add(edit_del);

		edit.setMnemonic('E');
		edit_sla.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
		edit_sla.setMnemonic('S');
		edit_dsa.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
		edit_dsa.setMnemonic('D');
		edit_del.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

		file_opn.addMouseListener(this);
		file_ext.addMouseListener(this);
		edit_sla.addMouseListener(this);
		edit_dsa.addMouseListener(this);
		edit_del.addMouseListener(this);
		file_sav.addMouseListener(this);
		file_sva.addMouseListener(this);
		file_new.addMouseListener(this);

		file_plantuml.addActionListener(e -> exportDiagram("PlantUML", "puml", DiagramExporter::plantUml));
		file_mermaid.addActionListener(e -> exportDiagram("Mermaid", "mmd", DiagramExporter::mermaid));

		view.add(view_grid);
		view.setMnemonic('V');
		view_grid.addActionListener(e -> mv.setShowGrid(view_grid.isSelected()));
		view.addSeparator();
		view.add(view_arrange);
		view_arrange.addActionListener(e -> arrangeAutomatically());

		buildMenu();

		mv.setShowTableTypes(cwui.getControlClass().getConnectionProfile().getServerType().getDialect().getTableTypes().length > 0);

		properties = new Properties(eui, cwui.getControlClass().getConnectionProfile().getServerType());

		if (model != null) {
			this.setSize(1024, 720);
			mv.setModel(model);
			arrangeAutomatically();
		}

		this.setLocation(eui.getLocation().x + (int) ((eui.getSize().width - this.getSize().width) / 2),
			eui.getLocation().y + (int) ((eui.getSize().height - this.getSize().height) / 2));

		this.updateTitle();

		this.setVisible(true);
	}

	/** Places the tables with the automatic layout, referenced tables left of the tables that refer to them. */
	private void arrangeAutomatically() {
		ModelArranger.arrange(mv.getModel());
		mv.resize();
		mv.repaint();
	}

	public void updateTitle() {
		this.setTitle("eSQLDesigner - '" + mv.getModel().getName() + "'");
	}

	public void buildMenu() {
		menu.removeAll();
		menu.add(file);
		menu.add(edit);
		menu.add(view);
		menu.add(mv.getModelMenu());

		this.getContentPane().add(mv.getToolbar(), BorderLayout.NORTH);
	}

	public void generate() {
		Generate g = new Generate(eui, cwui, mv.getModel());
	}

	public void showProperties(Object src) {
		properties.showProperties(src, mv.getModel());
		properties.setVisible(true);
	}

	public void openModel() {
		if (mv.isInPlaceMode()) {
			mv.exitPlaceMode();
		}

		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Open existing model");

		ModelFilter mf = new ModelFilter("edm", "eSQLManager Database Models (*.edm)");
		jfc.addChoosableFileFilter(mf);

		jfc.showOpenDialog(this);

		File f = jfc.getSelectedFile();
		if (f != null && f.exists()) {
			try {
				Model m = mv.getModel().loadModel(f);
				if (m != null) {
					mv.setModel(m);
					m.setFile(f);
				}
			} catch (Exception ex) {
				ApplicationContext.get().errors().report(this, "Open model", ex);
			}
		}

		mv.resize();
	}

	public void saveCurrentModel(boolean auto) {
		if (auto && mv.getModel().getFile() != null) {
			String xml = mv.getModel().getModelXML();
			try {
				try (PrintWriter out = new PrintWriter(new FileWriter(mv.getModel().getFile()))) {
					out.println(xml);
				}
			} catch (Exception ex) {
				ApplicationContext.get().errors().report(this, "Save current model", ex);
			}
		} else {
			JFileChooser jfc = new JFileChooser();
			jfc.setDialogTitle("Save model as");

			ModelFilter mf = new ModelFilter("edm", "eSQLManager Database Models (*.edm)");
			jfc.addChoosableFileFilter(mf);

			if (mv.getModel().getFile() != null) {
				jfc.setSelectedFile(mv.getModel().getFile());
			} else {
				jfc.setSelectedFile(new File(mv.getModel().getName() + ".edm"));
			}

			jfc.showSaveDialog(this);

			File f = jfc.getSelectedFile();
			if (f != null) {
				String xml = mv.getModel().getModelXML();
				try {
					try (PrintWriter out = new PrintWriter(new FileWriter(f))) {
						out.println(xml);
					}

					mv.getModel().setFile(f);
				} catch (Exception ex) {
					ApplicationContext.get().errors().report(this, "Save current model", ex);
				}
			}
		}
	}

	/** Writes the model as a text diagram to a file the user chooses. */
	private void exportDiagram(String format, String extension, java.util.function.Function<DiagramModel, String> exporter) {
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Export model as " + format);
		jfc.setSelectedFile(new File(mv.getModel().getName() + "." + extension));

		if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				java.nio.file.Files.writeString(jfc.getSelectedFile().toPath(), exporter.apply(DiagramModel.of(mv.getModel())),
					java.nio.charset.StandardCharsets.UTF_8);
				log.info("Model exported as {} to {}", format, jfc.getSelectedFile());
			} catch (Exception ex) {
				ApplicationContext.get().errors().report(this, "Export as " + format, ex);
			}
		}
	}

	public void newModel() {
		Dialogs.SaveChoice choice = Dialogs.askSave(this, "New model", "Save model '" + mv.getModel().getName() + "' first?");
		if (choice == Dialogs.SaveChoice.SAVE) {
			saveCurrentModel(true);
			mv.resetModel();
		} else if (choice == Dialogs.SaveChoice.DISCARD) {
			mv.resetModel();
		}
	}

	public ImageLoader getImageList() {
		return ApplicationContext.get().imageLoader();
	}

	public void mousePressed(MouseEvent e) {
	}
	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
		if (e.getSource() == file_ext) {
			Dialogs.SaveChoice choice = Dialogs.askSave(this, "Close designer", "Save model '" + mv.getModel().getName() + "' first?");
			if (choice == Dialogs.SaveChoice.SAVE) {
				saveCurrentModel(true);
				this.dispose();
			} else if (choice == Dialogs.SaveChoice.DISCARD) {
				this.dispose();
			}
		}
		if (e.getSource() == file_new) {
			newModel();
		}
		if (e.getSource() == file_opn) {
			Dialogs.SaveChoice choice = Dialogs.askSave(this, "Open model", "Save model '" + mv.getModel().getName() + "' first?");
			if (choice == Dialogs.SaveChoice.SAVE) {
				saveCurrentModel(true);
				openModel();
			} else if (choice == Dialogs.SaveChoice.DISCARD) {
				openModel();
			}
		}
		if (e.getSource() == edit_sla) {
			mv.getModel().selectAll();
		}
		if (e.getSource() == edit_dsa) {
			mv.getModel().deselectAll();
		}
		if (e.getSource() == edit_del) {
			mv.removeSelectedObjects();
		}
		if (e.getSource() == file_sav) {
			saveCurrentModel(true);
		}
		if (e.getSource() == file_sva) {
			saveCurrentModel(false);
		}
		updateTitle();
	}
}
