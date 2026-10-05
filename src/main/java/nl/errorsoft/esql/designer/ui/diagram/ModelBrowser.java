package nl.errorsoft.esql.designer.ui.diagram;

import nl.errorsoft.esql.designer.control.ModelViewerControl;

import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.*;
import javax.swing.event.*;

public class ModelBrowser extends JTabbedPane implements ChangeListener { // List with ModelViewer objects
	private final List<ModelViewer> v = new ArrayList<>();

	private ModelViewerControl mvc;

	public ModelBrowser(String name, ModelViewerControl mvc) {
		super();
		this.mvc = mvc;
		this.addModelViewer(name);
		this.addChangeListener(this);
	}

	public ModelViewer getCurrentModelViewer() {
		return (ModelViewer) v.get(this.getSelectedIndex());
	}

	public void addModelViewer(String name) {
		ModelViewer mv = new ModelViewer(mvc);
		JScrollPane jsp = new JScrollPane(mv, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		jsp.getViewport().setBackground(Color.white);
		this.addTab(name, jsp);
		this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		v.add(mv);
	}

	public void stateChanged(ChangeEvent e) {
		mvc.buildMenu();
	}
}
