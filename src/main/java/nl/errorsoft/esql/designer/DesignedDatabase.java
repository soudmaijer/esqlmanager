package nl.errorsoft.esql.designer;

import java.util.List;

/** A database as drawn in the designer, with its tables. */
public record DesignedDatabase(String name, List<DesignedTable> tables)
{
}
