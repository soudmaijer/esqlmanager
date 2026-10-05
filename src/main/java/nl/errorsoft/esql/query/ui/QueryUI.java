package nl.errorsoft.esql.query.ui;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.database.Database;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.util.ExtentionFileFilter;
import nl.errorsoft.esql.ui.icon.ImageLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class QueryUI extends JDialog implements ActionListener {
	private static final Logger log = LogManager.getLogger(QueryUI.class);

	private String db;
	private RSyntaxTextArea jt;

	// Internal toolbar
	private JToolBar tbQuery;
	private JButton btnSaveQuery;
	private JButton btnRunQuery;
	private JButton btnClose;
	private JCheckBox closeOnSuccess;
	private ImageLoader imgLoader;
	private ConnectionWindowCC cwcc;
	private JComboBox jcb;

	public QueryUI(ConnectionWindowCC cwcc, JFrame parent, ImageLoader imgLoader, java.util.Vector databases, Database d) {
		super(parent, false);

		// Set vars
		this.imgLoader = imgLoader;
		this.cwcc = cwcc;
		this.setTitle("Run SQL query on `" + cwcc.getTitle() + "`");

		// Load images

		// Toolbar
		tbQuery = new JToolBar();
		tbQuery.setFloatable(false);
		tbQuery.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		btnSaveQuery = new JButton(imgLoader.getIcon("imgSave"));
		btnSaveQuery.setToolTipText("Save query");
		btnSaveQuery.addActionListener(this);
		tbQuery.add(btnSaveQuery);
		btnClose = new JButton(imgLoader.getIcon("imgDeleteRow"));
		btnClose.setToolTipText("Close");
		btnClose.addActionListener(this);
		tbQuery.add(btnClose);
		btnRunQuery = new JButton(imgLoader.getIcon("imgDoRunQuery"));
		btnRunQuery.setToolTipText("Run query");
		btnRunQuery.addActionListener(this);
		tbQuery.add(btnRunQuery);
		tbQuery.addSeparator();

		tbQuery.add(new JLabel(" Database: "));
		jcb = new JComboBox(databases);
		jcb.setPreferredSize(new Dimension(150, 20));

		for (int i = 0; i < jcb.getItemCount(); i++) {
			if (((Database) jcb.getItemAt(i)).getName().equals(d.getName())) {
				jcb.setSelectedIndex(i);
			}
		}

		tbQuery.add(jcb);
		tbQuery.addSeparator();

		closeOnSuccess = new JCheckBox("Close when ready.");
		tbQuery.add(this.closeOnSuccess);

		this.getContentPane().add(tbQuery, java.awt.BorderLayout.NORTH);
		this.setSize((int) (parent.getToolkit().getScreenSize().getWidth() / 2), (int) (parent.getToolkit().getScreenSize().getHeight() / 4));
		this.setLocation(parent.getLocation().x + (int) ((parent.getSize().width - this.getSize().width) / 2),
			parent.getLocation().y + (int) ((parent.getSize().height - this.getSize().height) / 2));
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		jt = new RSyntaxTextArea();
		jt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		jt.setHighlightCurrentLine(false);
		jt.setCodeFoldingEnabled(false);
		jt.setRequestFocusEnabled(true);

		// RSyntaxTextArea brings undo and redo with the platform keys.
		RTextScrollPane jsp = new RTextScrollPane(jt);
		jsp.setLineNumbersEnabled(true);
		// After the scroll pane exists, so that the theme also colours the line numbers.
		EditorTheme.install(jt);
		jsp.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
		this.getContentPane().add(jsp);
		jt.requestFocus(true);

	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		if (src == this.btnSaveQuery) {
			JFileChooser chooser = new JFileChooser();
			chooser.addChoosableFileFilter(new ExtentionFileFilter("SQL file", new String[]{".sql"}));
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.setDialogTitle("Save query as...");

			try {
				if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					String fileName = chooser.getSelectedFile().getAbsolutePath();

					if (!fileName.endsWith(".sql")) {
						fileName = chooser.getSelectedFile().getAbsolutePath() + ".sql";
					}

					java.nio.file.Files.writeString(java.nio.file.Path.of(fileName), jt.getText() + System.lineSeparator());
				}
			} catch (Exception err) {
				ApplicationContext.get().errors().report(this, "Save query", err);
			}
		} else if (src == btnRunQuery) {
			if (jcb.getSelectedItem() != null && jcb.getSelectedItem() instanceof nl.errorsoft.esql.database.Database) {
				cwcc.changeDatabase((nl.errorsoft.esql.database.Database) jcb.getSelectedItem());
			}

			String s = "";
			java.util.StringTokenizer st = new java.util.StringTokenizer(jt.getText(), ";", false);
			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < jt.getDocument().getLength(); i++) {
				try {
					sb.append(jt.getDocument().getText(i, 1));

					if (sb.charAt(sb.length() - 1) == '\n') {
						String temp = sb.toString().substring(0, sb.length() - 1).trim();

						if (temp.endsWith(";")) {
							// execute the query.
							cwcc.runCustomSQL(temp);
							sb = new StringBuffer();
						}
					} else if (jt.getDocument().getLength() - 1 == i) {
						String temp = sb.toString().substring(0, sb.length()).trim();

						// execute the query.
						cwcc.runCustomSQL(temp);
						sb = new StringBuffer();
					}
				} catch (Exception ex) {
					ApplicationContext.get().errors().report(this, "Run query", ex);
				}
			}

			String temp = sb.toString().trim();

			if (temp.length() > 0) {
				// execute the query.
				cwcc.runCustomSQL(temp);
			}

			if (closeOnSuccess.isSelected()) {
				this.dispose();
			}
		} else if (src == btnClose) {
			this.dispose();
		}
	}
}
