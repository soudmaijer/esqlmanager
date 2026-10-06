package nl.errorsoft.esql.query.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import nl.errorsoft.esql.query.control.QueryController;
import nl.errorsoft.esql.query.plan.Plan;
import nl.errorsoft.esql.query.plan.PlanFormat;
import nl.errorsoft.esql.query.plan.PlanHints;
import nl.errorsoft.esql.query.plan.PlanNode;
import nl.errorsoft.esql.ui.editor.EditorTheme;
import nl.errorsoft.esql.ui.util.Forms;

/**
 * The plan of a statement in a result tab of the query tab. The Plan tab is a tree table of the steps (rows estimated and actual, loops, cost, time and
 * the share of each step as a bar), the hottest steps in the theme's warning colour, a marker on steps with a hint, and the details of the selected step
 * beside it; the Text tab has the plan as the server gave it, with Copy plan.
 */
class PlanView extends JPanel {
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final int INDENT = 16;

	PlanView(QueryController.ExplainResult explained) {
		super(new BorderLayout());
		String raw = explained.raw().text();
		JTabbedPane views = new JTabbedPane();
		views.putClientProperty("JTabbedPane.tabHeight", 24);
		if (explained.plan() != null) {
			views.addTab("Plan", planPanel(explained.plan()));
		}
		views.addTab("Text", textPanel(raw));

		String when = (explained.analyzed() ? "Analyzed" : "Explained") + " at " + TIME.format(explained.ranAt())
			+ (explained.database().isEmpty() ? "" : " on " + explained.database()) + " in " + explained.millis() + " ms";
		String summary = explained.plan() == null ? when : explained.plan().summary() + ". " + when;
		JLabel line = new JLabel(summary);
		line.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
		add(views, BorderLayout.CENTER);
		add(line, BorderLayout.SOUTH);
	}

