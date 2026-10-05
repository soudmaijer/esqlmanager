package nl.errorsoft.esql.settings.ui;

import javax.swing.JComboBox;
import javax.swing.JFrame;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.control.ESQLManagerCC;
import nl.errorsoft.esql.settings.Appearance;
import nl.errorsoft.esql.ui.util.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/** The preferences of the application: the appearance. */
public class SettingsUI extends FormDialog {
	private final JComboBox<Appearance> appearance = new JComboBox<>(Appearance.values());

	public SettingsUI(ESQLManagerCC jmcc, JFrame parent) {
		super(parent, "Preferences", true);
		appearance.setSelectedItem(ApplicationContext.get().settings().getAppearance());

		setOkCancel(new Forms.Grid().row("&Appearance:", appearance).panel(), "&Save", "Cancel");
		setOnAccept(() -> {
			ApplicationContext.get().settings().setAppearance((Appearance) appearance.getSelectedItem());
			ApplicationContext.get().settings().saveSettings();
			ApplicationContext.get().settings().getAppearance().apply();
		});
		setInitialFocus(appearance);
		showDialog();
	}
}
