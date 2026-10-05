package nl.errorsoft.esql.ui.editor;

import com.formdev.flatlaf.FlatLaf;
import nl.errorsoft.esql.app.ApplicationContext;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

/** Gives an {@link RSyntaxTextArea} the light or dark RSyntaxTextArea theme that matches the look and feel, also after the look and feel changes. */
public final class EditorTheme {
	private static final Logger log = LogManager.getLogger(EditorTheme.class);
	private static final String LIGHT = "/org/fife/ui/rsyntaxtextarea/themes/idea.xml";
	private static final String DARK = "/org/fife/ui/rsyntaxtextarea/themes/dark.xml";
	/** The editors that follow the theme and the font size of the preferences, until they are collected. */
	private static final Set<RSyntaxTextArea> EDITORS = Collections.newSetFromMap(new WeakHashMap<>());

	private EditorTheme() {
	}

	/** Applies the theme now and whenever the look and feel changes, until the editor's window is disposed. */
	public static void install(RSyntaxTextArea editor) {
		apply(editor);
		EDITORS.add(editor);

		// The look and feel updates the components after it is installed, so the theme is applied after that.
		PropertyChangeListener listener = e -> {
			if ("lookAndFeel".equals(e.getPropertyName())) {
				SwingUtilities.invokeLater(() -> apply(editor));
			}
		};
		UIManager.addPropertyChangeListener(listener);
		editor.addHierarchyListener(e -> {
			if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0 && !editor.isDisplayable()) {
				UIManager.removePropertyChangeListener(listener);
			}
		});
	}

	public static void apply(RSyntaxTextArea editor) {
		boolean dark = UIManager.getLookAndFeel() instanceof FlatLaf && FlatLaf.isLafDark();

		try (InputStream in = EditorTheme.class.getResourceAsStream(dark ? DARK : LIGHT)) {
			Theme.load(in).apply(editor);
		} catch (IOException e) {
			// The editor still works with the default colours.
			log.warn("Could not load the editor theme", e);
		}
		editor.setFont(font());
	}

	/** Gives every editor the font size of the preferences, for use after the preferences changed. Call on the event thread. */
	public static void applyFontSize() {
		for (RSyntaxTextArea editor : List.copyOf(EDITORS)) {
			editor.setFont(font());
		}
	}

	private static Font font() {
		return new Font(Font.MONOSPACED, Font.PLAIN, ApplicationContext.get().settings().getEditorFontSize());
	}
}
