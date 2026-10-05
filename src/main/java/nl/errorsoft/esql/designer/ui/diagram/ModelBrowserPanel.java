package nl.errorsoft.esql.designer.ui.diagram;

import nl.errorsoft.esql.designer.control.ModelViewerControl;

import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.*;
import javax.swing.event.*;

public class ModelBrowserPanel extends JTabbedPane implements ChangeListener { // List with DesignerCanvas objects
	private final List<DesignerCanvas> v = new ArrayList<>();

	private ModelViewerControl mvc;

	public ModelBrowserPanel(String name, ModelViewerControl mvc) {
		super();
		this.mvc = mvc;
		this.addModelViewer(name);
		this.addChangeListener(this);
	}

	public DesignerCanvas getCurrentModelViewer() {
		return (DesignerCanvas) v.get(this.getSelectedIndex());
	}

	public void addModelViewer(String name) {
		DesignerCanvas mv = new DesignerCanvas(mvc);
		JScrollPane jsp = new JScrollPane(mv, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		jsp.getViewport().setBackground(UIManager.getColor("Panel.background"));
		this.addTab(name, jsp);
		this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		v.add(mv);
	}

	public void stateChanged(ChangeEvent e) {
		mvc.buildMenu();
	}
}
