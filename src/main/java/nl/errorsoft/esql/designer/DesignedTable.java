package nl.errorsoft.esql.designer;

import java.util.List;

import nl.errorsoft.esql.domain.CreateColumn;

/** A table as drawn in the designer, ready to be created. */
public record DesignedTable( String name, String type, String comment, List<CreateColumn> columns )
{
}
