package nl.errorsoft.esql.designer.control;

import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.designer.ui.diagram.ModelCard;

import java.util.List;

public class DesignerCanvasController {
	private final DesignerWindow designerWindow;

	public DesignerCanvasController(DesignerWindow designerWindow) {
		this.designerWindow = designerWindow;
	}

	public void showPropertiesDialog(List<ModelCard> selection) {
		if (selection.size() == 1) {
			designerWindow.showProperties(selection.get(0));
		}
	}

	public void showModelPropertiesDialog(Object model) {
		designerWindow.showProperties(model);
	}

	public void buildMenu() {
		designerWindow.buildMenu();
	}

	public void saveModel(boolean direct) {
		designerWindow.saveCurrentModel(direct);
	}

	public void newModel() {
		designerWindow.newModel();
	}

	public void openModel() {
		designerWindow.openModel();
	}

	public void updateTitle() {
		designerWindow.updateTitle();
	}

	public void saveModel() {
		this.designerWindow.saveCurrentModel(true);
	}

	public void generate() {
		this.designerWindow.generate();
	}
}
