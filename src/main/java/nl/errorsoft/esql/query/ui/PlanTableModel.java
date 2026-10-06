package nl.errorsoft.esql.query.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import nl.errorsoft.esql.query.plan.Plan;
import nl.errorsoft.esql.query.plan.PlanFormat;
import nl.errorsoft.esql.query.plan.PlanNode;

/** The steps of a plan as rows of a tree table: a step is followed by its children, which are left out while it is collapsed. */
class PlanTableModel extends AbstractTableModel {
	static final int OPERATION = 0;
	static final int SHARE = 5;
	private static final String[] COLUMNS = {"Operation", "Rows", "Loops", "Cost", "Time", "% of total"};

	/** A visible step and how deep it is in the tree. */
	record Row(PlanNode node, int depth) {
	}

	private final Plan plan;
	private final Set<PlanNode> collapsed = Collections.newSetFromMap(new IdentityHashMap<>());
	private final List<Row> rows = new ArrayList<>();

	PlanTableModel(Plan plan) {
		this.plan = plan;
		rebuild();
	}

	Plan plan() {
		return plan;
	}

	Row row(int index) {
		return rows.get(index);
	}

	boolean isCollapsed(PlanNode node) {
		return collapsed.contains(node);
	}

	void toggle(int index) {
		PlanNode node = rows.get(index).node();
		if (node.children().isEmpty()) {
			return;
		}
		if (!collapsed.remove(node)) {
			collapsed.add(node);
		}
		rebuild();
		fireTableDataChanged();
	}

	private void rebuild() {
		rows.clear();
		add(plan.root(), 0);
	}

	private void add(PlanNode node, int depth) {
		rows.add(new Row(node, depth));
		if (!collapsed.contains(node)) {
			node.children().forEach(child -> add(child, depth + 1));
		}
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return COLUMNS.length;
	}

	@Override
	public String getColumnName(int column) {
		return COLUMNS[column];
	}

	@Override
	public Object getValueAt(int index, int column) {
		PlanNode node = rows.get(index).node();
		return switch (column) {
			case OPERATION -> node.operation() + (node.relation() == null ? "" : " on " + node.relation())
				+ (node.index() == null ? "" : " using " + node.index());
			case 1 -> rowsText(node);
			case 2 -> node.actual() == null ? "" : PlanFormat.number(node.actual().loops());
			case 3 -> node.estimate() == null || node.estimate().totalCost() == null ? "" : PlanFormat.number(node.estimate().totalCost());
			case 4 -> node.actual() == null ? "" : PlanFormat.millis(node.actual().totalTime());
			default -> plan.share(node);
		};
	}

	/** Estimated rows, and when analyzed the actual rows after a slash: "3 / 286". */
	static String rowsText(PlanNode node) {
		String estimated = node.estimate() == null || node.estimate().rows() == null ? "" : PlanFormat.number(node.estimate().rows());
		if (node.actual() == null) {
			return estimated;
		}
		return (estimated.isEmpty() ? "" : estimated + " / ") + PlanFormat.number(node.actual().rows());
	}
}
