package nl.errorsoft.esql.designer.control;

import nl.errorsoft.esql.designer.ui.DBCreator;

import java.util.Vector;
import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.ui.icon.ImageLoader;

public class ModelViewerControl {
	private DBCreator db;

	public ModelViewerControl(DBCreator db) {
		this.db = db;
	}

	public void showPropertiesDialog(Vector sel) {
		if (sel.size() == 1) {
			db.showProperties(sel.get(0));
		}
	}

	public void showModelPropertiesDialog(Object model) {
		db.showProperties(model);
	}

	public void buildMenu() {
		db.buildMenu();
	}

	public void saveModel(boolean direct) {
		db.saveCurrentModel(direct);
	}

	public void newModel() {
		db.newModel();
	}

	public void openModel() {
		db.openModel();
	}

	public void updateTitle() {
		db.updateTitle();
	}

	public ImageLoader getImageList() {
		return ApplicationContext.get().imageLoader();
	}

	public void saveModel() {
		this.db.saveCurrentModel(true);
	}

	public void generate() {
		this.db.generate();
	}
}
