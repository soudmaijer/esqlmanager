package nl.errorsoft.esql.table;

/** The outcome of a free query: a table describing the result columns and the rows. */
public record QueryResult(Table table, TableData[][] rows) {
}
