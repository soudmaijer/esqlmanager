package nl.errorsoft.esql.designer.control;

import java.util.List;

import nl.errorsoft.esql.connection.control.ConnectionWindowController;
import nl.errorsoft.esql.designer.DesignedModel;
import nl.errorsoft.esql.designer.ModelCheck;
import nl.errorsoft.esql.designer.model.Model;

/** Checks a designer model and generates it on the server of a connection; the generate dialog shows the progress. */
public class GenerateController {
	private final ConnectionWindowController connectionWindowController;

	public GenerateController(ConnectionWindowController connectionWindowController) {
		this.connectionWindowController = connectionWindowController;
	}

	/** The model as plain records; call it on the event thread, the snapshot can then be checked and generated on any thread. */
	public DesignedModel snapshot(Model model) {
		return model.toDesigned();
	}

	public List<String> problems(ModelCheck step, DesignedModel model) {
		return step.problems(model);
	}

	/** Creates what is missing on the server; the step callback is called for every database, table and column. Not for the event thread. */
	public void generate(DesignedModel model, Runnable step) throws Exception {
		connectionWindowController.getContext().designer().generate(model.databases(), step);
	}

	/** Shows the databases again in the tree of the connection window after a generation; they are listed in the background. */
	public void reloadTree() {
		connectionWindowController.showDatabaseTree();
	}
}
