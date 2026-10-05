package nl.errorsoft.esql.importer.ui;

import nl.errorsoft.esql.error.Dialogs;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.importer.control.ImportCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import nl.errorsoft.esql.ui.util.Forms;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ImportSelectionUI extends JDialog implements ActionListener {
	private static final Logger log = LogManager.getLogger(ImportSelectionUI.class);

	JPanel jPanel1 = new JPanel();
	JRadioButton jRadioButton1 = new JRadioButton();
	JRadioButton jRadioButton2 = new JRadioButton();
	JLabel jLabel1 = new JLabel();
	JButton jButton1 = new JButton();
	JButton jButton2 = new JButton();
	private ESQLManagerUI jm;
	private ImportCC ecc;

	public ImportSelectionUI(ImportCC ecc, ESQLManagerUI jm) {
		try {
			this.jm = jm;
			this.ecc = ecc;
			jbInit();
		} catch (Exception ex) {
			ApplicationContext.get().errors().report(this, "Import", ex);
		}
	}

	void jbInit() throws Exception {
		this.setTitle("Import data");
		this.setResizable(false);
		jRadioButton1.setText("From a CSV comma-separated file");
		jRadioButton2.setText("From a file containing SQL statements");
		jLabel1.setFont(jLabel1.getFont().deriveFont(Font.BOLD));
		jLabel1.setText("Import data options:");
		jButton1.setText("Cancel");
		jButton1.addActionListener(this);
		jButton2.setText("Next");
		jButton2.addActionListener(this);
		jRadioButton1.setEnabled(false);
		jRadioButton2.setSelected(true);

		jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.Y_AXIS));
		jPanel1.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		jPanel1.add(jRadioButton2);
		jPanel1.add(jRadioButton1);
		JPanel root = Forms.padded(new JPanel(new BorderLayout(0, Forms.GAP)));
		root.add(jLabel1, BorderLayout.NORTH);
		root.add(jPanel1, BorderLayout.CENTER);
		root.add(Forms.buttonRow(jButton1, jButton2), BorderLayout.SOUTH);
		setContentPane(root);
		getRootPane().setDefaultButton(jButton2);
		pack();
		setLocationRelativeTo(jm);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.jButton1) {
			dispose();
		} else if (e.getSource() == this.jButton2) {
			if (!jRadioButton1.isSelected() && !jRadioButton2.isSelected()) {
				Dialogs.error(this, getTitle(), "Select an import option.");
			} else {
				// CSV
				if (jRadioButton1.isSelected()) {
					// SQL
				} else if (jRadioButton2.isSelected()) {
					ecc.startImportSQLUI();
				}
				this.dispose();
			}
		}
	}

}
