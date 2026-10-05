package nl.errorsoft.esql.designer.ui.diagram;

import nl.errorsoft.esql.designer.control.DesignerCanvasController;

import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.*;
import javax.swing.event.*;

public class ModelBrowserPanel extends JTabbedPane implements ChangeListener { // List with DesignerCanvas objects
	private final List<DesignerCanvas> v = new ArrayList<>();

	private DesignerCanvasController canvasController;

	public ModelBrowserPanel(String name, DesignerCanvasController canvasController) {
		super();
		this.canvasController = canvasController;
		this.addModelViewer(name);
		this.addChangeListener(this);
	}

	public DesignerCanvas getCurrentModelViewer() {
		return (DesignerCanvas) v.get(this.getSelectedIndex());
	}

	public void addModelViewer(String name) {
		DesignerCanvas canvas = new DesignerCanvas(canvasController);
		JScrollPane jsp = new JScrollPane(canvas, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		jsp.getViewport().setBackground(UIManager.getColor("Panel.background"));
		this.addTab(name, jsp);
		this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		v.add(canvas);
	}

	public void stateChanged(ChangeEvent e) {
		canvasController.buildMenu();
	}
}
