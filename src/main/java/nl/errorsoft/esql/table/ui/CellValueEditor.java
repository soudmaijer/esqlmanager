package nl.errorsoft.esql.table.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.table.PendingChange;
import nl.errorsoft.esql.table.TableCell;
import nl.errorsoft.esql.table.ValueFormat;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.util.ExtensionFileFilter;
import nl.errorsoft.esql.ui.util.FileChoosers;
import nl.errorsoft.esql.ui.util.Forms;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * The value of one cell below the grid: a syntax coloured editor (JSON, XML, SQL or plain, guessed from the text) with Format, Wrap lines, Set NULL, files,
 * Apply and Revert, or for binary data the Upload and Download buttons. An empty text is SQL NULL.
 */
public class CellValueEditor extends JPanel {
	private static final String TEXT = "text";
	private static final String BINARY = "binary";

	/** What the editor asks of its view: write a text (empty is NULL), close, and the blob transfers of a binary cell. */
	public record Actions(Consumer<String> apply, Runnable close, Runnable upload, Runnable download, Runnable changed) {
	}

	private final Actions actions;
	private final RSyntaxTextArea editor = new RSyntaxTextArea(8, 60);
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JLabel title = new JLabel();
	private final JLabel hint = new JLabel();
	private final JButton format = Forms.button("F&ormat");
	private final JToggleButton wrap = Forms.mnemonic(new JToggleButton(), "&Wrap lines");
	private final JButton setNull = Forms.button("Set &NULL");
	private final JButton load = Forms.button("&Load from file...");
	private final JButton save = Forms.button("&Save to file...");
	private final JButton revert = Forms.button("&Revert");
	private final JButton apply = Forms.button("&Apply");
	private final JButton close = Forms.button("&Close");
	private final JButton upload = Forms.button("&Upload...");
	private final JButton download = Forms.button("&Download...");
	private TableCell cell;
	private boolean writable;

	public CellValueEditor(Actions actions) {
		super(new BorderLayout());
		this.actions = actions;
		EditorTheme.install(editor);
		editor.setCodeFoldingEnabled(true);
		editor.setHighlightCurrentLine(false);
		editor.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				changed();
			}

			public void removeUpdate(DocumentEvent e) {
				changed();
			}

