package nl.errorsoft.esql.ui.editor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.ui.util.Forms;
import nl.errorsoft.esql.ui.util.ToolbarButtons;

/**
 * A find bar below a text area: menu key+F opens it, typing searches, Enter finds the next match and Shift+Enter the previous one, all matches are
 * highlighted and "2 of 5" or "No results" says where you are. Esc or the cross closes it and gives the focus back to the text. The search itself is
 * RSyntaxTextArea's {@link SearchEngine}.
 */
public final class FindBar extends JPanel {
	private static final String OPEN = "esql.find";
	private final RTextArea area;
	private final JTextField field = new JTextField(22);
	private final JCheckBox matchCase = new JCheckBox("Match case");
	private final JLabel count = new JLabel(" ");

	private FindBar(RTextArea area) {
		super(new BorderLayout());
		this.area = area;
		var images = ApplicationContext.get().imageLoader();

		JLabel label = new JLabel("Find:");
		label.setDisplayedMnemonic('i');
		label.setLabelFor(field);
		JButton previous = iconButton(images.getIcon("imgPrev"), "Previous match (Shift+Enter)", KeyEvent.VK_P, () -> find(false));
		JButton next = iconButton(images.getIcon("imgNext"), "Next match (Enter)", KeyEvent.VK_N, () -> find(true));
		JButton close = iconButton(images.getIcon("imgClose"), "Close (Esc)", 0, this::close);
		matchCase.setMnemonic(KeyEvent.VK_C);
		matchCase.setFocusable(false);
		matchCase.addActionListener(e -> search());
		count.setForeground(UIManager.getColor("Label.disabledForeground"));

		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		row.add(label);
		row.add(field);
		row.add(previous);
		row.add(next);
		row.add(matchCase);
		row.add(count);
		add(row, BorderLayout.CENTER);
		add(close, BorderLayout.EAST);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
			BorderFactory.createEmptyBorder(0, 4, 0, 4)));

		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				search();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				search();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				search();
			}
		});
		bind(field, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next", () -> find(true));
		bind(field, JComponent.WHEN_FOCUSED, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "previous", () -> find(false));
		bind(this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close", this::close);
		setVisible(false);
	}

	/**
	 * Puts a find bar at the bottom of {@code host} (a {@link BorderLayout} panel that holds the text area) and binds menu key+F on the host, so the
	 * shortcut works while the text area or the bar has the focus.
	 */
	public static FindBar install(RTextArea area, JPanel host) {
		FindBar bar = new FindBar(area);
		host.add(bar, BorderLayout.SOUTH);
		KeyStroke find = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
		bind(host, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, find, OPEN, bar::open);
		bind(area, JComponent.WHEN_FOCUSED, find, OPEN, bar::open);
		return bar;
	}

	/** Shows the bar with the selected text (or the last search) in the field, and searches it. */
	public void open() {
		String selected = area.getSelectedText();
		if (selected != null && !selected.isEmpty() && !selected.contains("\n")) {
			field.setText(selected);
		}
		setVisible(true);
		revalidate();
		field.selectAll();
		field.requestFocusInWindow();
		search();
	}

	/** Closes the bar, removes the highlights and gives the focus back to the text. */
	public void close() {
		area.clearMarkAllHighlights();
		setVisible(false);
		area.requestFocusInWindow();
	}

	/** The query as it was typed: from where the current match starts, so that typing more keeps the same match when it still fits. */
	private void search() {
		area.setCaretPosition(Math.min(area.getSelectionStart(), area.getDocument().getLength()));
		find(true);
	}

	private void find(boolean forward) {
		String query = field.getText();
		if (query.isEmpty()) {
			area.clearMarkAllHighlights();
			showCount(" ", false);
			return;
		}
		SearchContext context = new SearchContext(query, matchCase.isSelected());
		context.setSearchForward(forward);
		context.setSearchWrap(true);
		context.setMarkAll(true);
		SearchEngine.find(area, context);
		var positions = FindMatches.positions(area.getText(), query, matchCase.isSelected());
		showCount(FindMatches.describe(positions, area.getSelectionStart()), positions.isEmpty());
	}

	private void showCount(String text, boolean none) {
		count.setText(text);
		count.setForeground(none ? Forms.errorColor() : UIManager.getColor("Label.disabledForeground"));
	}

	public JTextField getField() {
		return field;
	}

	private static JButton iconButton(javax.swing.Icon icon, String tooltip, int mnemonic, Runnable action) {
		JButton button = new JButton(icon);
		button.setToolTipText(tooltip);
		button.getAccessibleContext().setAccessibleName(tooltip);
		if (mnemonic != 0) {
			button.setMnemonic(mnemonic);
		}
		button.addActionListener(e -> action.run());
		ToolbarButtons.style(button);
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		return button;
	}

	private static void bind(JComponent component, int condition, KeyStroke key, String name, Runnable action) {
		component.getInputMap(condition).put(key, name);
		component.getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}
}
