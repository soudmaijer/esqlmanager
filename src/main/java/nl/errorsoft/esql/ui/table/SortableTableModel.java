package nl.errorsoft.esql.ui.table;

import nl.errorsoft.esql.table.TableCell;

import javax.swing.table.DefaultTableModel;

public class SortableTableModel extends DefaultTableModel {
	private int[] indexes;
	private SortItem[] items;

	public SortableTableModel() {

	}

	public Object getValueAt(int row, int column) {
		getIndexes();
		int rowIndex = row;
		if (indexes != null && row < indexes.length) {
			rowIndex = indexes[row];
		}
		return super.getValueAt(rowIndex, column);
	}

	public void setValueAt(Object value, int row, int column) {
		int rowIndex = row;
		if (indexes != null && row < indexes.length) {
			rowIndex = indexes[row];
		}
		super.setValueAt(value, rowIndex, column);
	}

	public void sortByColumn(int column, boolean ascend) {
		Sort.fastQuickSort(getSortItems(column));

		int itemCount = items.length;

		if (ascend) {
			for (int i = 0; i < itemCount; i++) {
				indexes[i] = items[i].getIndex();
			}
		} else {
			for (int i = 0; i < itemCount; i++) {
				indexes[i] = items[itemCount - i - 1].getIndex();
			}
		}
		this.fireTableDataChanged();
	}

	public SortItem[] getSortItems(int column) {
		getIndexes();
		items = new SortItem[indexes.length];

		for (int i = 0; i < items.length; i++) {
			items[i] = new SortItem(indexes[i], getValueAt(i, column));
		}
		return items;
	}

	public int[] getIndexes() {
		int rowCount = getRowCount();
		if (indexes != null) {
			if (indexes.length == rowCount) {
				return indexes;
			}
		}
		indexes = new int[rowCount];
		for (int i = 0; i < rowCount; i++) {
			indexes[i] = i;
		}
		return indexes;
	}
}

class Sort {
	public static void fastQuickSort(SortItem items[]) {
		int itemCount = items.length;

		QuickSort(items, 0, itemCount - 1);
		InsertionSort(items, 0, itemCount - 1);
	}

	private static void QuickSort(SortItem items[], int low, int high) {
		int cutoff = 4;
		int i;
		int j;
		SortItem pivot;

		if ((high - low) > cutoff) {
			i = (high + low) / 2;

			if (compare(items[low].getObject(), items[i].getObject()) > 0) {
				swap(items, low, i); // Tri-Median Methode!
			}
			if (compare(items[low].getObject(), items[high].getObject()) > 0) {
				swap(items, low, high);
			}
			if (compare(items[i].getObject(), items[high].getObject()) > 0) {
				swap(items, i, high);
			}

			j = high - 1;
			swap(items, i, j);
			i = low;
			pivot = items[j];
			for (;;) {
				while (compare(items[++i].getObject(), pivot.getObject()) < 0) {
				}
				while (compare(items[--j].getObject(), pivot.getObject()) > 0) {
				}
				if (j < i) {
					break;
				}
				swap(items, i, j);
			}
			swap(items, i, high - 1);
			QuickSort(items, low, j);
			QuickSort(items, i + 1, high);
		}
	}

	private static void swap(SortItem items[], int i, int j) {
		SortItem held;
		held = items[i];
		items[i] = items[j];
		items[j] = held;
	}

	private static void InsertionSort(SortItem items[], int low, int high) {
		int i;
		int j;
		SortItem pivot;

		for (i = low + 1; i <= high; i++) {
			pivot = items[i];
			j = i;

			while ((j > low) && (compare(items[j - 1].getObject(), pivot.getObject()) > 0)) {
				items[j] = items[j - 1];
				j--;
			}
			items[j] = pivot;
		}
	}

	public static int compare(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return 0;
		} else if (o1 == null) {
			return -1;
		} else if (o2 == null) {
			return 1;
		} else {
			TableCell a = (TableCell) o1;
			TableCell b = (TableCell) o2;

			Object native1 = a.getNativeData();
			Object native2 = b.getNativeData();

			if (native1 instanceof Number number && native2 instanceof Number number1) {
				return compare(number, number1);
			} else if (native1 instanceof java.util.Date date && native2 instanceof java.util.Date date1) {
				return compare(date, date1);
			} else {
				return (o1.toString().toLowerCase()).compareTo(o2.toString().toLowerCase());
			}
		}
	}

	public static int compare(Number o1, Number o2) {
		double n1 = o1.doubleValue();
		double n2 = o2.doubleValue();

		if (n1 < n2) {
			return -1;
		} else if (n1 > n2) {
			return 1;
		} else {
			return 0;
		}
	}

	public static int compare(java.util.Date o1, java.util.Date o2) {
		if (o1.before(o2)) {
			return -1;
		} else if (o1.after(o2)) {
			return 1;
		} else {
			return 0;
		}
	}
}

class SortItem {
	private int index;
	private Object value;

	public SortItem(int index, Object value) {
		this.index = index;
		this.value = value;
	}

	public int getIndex() {
		return index;
	}

	public Object getObject() {
		return value;
	}
}
