package nl.errorsoft.esql.table;

/** The window of rows shown in the data tab: how many to skip and how many to show. All moves are pure, the table row count comes in as a parameter. */
public record Paging(int skip, int show) {

	public Paging first() {
		return new Paging(0, show);
	}

	public Paging previous() {
		return new Paging(Math.max(0, skip - show), show);
	}

	/** The next page, or this one when there are no more rows. */
	public Paging next(int rows) {
		int next = skip + show;
		return next >= rows ? this : new Paging(next, show);
	}

	/** The page that ends with the last row. */
	public Paging last(int rows) {
		return new Paging(Math.max(0, rows - show), show);
	}

	/** This page, moved back when it reaches beyond the last row. */
	public Paging clamped(int rows) {
		return skip + show > rows ? last(rows) : this;
	}
}
