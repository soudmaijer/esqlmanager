package nl.errorsoft.esql.designer;

import nl.errorsoft.esql.connection.ServerType;

/** Whether a model may be generated on a connection: only on the kind of server it is designed for. Pure, no Swing. */
public final class ModelTarget {
	private ModelTarget() {
	}

	/**
	 * Why the model cannot be generated on a connection to this server, null when it can: a model for one server is not generated on another. A model
	 * whose server is unknown (null, a file written before 0.3) fits any.
	 */
	public static String refusal(ServerType model, ServerType connection) {
		if (model == null || model.getType() == connection.getType()) {
			return null;
		}
		return "The model is designed for " + model.getDescription() + " and cannot be generated on " + connection.getDescription()
			+ ". Generate it on a " + model.getDescription() + " connection.";
	}
}
