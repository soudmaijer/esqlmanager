package nl.errorsoft.esql.server.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.dialog.FormDialog;
import nl.errorsoft.esql.ui.util.Forms;

/** The full statement of a process in a read-only editor with SQL colouring, with a Copy button. */
public class ProcessQueryDialog extends FormDialog {
	public ProcessQueryDialog(Window owner, String heading, String statement) {
		super(owner, "Statement of " + heading, false);
		setResizable(true);

		RSyntaxTextArea text = new RSyntaxTextArea(statement == null ? "" : statement);
		text.setEditable(false);
		text.setLineWrap(true);
		text.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		EditorTheme.install(text);
		text.setCaretPosition(0);
		JScrollPane scroll = new JScrollPane(text);
		scroll.setPreferredSize(new Dimension(640, 320));

		JPanel content = new JPanel(new BorderLayout(0, Forms.GAP));
		content.add(new JLabel(heading), BorderLayout.NORTH);
		content.add(scroll, BorderLayout.CENTER);

		JButton copy = Forms.button("&Copy");
		copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.getText()), null));
		JButton close = Forms.button("C&lose");
		close.addActionListener(e -> dispose());
		layoutDialog(content, copy, close);
		setInitialFocus(close);
	}
}
