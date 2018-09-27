//Source file: d:\\roseoutput\\esql\\esql\\table\\MultiLineCellEditor.java

package nl.errorsoft.esqlmanager.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;

public class MultiLineCellEditor extends AbstractCellEditor implements TableCellEditor 
{
	private final Border focusBorder = new LineBorder( Color.RED );
	private final Border emptyBorder = BorderFactory.createEmptyBorder(0,0,0,0);
	private JTextArea editorComponent;
	private JScrollPane pane;

   /**
    * @roseuid 3E05A70C0100
    */
	public MultiLineCellEditor( JTextArea editorComponent ) 
   {
		this.editorComponent = editorComponent;
		this.editorComponent.setBorder(emptyBorder);
		pane = new JScrollPane( this.editorComponent, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		pane.setBorder( emptyBorder );
	}
		
	/*
	 *	Override method in TableCellEditor, returns the value of the selected cell.
	 */
	public Object getCellEditorValue() 
	{
		return editorComponent.getText();
	}
	
	/*
	 * Get the complete component for overriding settings in constructor and adding Mouse and KeyListeners.
	 */
	public Component getTableCellEditorComponent()
	{
		return this.editorComponent;
	}	
	
	/*
	 *	Override method in TableCellEditor, returns the EditorComponent for the selected Cell.
	 */
	public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row, int column ) 
	{
		editorComponent.setBorder( focusBorder );
		editorComponent.setText((value != null) ? value.toString() : "");
		editorComponent.grabFocus();
		editorComponent.selectAll();
		return editorComponent;
	}
}