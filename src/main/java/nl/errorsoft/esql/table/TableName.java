package nl.errorsoft.esql.table;

/**
 * The name of a table as a statement needs it: the schema it is in, null on servers without schemas (MySQL)
 * or when the connection's current schema is meant, and the table name. The dialect quotes it.
 */
public record TableName(String schema, String name) {
	/** A table in the current schema, or on a server without schemas. */
	public static TableName of(String name) {
		return new TableName(null, name);
	}

	/** Another table in the same schema, such as the table a foreign key refers to. */
	public TableName sibling(String otherName) {
		return new TableName(schema, otherName);
	}

	@Override
	public String toString() {
		return schema == null ? name : schema + "." + name;
	}
}
