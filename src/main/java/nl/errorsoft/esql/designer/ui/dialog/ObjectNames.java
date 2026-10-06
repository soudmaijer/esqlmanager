package nl.errorsoft.esql.designer.ui.dialog;

import java.util.List;

import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseCard;
import nl.errorsoft.esql.designer.ui.diagram.ModelCard;
import nl.errorsoft.esql.designer.ui.diagram.TableCard;

/** The checks on the names of the model, its databases and its tables in the designer. Each returns the message to show, null when the name is fine. */
public final class ObjectNames {
	private ObjectNames() {
	}

	public static String modelProblem(String name) {
		return name == null || name.isBlank() ? "The model needs a name." : null;
	}

	/** A database needs a name that no other database of the model has (ignoring case). */
	public static String databaseProblem(Model model, DatabaseCard self, String name) {
		if (name == null || name.isBlank()) {
			return "The " + model.term() + " needs a name.";
		}
		for (ModelCard object : model.getObjects()) {
			if (object instanceof DatabaseCard other && other != self && other.getName().equalsIgnoreCase(name.trim())) {
				return "The model has a " + model.term() + " named '" + name.trim() + "' already.";
			}
		}
		return null;
	}

	/**
	 * A table needs a name that no other table in the same database has (ignoring case). Tables that are linked to no database are in one
	 * group, because they are created in the database the generator is pointed to.
	 */
	public static String tableProblem(Model model, TableCard self, String name) {
		if (name == null || name.isBlank()) {
			return "The table needs a name.";
		}
		List<DatabaseCard> databases = databasesOf(model, self);

		for (ModelCard object : model.getObjects()) {
			if (object instanceof TableCard other && other != self && other.getName().equalsIgnoreCase(name.trim()) && shareDatabase(databases,
				databasesOf(model, other))) {
				return "The model has a table named '" + name.trim() + "' in the same " + model.term() + " already.";
			}
		}
		return null;
	}

	private static boolean shareDatabase(List<DatabaseCard> first, List<DatabaseCard> second) {
		return first.isEmpty() && second.isEmpty() || first.stream().anyMatch(second::contains);
	}

	private static List<DatabaseCard> databasesOf(Model model, TableCard table) {
		return model.getReferences(table).stream().filter(DatabaseCard.class::isInstance).map(DatabaseCard.class::cast).toList();
	}
}
