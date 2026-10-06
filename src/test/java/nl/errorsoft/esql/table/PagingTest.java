package nl.errorsoft.esql.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PagingTest {

	@Test
	void firstAndPreviousStopAtZero() {
		assertEquals(new Paging(0, 10), new Paging(25, 10).first());
		assertEquals(new Paging(15, 10), new Paging(25, 10).previous());
		assertEquals(new Paging(0, 10), new Paging(5, 10).previous());
	}

	@Test
	void nextStaysWhenThereAreNoMoreRows() {
		assertEquals(new Paging(10, 10), new Paging(0, 10).next(25));
		assertEquals(new Paging(10, 10), new Paging(10, 10).next(20));
		assertEquals(new Paging(0, 10), new Paging(0, 10).next(10));
	}

	@Test
	void lastEndsWithTheLastRow() {
		assertEquals(new Paging(15, 10), new Paging(0, 10).last(25));
		assertEquals(new Paging(0, 10), new Paging(0, 10).last(4));
	}

	@Test
	void clampedMovesBackOnlyWhenBeyondTheRows() {
		assertEquals(new Paging(10, 10), new Paging(10, 10).clamped(30));
		assertEquals(new Paging(15, 10), new Paging(30, 10).clamped(25));
		assertEquals(new Paging(0, 10), new Paging(5, 10).clamped(3));
	}
}
