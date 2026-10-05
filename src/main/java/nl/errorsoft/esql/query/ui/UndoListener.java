package nl.errorsoft.esql.query.ui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.undo.UndoManager;

/** Keeps the undo manager of an editor up to date with its undoable edits. */
public class UndoListener implements UndoableEditListener {
	/**
		 * Messaged when the Document has created an edit, the edit is
	 * added to <code>undo</code>, an instance of UndoManager.
	 */
	private UndoManager ndo;

	public UndoListener(UndoManager ndo) {
		this.ndo = ndo;
	}

	public void undoableEditHappened(UndoableEditEvent e) {
		ndo.addEdit(e.getEdit());
	}
}
