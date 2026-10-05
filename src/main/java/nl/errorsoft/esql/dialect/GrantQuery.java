package nl.errorsoft.esql.dialect;

import java.util.List;

/**
 * The query that reads the privileges of an account on a target.
 * @param parameters bound in order to the placeholders of the query.
 * @param database the database to make active before running it, null when any will do.
 */
public record GrantQuery(String sql, List<String> parameters, String database) {
}
