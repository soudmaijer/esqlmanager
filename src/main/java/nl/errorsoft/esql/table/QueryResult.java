package nl.errorsoft.esql.table;

import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;

import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableData;

/** The outcome of a free query: a table describing the result columns and the rows. */
public record QueryResult(Table table, TableData[][] rows) {
}
