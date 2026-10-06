package nl.errorsoft.esql.app.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The tab bar at the top of the main window with one tab per work window (table data, queries, editors, designers, help). The windows are internal frames on the
 * desktop, always maximized and without a title bar: the tab shows their icon and title, selecting a tab brings its frame to the front, and a frame that
 * becomes active selects its tab. The close cross (or a middle click) asks the frame to close the way its own close action does; the tab goes when the
 * window is removed. When two tabs have the same title, the tabs of windows of a connection name it: "orders (postgres@localhost)".
 */
public class WindowTabsPanel extends JPanel {
	/** The client property of a frame with the title of its connection, such as {@code postgres@localhost}. */
	public static final String CONNECTION_TITLE = "WindowTabsPanel.connection";

	private static final Logger log = LogManager.getLogger(WindowTabsPanel.class);

	private final JDesktopPane desktop;
	private final JTabbedPane tabs = new JTabbedPane();
	private final List<JInternalFrame> frames = new ArrayList<>();

	public WindowTabsPanel(JDesktopPane desktop) {
		super(new BorderLayout());
		this.desktop = desktop;
		tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabs.putClientProperty("JTabbedPane.tabClosable", true);
		tabs.putClientProperty("JTabbedPane.tabCloseToolTipText", "Close");
		tabs.putClientProperty("JTabbedPane.tabCloseCallback", (BiConsumer<JTabbedPane, Integer>) (pane, index) -> close(frames.get(index)));
		tabs.putClientProperty("JTabbedPane.showContentSeparator", false);
		tabs.addChangeListener(e -> showFrame(selectedFrame()));
		add(tabs, BorderLayout.CENTER);
		setVisible(false);
	}

	/** Puts a window on the desktop, maximized, with a tab of its own, and brings it to the front. */
	public void addWindow(JInternalFrame frame) {
		frame.setResizable(false);
		frame.setIconifiable(false);
		frame.setMaximizable(false);
		desktop.add(frame);
		hideTitleBar(frame);
		// The look and feel installs a new title bar when the theme changes.
		frame.addPropertyChangeListener("UI", e -> hideTitleBar(frame));
		frame.setVisible(true);
		try {
			frame.setMaximum(true);
		} catch (PropertyVetoException e) {
			log.warn("Could not maximize {}", frame.getTitle(), e);
		}

		frames.add(frame);
		tabs.addTab(frame.getTitle(), frame.getFrameIcon(), emptyContent());
		PropertyChangeListener titleChanged = e -> updateTab(frame);
		frame.addPropertyChangeListener(JInternalFrame.TITLE_PROPERTY, titleChanged);
		frame.addPropertyChangeListener(JInternalFrame.FRAME_ICON_PROPERTY, titleChanged);
		frame.addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameActivated(InternalFrameEvent e) {
				int index = frames.indexOf(frame);
				if (index >= 0 && tabs.getSelectedIndex() != index) {
					tabs.setSelectedIndex(index);
				}
			}
		});
		setVisible(true);
		updateTitles();
		tabs.setSelectedIndex(frames.size() - 1);
		showFrame(frame);
	}

	/** Takes a closed window off the desktop and removes its tab; the tab next to it comes to the front. */
	public void removeWindow(JInternalFrame frame) {
		int index = frames.indexOf(frame);
		if (index >= 0) {
			frames.remove(index);
			tabs.removeTabAt(index);
		}
		desktop.getDesktopManager().closeFrame(frame);
		frame.dispose();
		setVisible(!frames.isEmpty());
		updateTitles();
		showFrame(selectedFrame());
	}

	/** The open windows, in the order of their tabs. */
	public List<JInternalFrame> getFrames() {
		return List.copyOf(frames);
	}

	/** The window of the selected tab, null when none is open. */
	public JInternalFrame selectedFrame() {
		int index = tabs.getSelectedIndex();
		return index < 0 ? null : frames.get(index);
	}

	public void select(JInternalFrame frame) {
		int index = frames.indexOf(frame);
		if (index >= 0) {
			tabs.setSelectedIndex(index);
		}
	}

	/** Selects the next (+1) or previous (-1) tab, going round at the ends. */
	public void selectNext(int step) {
		if (!frames.isEmpty()) {
			tabs.setSelectedIndex(Math.floorMod(tabs.getSelectedIndex() + step, frames.size()));
		}
	}

	/** Asks the window to close as its own close action does (it may ask the user first). */
	private void close(JInternalFrame frame) {
		frame.doDefaultCloseAction();
	}

	private void updateTab(JInternalFrame frame) {
		int index = frames.indexOf(frame);
		if (index >= 0) {
			tabs.setIconAt(index, frame.getFrameIcon());
		}
		updateTitles();
	}

	/** The title of each tab, with the connection behind it when another tab has the same title. */
	private void updateTitles() {
		for (int i = 0; i < frames.size(); i++) {
			JInternalFrame frame = frames.get(i);
			String title = frame.getTitle();
			Object connection = frame.getClientProperty(CONNECTION_TITLE);
			boolean shared = frames.stream().filter(other -> other.getTitle().equals(title)).count() > 1;
			tabs.setTitleAt(i, shared && connection != null ? title + " (" + connection + ")" : title);
			tabs.setToolTipTextAt(i, connection != null ? title + " (" + connection + ")" : null);
		}
	}

	private void showFrame(JInternalFrame frame) {
		if (frame == null || frame.isSelected()) {
			return;
		}
		try {
			frame.setSelected(true);
		} catch (PropertyVetoException e) {
			log.debug("{} was not selected: {}", frame.getTitle(), e.getMessage());
		}
	}

	/** The tab shows the title, so the frame has no title bar or border of its own. Its menu bar (the designer's) stays. */
	private static void hideTitleBar(JInternalFrame frame) {
		if (frame.getUI() instanceof BasicInternalFrameUI ui) {
			ui.setNorthPane(null);
		}
		frame.setBorder(null);
	}

	/** The tabs only switch windows, the windows themselves are on the desktop below. */
	private static JComponent emptyContent() {
		JPanel empty = new JPanel();
		empty.setPreferredSize(new Dimension(0, 0));
		return empty;
	}
}
