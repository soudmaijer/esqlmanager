package nl.errorsoft.esql.settings.ui.dialog;

import java.awt.BorderLayout;
import java.io.File;
import java.nio.charset.Charset;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.app.control.MainController;
import nl.errorsoft.esql.settings.Appearance;
import nl.errorsoft.esql.settings.Settings;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.util.Encodings;
import nl.errorsoft.esql.ui.dialog.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/** The preferences of the application: the appearance, the editor font size, the folder file windows start in and the default file encoding. */
public class SettingsDialog extends FormDialog {
	private final JComboBox<Appearance> appearance = new JComboBox<>(Appearance.values());
	private final JSpinner fontSize;
	private final JTextField folder = new JTextField(24);
	private final JComboBox<Charset> encoding;

	public SettingsDialog(MainController mainController, JFrame parent) {
		super(parent, "Preferences", true);
		Settings settings = ApplicationContext.get().settings();
		appearance.setSelectedItem(settings.getAppearance());
		fontSize = new JSpinner(new SpinnerNumberModel(settings.getEditorFontSize(), Settings.MIN_FONT_SIZE, Settings.MAX_FONT_SIZE, 1));
		folder.setText(settings.getDefaultFolder());
		folder.setToolTipText("The export, import and file transfer windows start in this folder. Empty for the folder the system chooses.");
		encoding = Encodings.combo(settings.getDefaultEncoding());

		JButton browse = Forms.button("&Browse...");
		browse.addActionListener(e -> chooseFolder());
		JPanel folderRow = new JPanel(new BorderLayout(Forms.GAP, 0));
		folderRow.add(folder, BorderLayout.CENTER);
		folderRow.add(browse, BorderLayout.EAST);

		Forms.Grid form = new Forms.Grid().row("&Appearance:", appearance).row("Editor &font size:", fontSize)
			.row(Forms.label("Default &folder:", folder), folderRow).row("Default file &encoding:", encoding);
		setOkCancel(form.panel(), "&Save", "Cancel");
		setValidator(this::problem);
		setOnAccept(() -> {
			settings.setAppearance((Appearance) appearance.getSelectedItem());
			settings.setEditorFontSize((Integer) fontSize.getValue());
			settings.setDefaultFolder(folder.getText());
			settings.setDefaultEncoding((Charset) encoding.getSelectedItem());
			// The choices apply now even when they cannot be kept for the next start.
			settings.getAppearance().apply();
			EditorTheme.applyFontSize();
			try {
				settings.saveSettings();
			} catch (EsqlException e) {
				ApplicationContext.get().errors().report(getOwner(), "Save settings", e);
			}
		});
		setInitialFocus(appearance);
		showDialog();
	}

	/** The message for a default folder that does not exist, null when the input is fine. */
	private String problem() {
		String text = folder.getText().trim();
		return text.isEmpty() || new File(text).isDirectory() ? null : "The folder '" + text + "' does not exist.";
	}

	private void chooseFolder() {
		JFileChooser chooser = new JFileChooser(folder.getText().trim().isEmpty() ? null : new File(folder.getText().trim()));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Default folder");

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			folder.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}
}
