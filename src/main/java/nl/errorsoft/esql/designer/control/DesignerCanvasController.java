package nl.errorsoft.esql.designer.control;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.designer.ui.DesignerWindow;
import nl.errorsoft.esql.designer.ui.diagram.ModelObject;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import java.util.List;

public class DesignerCanvasController {
	private DesignerWindow designerWindow;

	public DesignerCanvasController(DesignerWindow designerWindow) {
		this.designerWindow = designerWindow;
	}

	public void showPropertiesDialog(List<ModelObject> sel) {
		if (sel.size() == 1) {
			designerWindow.showProperties(sel.get(0));
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

	public ImageLoader getImageList() {
		return ApplicationContext.get().imageLoader();
	}

	public void saveModel() {
		this.designerWindow.saveCurrentModel(true);
	}

	public void generate() {
		this.designerWindow.generate();
	}
}
