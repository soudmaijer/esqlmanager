package esql.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.undo.UndoManager;

/** inner class for handling undoable edit events */  
public class UndoHandler implements UndoableEditListener 
{    
	/**     
		 * Messaged when the Document has created an edit, the edit is     
	 * added to <code>undo</code>, an instance of UndoManager.     
	 */    
	private UndoManager ndo;
	
	public UndoHandler( UndoManager ndo )
	{
		this.ndo = ndo;
	}
	
	public void undoableEditHappened(UndoableEditEvent e) 
	{      
		ndo.addEdit(e.getEdit());    
	}  
}