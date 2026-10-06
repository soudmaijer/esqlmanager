package nl.errorsoft.esql.connection.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.app.ui.WindowTabsPanel;
import nl.errorsoft.esql.connection.control.ConnectionWindowController;

/**
 * One work view of a connection (table data, a table list, a query, an editor) as an internal frame of the main window with a tab of its own. Below the
 * view a bar shows what the view did last, and the paging of table data. Closing the frame (its tab's cross) is handed to {@code onClose}, which asks an
 * editor first.
 */
public class WorkFrame extends JInternalFrame {
	private static final String READY = "Ready"; // Only sizes the status bar

	private final ConnectionWindowController connection;
	private final String key;
	private final JPanel content = new JPanel(new BorderLayout());
	private final JLabel status = new JLabel(READY, SwingConstants.LEFT);
	private final JPanel navigation = new JPanel(new BorderLayout());
	private Component view;

	WorkFrame(ConnectionWindowController connection, String key, String title, Component view, Consumer<WorkFrame> onClose) {
		super(title, false, true);
		this.connection = connection;
		this.key = key;
		setFrameIcon(ApplicationContext.get().imageLoader().getIcon(connection.getConnectionProfile().getServerType().iconName()));
		putClientProperty(WindowTabsPanel.CONNECTION_TITLE, connection.getTitle());
		setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosing(InternalFrameEvent e) {
				onClose.accept(WorkFrame.this);
			}

			@Override
			public void internalFrameActivated(InternalFrameEvent e) {
				connection.showStatusInfo();
			}
		});

		// What the view did last on the left, cut with an ellipsis so it never pushes the paging away; the paging in the centre.
		status.setPreferredSize(new Dimension(0, status.getPreferredSize().height));
		status.setMinimumSize(new Dimension(0, 0));
		status.setText("");
		status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		status.setForeground(UIManager.getColor("Label.disabledForeground"));
		navigation.setVisible(false);
		JPanel statusBar = new JPanel(new GridBagLayout());
		statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")));
		GridBagConstraints cell = new GridBagConstraints();
		cell.fill = GridBagConstraints.HORIZONTAL;
		cell.weightx = 1;
		statusBar.add(status, cell);
		cell.gridx = 1;
		cell.weightx = 0;
		statusBar.add(navigation, cell);
		cell.gridx = 2;
		cell.weightx = 1;
		statusBar.add(Box.createHorizontalGlue(), cell);
		content.add(statusBar, BorderLayout.SOUTH);
		setContentPane(content);
		setView(view);
	}

	public ConnectionWindowController getConnection() {
		return connection;
	}

	/** What the view is found again by, such as the table it shows. */
	String getKey() {
		return key;
	}

	Component getView() {
		return view;
	}

	/** Replaces the view (the same table opened again) and its message. */
	void setView(Component newView) {
		if (view != null) {
			content.remove(view);
		}
		view = newView;
		content.add(newView, BorderLayout.CENTER);
		setNavigation(null);
		setMessage(null);
		content.revalidate();
		content.repaint();
	}

	/** The paging buttons of table data, null for none. */
	void setNavigation(JComponent bar) {
		navigation.removeAll();
		if (bar != null) {
			navigation.add(bar, BorderLayout.CENTER);
		}
		navigation.setVisible(bar != null);
		navigation.revalidate();
	}

	/** What the view did last; a view without a message leaves the bar empty. */
	void setMessage(String text) {
		boolean empty = text == null || text.isEmpty();
		status.setText(empty ? "" : text);
		status.setToolTipText(empty ? null : text);
	}

	@Override
	public String toString() {
		return getTitle();
	}
}
