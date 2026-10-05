package nl.errorsoft.esql.database.ui.dialog;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import nl.errorsoft.esql.dialect.DatabaseOption;
import nl.errorsoft.esql.ui.dialog.Dialogs;
import nl.errorsoft.esql.ui.util.Forms;

/** The Create database dialog: the name and the options of the server (character set, owner, ...), each chosen from the values the server offers. */
public final class CreateDatabaseDialog {
	/** What the user chose: the name and the value per option key, blank where the server default is wanted. */
	public record Request(String name, Map<String, String> options) {
	}

	private static final String SERVER_DEFAULT = "";

	private CreateDatabaseDialog() {
	}

	/**
	 * Asks for the name and options.
	 * @param term what the server calls a database ("database")
	 * @param options the options of the dialect
	 * @param choices the values per option key
	 * @param existing the names that are taken, the name must differ from all of them (ignoring case)
	 * @return null when the user cancels
	 */
	public static Request ask(Component parent, String term, List<DatabaseOption> options, Map<String, List<String>> choices, List<String> existing) {
		JTextField name = new JTextField(24);
		Forms.Grid grid = new Forms.Grid().row("&Name:", name);
		Map<String, JComboBox<String>> combos = new LinkedHashMap<>();

		for (DatabaseOption option : options) {
			JComboBox<String> combo = new JComboBox<>();
			combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
					return super.getListCellRendererComponent(list, SERVER_DEFAULT.equals(value) ? "(server default)" : value, index, selected, focus);
				}
			});
			combos.put(option.key(), combo);
			grid.row("&" + option.label() + ":", combo);
		}

		// A narrowed option (collation) lists the values that start with the chosen value of the other one (character set).
		for (DatabaseOption option : options) {
			Runnable fill = () -> fill(combos.get(option.key()), choices.getOrDefault(option.key(), List.of()),
				option.narrowedBy() == null ? "" : java.util.Objects.toString(combos.get(option.narrowedBy()).getSelectedItem(), ""));
			fill.run();
			if (option.narrowedBy() != null) {
				combos.get(option.narrowedBy()).addActionListener(e -> fill.run());
			}
		}

		boolean accepted = Dialogs.form(parent, "Create " + term, grid.panel(), "Create", name, () -> {
			String value = name.getText().trim();
			if (value.isEmpty()) {
				return "Enter a name for the " + term + ".";
			}
			return existing.stream().anyMatch(value::equalsIgnoreCase) ? "A " + term + " named '" + value + "' exists already." : null;
		});

		if (!accepted) {
			return null;
		}

		Map<String, String> chosen = new LinkedHashMap<>();
		combos.forEach((key, combo) -> chosen.put(key, java.util.Objects.toString(combo.getSelectedItem(), "")));
		return new Request(name.getText().trim(), chosen);
	}

	private static void fill(JComboBox<String> combo, List<String> values, String prefix) {
		combo.removeAllItems();
		combo.addItem(SERVER_DEFAULT);

		for (String value : values) {
			if (prefix.isEmpty() || value.startsWith(prefix + "_")) {
				combo.addItem(value);
			}
		}
	}
}
