package nl.errorsoft.esql.designer.ui.dialog;

import java.util.List;

import nl.errorsoft.esql.designer.model.Model;
import nl.errorsoft.esql.designer.ui.diagram.DatabaseObject;
import nl.errorsoft.esql.designer.ui.diagram.ModelObject;
import nl.errorsoft.esql.designer.ui.diagram.TableObject;

/** The checks on the names of the model, its databases and its tables in the designer. Each returns the message to show, null when the name is fine. */
public final class ObjectNames {
	private ObjectNames() {
	}

	public static String modelProblem(String name) {
		return name == null || name.isBlank() ? "The model needs a name." : null;
	}

	/** A database needs a name that no other database of the model has (ignoring case). */
	public static String databaseProblem(Model model, DatabaseObject self, String name) {
		if (name == null || name.isBlank()) {
			return "The database needs a name.";
		}
		for (ModelObject object : model.getObjects()) {
			if (object instanceof DatabaseObject other && other != self && other.getName().equalsIgnoreCase(name.trim())) {
				return "The model has a database named '" + name.trim() + "' already.";
			}
		}
		return null;
	}

	/**
	 * A table needs a name that no other table in the same database has (ignoring case). Tables that are linked to no database are in one
	 * group, because they are created in the database the generator is pointed to.
	 */
	public static String tableProblem(Model model, TableObject self, String name) {
		if (name == null || name.isBlank()) {
			return "The table needs a name.";
		}
		List<DatabaseObject> databases = databasesOf(model, self);

		for (ModelObject object : model.getObjects()) {
			if (object instanceof TableObject other && other != self && other.getName().equalsIgnoreCase(name.trim()) && shareDatabase(databases,
				databasesOf(model, other))) {
				return "The model has a table named '" + name.trim() + "' in the same database already.";
			}
		}
		return null;
	}

	private static boolean shareDatabase(List<DatabaseObject> first, List<DatabaseObject> second) {
		return first.isEmpty() && second.isEmpty() || first.stream().anyMatch(second::contains);
	}

	private static List<DatabaseObject> databasesOf(Model model, TableObject table) {
		return model.getReferences(table).stream().filter(DatabaseObject.class::isInstance).map(DatabaseObject.class::cast).toList();
	}
}