	private static JComponent planPanel(Plan plan) {
		PlanTableModel model = new PlanTableModel(plan);
		Set<PlanNode> hot = plan.hottest();
		JTable table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setShowVerticalLines(false);
		table.setFillsViewportHeight(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.setRowHeight(Math.max(table.getRowHeight(), 22));
		table.getColumnModel().getColumn(PlanTableModel.OPERATION).setCellRenderer(new OperationRenderer(model));
		DefaultTableCellRenderer numbers = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
				super.getTableCellRendererComponent(t, value, selected, focus, row, column);
				setHorizontalAlignment(RIGHT);
				boolean isHot = hot.contains(model.row(row).node()) && column == 4;
				setFont(isHot ? getFont().deriveFont(Font.BOLD) : getFont());
				if (!selected) {
					setForeground(isHot ? Forms.warningColor() : t.getForeground());
				}
				return this;
			}
		};
		for (int column = 1; column < PlanTableModel.SHARE; column++) {
			table.getColumnModel().getColumn(column).setCellRenderer(numbers);
		}
		table.getColumnModel().getColumn(PlanTableModel.SHARE).setCellRenderer(new ShareRenderer(model, hot));
		int[] widths = {360, 110, 60, 80, 90, 110};
		for (int column = 0; column < widths.length; column++) {
			table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
		}
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				int column = table.columnAtPoint(e.getPoint());
				if (row < 0 || column != PlanTableModel.OPERATION) {
					return;
				}
				int arrowEnd = table.getCellRect(row, column, false).x + (model.row(row).depth() + 1) * INDENT + 4;
				if (e.getClickCount() == 2 || e.getX() < arrowEnd) {
					PlanNode node = model.row(row).node();
					model.toggle(row);
					for (int i = 0; i < model.getRowCount(); i++) {
						if (model.row(i).node() == node) {
							table.setRowSelectionInterval(i, i);
						}
					}
				}
			}
		});

		JTextPane details = new JTextPane();
		details.setContentType("text/html");
		details.setEditable(false);
		// FlatLaf paints a read-only text pane grey; the details read like the table.
		details.setBackground(UIManager.getColor("Table.background"));
		details.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		details.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
		table.getSelectionModel().addListSelectionListener(e -> {
			int row = table.getSelectedRow();
			details.setText(row < 0 ? "" : detailsHtml(plan, model.row(row).node()));
			details.setCaretPosition(0);
		});
		if (model.getRowCount() > 0) {
			table.setRowSelectionInterval(0, 0);
		}
		JScrollPane detailsScroll = new JScrollPane(details);
		detailsScroll.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setBorder(BorderFactory.createEmptyBorder());
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, detailsScroll);
		split.setResizeWeight(0.72);
		split.setContinuousLayout(true);
		split.setBorder(BorderFactory.createEmptyBorder());
		return split;
	}

	private static JComponent textPanel(String raw) {
		RSyntaxTextArea text = new RSyntaxTextArea(raw);
		String start = raw.strip();
		text.setSyntaxEditingStyle(start.startsWith("[") || start.startsWith("{") ? SyntaxConstants.SYNTAX_STYLE_JSON : SyntaxConstants.SYNTAX_STYLE_NONE);
		text.setEditable(false);
		text.setHighlightCurrentLine(false);
		text.setCodeFoldingEnabled(false);
		text.setCaretPosition(0);
		RTextScrollPane scroll = new RTextScrollPane(text, false);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		EditorTheme.install(text);
		JButton copy = new JButton("Copy plan");
		copy.setMnemonic('C');
		copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(raw), null));
		JPanel buttons = new JPanel(new BorderLayout());
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		buttons.add(copy, BorderLayout.EAST);
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.SOUTH);
		return panel;
	}

	/** The details of a step: what it reads, its conditions, buffers, how far the estimate was off, the hints and everything else the server said. */
	static String detailsHtml(Plan plan, PlanNode node) {
		StringBuilder html = new StringBuilder("<html><body style='font-family:sans-serif'>");
		html.append("<b>").append(escape(node.operation())).append("</b><br>");
		row(html, "Relation", node.relation());
		row(html, "Index", node.index());
		for (String condition : node.conditions()) {
			int colon = condition.indexOf(": ");
			row(html, colon < 0 ? "Condition" : condition.substring(0, colon), colon < 0 ? condition : condition.substring(colon + 2));
		}
		row(html, "Rows",
			PlanTableModel.rowsText(node).isEmpty() ? null : PlanTableModel.rowsText(node) + (node.actual() == null ? " estimated" : " (estimated / actual)"));
		Double error = node.estimateError();
		if (error != null && error > 1.5) {
			row(html, "Estimate", "off by " + Math.round(error) + "x");
		}
		if (node.actual() != null) {
			row(html, "Time", PlanFormat.millis(node.actual().totalTime()) + ", of which " + PlanFormat.millis(node.exclusiveTime()) + " in this step");
		}
		if (node.buffers() != null) {
			row(html, "Buffers", PlanFormat.number(node.buffers().hit()) + " hit, " + PlanFormat.number(node.buffers().read()) + " read");
		}
		row(html, "Share", Math.round(plan.share(node) * 100) + "% of the " + (plan.analyzed() ? "time" : "cost"));
		List<String> hints = PlanHints.of(node);
		for (String hint : hints) {
			html.append("<br><span>&#9888; ").append(escape(hint)).append("</span>");
		}
		if (!node.details().isEmpty()) {
			html.append("<br>");
			for (Map.Entry<String, String> detail : node.details().entrySet()) {
				row(html, detail.getKey(), detail.getValue());
			}
		}
		return html.append("</body></html>").toString();
	}

	private static void row(StringBuilder html, String key, String value) {
		if (value != null && !value.isEmpty()) {
			html.append(escape(key)).append(": ").append(escape(value)).append("<br>");
		}
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** The operation indented by its depth, with an arrow when it has children and a warning marker (hints as tooltip) when there are hints. */
	private static final class OperationRenderer extends DefaultTableCellRenderer {
		private final PlanTableModel model;

		OperationRenderer(PlanTableModel model) {
			this.model = model;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
			super.getTableCellRendererComponent(table, value, selected, focus, row, column);
			PlanTableModel.Row line = model.row(row);
			PlanNode node = line.node();
			String arrow = node.children().isEmpty() ? "    " : model.isCollapsed(node) ? "▸ " : "▾ ";
			setText(arrow + value);
			setBorder(BorderFactory.createEmptyBorder(0, 4 + line.depth() * INDENT, 0, 4));
			List<String> hints = PlanHints.of(node);
			setIcon(hints.isEmpty() ? null : HintIcon.INSTANCE);
			setHorizontalTextPosition(LEADING);
			setIconTextGap(6);
			setToolTipText(hints.isEmpty() ? null : "<html>" + String.join("<br>", hints.stream().map(PlanView::escape).toList()) + "</html>");
			return this;
		}
	}

	/** The share of a step as a bar behind its percentage, in the accent colour, or the warning colour for a hot step. */
	private static final class ShareRenderer extends JComponent implements TableCellRenderer {
		private final PlanTableModel model;
		private final Set<PlanNode> hot;
		private double share;
		private boolean isHot;
		private boolean selected;
		private JTable table;

		ShareRenderer(PlanTableModel model, Set<PlanNode> hot) {
			this.model = model;
			this.hot = hot;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
			this.table = table;
			this.share = value instanceof Double d ? d : 0;
			this.isHot = hot.contains(model.row(row).node());
			this.selected = selected;
			return this;
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setColor(selected ? table.getSelectionBackground() : table.getBackground());
				g2.fillRect(0, 0, getWidth(), getHeight());
				int barWidth = getWidth() - 46;
				int barHeight = 8;
				int y = (getHeight() - barHeight) / 2;
				Color track = UIManager.getColor("ProgressBar.background");
				g2.setColor(track != null ? track : table.getGridColor());
				g2.fillRoundRect(4, y, barWidth, barHeight, barHeight, barHeight);
				Color accent = UIManager.getColor("Component.accentColor");
				g2.setColor(isHot ? Forms.warningColor() : accent != null ? accent : table.getSelectionBackground());
				g2.fillRoundRect(4, y, (int) Math.round(barWidth * Math.min(1, share)), barHeight, barHeight, barHeight);
				g2.setFont(table.getFont());
				g2.setColor(selected ? table.getSelectionForeground() : table.getForeground());
				String text = Math.round(share * 100) + "%";
				int textWidth = g2.getFontMetrics().stringWidth(text);
				g2.drawString(text, getWidth() - 4 - textWidth, (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
			} finally {
				g2.dispose();
			}
		}
	}

	/** A small warning triangle in the theme's warning colour. */
	private static final class HintIcon implements Icon {
		static final HintIcon INSTANCE = new HintIcon();

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(Forms.warningColor());
				g2.fill(new Polygon(new int[]{x + 7, x + 14, x}, new int[]{y + 1, y + 13, y + 13}, 3));
				g2.setColor(UIManager.getColor("Table.background"));
				g2.setStroke(new BasicStroke(1.6f));
				g2.drawLine(x + 7, y + 5, x + 7, y + 9);
				g2.drawLine(x + 7, y + 11, x + 7, y + 11);
			} finally {
				g2.dispose();
			}
		}

		@Override
		public int getIconWidth() {
			return 14;
		}

		@Override
		public int getIconHeight() {
			return 14;
		}
	}
}