			public void changedUpdate(DocumentEvent e) {
				changed();
			}
		});

		format.setToolTipText("Pretty print the JSON or XML");
		wrap.setToolTipText("Wrap long lines at the edge of the editor");
		setNull.setToolTipText("Empty the value, Apply writes NULL");
		load.setToolTipText("Replace the value with the text of a file");
		save.setToolTipText("Save the value to a text file");
		revert.setToolTipText("Go back to the value of the cell");
		apply.setToolTipText("Write the value into the cell");
		close.setToolTipText("Close the value editor");
		upload.setToolTipText("Load a file into the cell");
		download.setToolTipText("Save the cell data to a file");

		format.addActionListener(e -> formatText());
		wrap.addActionListener(e -> editor.setLineWrap(wrap.isSelected()));
		setNull.addActionListener(e -> editor.setText(""));
		load.addActionListener(e -> loadFromFile());
		save.addActionListener(e -> saveToFile());
		revert.addActionListener(e -> showText());
		apply.addActionListener(e -> actions.apply().accept(editor.getText()));
		close.addActionListener(e -> actions.close().run());
		upload.addActionListener(e -> actions.upload().run());
		download.addActionListener(e -> actions.download().run());

		JToolBar bar = new JToolBar();
		bar.setFloatable(false);
		title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
		bar.add(title);
		bar.add(format);
		bar.add(wrap);
		bar.add(setNull);
		bar.add(load);
		bar.add(save);
		bar.add(Box.createHorizontalGlue());
		hint.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 12));
		bar.add(hint);
		bar.add(revert);
		bar.add(apply);
		bar.add(close);
		bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Component.borderColor")),
			BorderFactory.createEmptyBorder(2, 8, 2, 4)));

		RTextScrollPane scroll = new RTextScrollPane(editor);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		content.add(scroll, TEXT);
		JPanel binary = Forms.padded(new JPanel(new FlowLayout(FlowLayout.LEFT, Forms.GAP, 0)));
		binary.add(new JLabel("Binary data is not shown as text."));
		binary.add(upload);
		binary.add(download);
		content.add(binary, BINARY);

		add(bar, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}

	/**
	 * Shows a cell. {@code readOnly} is null when the value can be written, else the hint saying why it cannot; binary data is never shown as text.
	 */
	public void showCell(TableCell cell, String readOnly) {
		this.cell = cell;
		this.writable = readOnly == null;
		title.setText(cell.getTableColumn() == null ? "Value" : cell.getTableColumn().getName());
		boolean binaryCell = cell.getTableColumn() != null && cell.getTableColumn().isBinary();
		cards.show(content, binaryCell ? BINARY : TEXT);
		editor.setEditable(writable && !binaryCell);
		upload.setEnabled(writable && !cell.isNewRow());
		download.setEnabled(writable && !cell.isNewRow());
		for (JButton textOnly : new JButton[]{format, setNull, load, save}) {
			textOnly.setVisible(!binaryCell);
		}
		wrap.setVisible(!binaryCell);
		// Taken when a cell is shown, so that it follows a change of look and feel.
		hint.setForeground(UIManager.getColor("Label.disabledForeground"));
		hint.setText(readOnly == null ? "" : readOnly);
		showText();
	}

	/** Loads the value of the cell again, for Revert and after the value was written. */
	public void showText() {
		if (cell == null) {
			return;
		}
		String text = cell.getEditText();
		ValueFormat kind = ValueFormat.detect(text);
		editor.setSyntaxEditingStyle(switch (kind) {
			case JSON -> SyntaxConstants.SYNTAX_STYLE_JSON;
			case XML -> SyntaxConstants.SYNTAX_STYLE_XML;
			case SQL -> SyntaxConstants.SYNTAX_STYLE_SQL;
			case PLAIN -> SyntaxConstants.SYNTAX_STYLE_NONE;
		});
		editor.setText(text);
		editor.setCaretPosition(0);
		// Undo starts at the value of the cell, not at the previous cell.
		editor.discardAllEdits();
		changed();
	}

	public TableCell getCell() {
		return cell;
	}

	/** Whether Apply would change the cell. */
	public boolean isChanged() {
		return cell != null && PendingChange.of(writable, false, cell, editor.getText()) == PendingChange.CELL;
	}

	/** The editor itself, for focus and for a harness. */
	RSyntaxTextArea textArea() {
		return editor;
	}

	private void changed() {
		boolean changed = isChanged();
		apply.setEnabled(changed);
		revert.setEnabled(changed);
		setNull.setEnabled(writable && !editor.getText().isEmpty());
		load.setEnabled(writable);
		format.setEnabled(writable && ValueFormat.detect(editor.getText()).canFormat());
		if (writable) {
			hint.setText(editor.getText().isEmpty() ? "Empty is NULL" : "");
		}
		actions.changed().run();
	}

	private void formatText() {
		try {
			editor.setText(ValueFormat.detect(editor.getText()).format(editor.getText()));
			editor.setCaretPosition(0);
		} catch (Exception e) {
			ApplicationContext.get().errors().report(this, "Format value", e);
		}
	}

	private void loadFromFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Load value from file");

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			Path source = chooser.getSelectedFile().toPath();
			// The file is read off the event thread and shown on it.
			Thread.ofVirtual().name("load-cell-value").start(() -> {
				try {
					String text = Files.readString(source);
					SwingUtilities.invokeLater(() -> editor.setText(text));
				} catch (Exception e) {
					SwingUtilities.invokeLater(() -> ApplicationContext.get().errors().report(this, "Load value from file", e));
				}
			});
		}
	}

	private void saveToFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.addChoosableFileFilter(new ExtensionFileFilter("text file", new String[]{".txt"}));
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setDialogTitle("Save value to file");

		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			Path target = FileChoosers.withExtension(chooser.getSelectedFile(), ".txt").toPath();
			String text = editor.getText() + System.lineSeparator();
			// The file is written off the event thread, a failure is reported on it.
			Thread.ofVirtual().name("save-cell-value").start(() -> {
				try {
					Files.writeString(target, text);
				} catch (Exception e) {
					SwingUtilities.invokeLater(() -> ApplicationContext.get().errors().report(this, "Save value to file", e));
				}
			});
		}
	}
}
