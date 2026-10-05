package nl.errorsoft.esql.help.ui;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import nl.errorsoft.esql.app.ApplicationContext;

/** The user documentation as a work window of the main window, with a tab of its own; it needs no connection. */
public class HelpWindow extends JInternalFrame {
	public static final String TITLE = "eSQLManager Help";

	/** @param onClose called when the user closes the window, it takes the window off the desktop */
	public HelpWindow(Runnable onClose) {
		super(TITLE, true, true);
		setFrameIcon(ApplicationContext.get().imageLoader().getIcon("imgHelpTab"));
		setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		add(new JScrollPane(new HelpPanel()));
		addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosing(InternalFrameEvent e) {
				onClose.run();
			}
		});
	}
}
