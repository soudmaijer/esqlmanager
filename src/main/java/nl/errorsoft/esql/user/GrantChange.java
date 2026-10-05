package nl.errorsoft.esql.user;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The planned change of a user's privileges on a target: what the server has granted when the plan was made, what the user wants, and the statements
 * that make the difference. The confirmation shows {@link #revoked()} of this plan, and exactly these statements run.
 */
public record GrantChange(DatabaseUser user, GrantTarget target, Set<String> current, Set<String> wanted, List<String> statements) {
	public GrantChange {
		current = Set.copyOf(current);
		wanted = Set.copyOf(wanted);
		statements = List.copyOf(statements);
	}

	/** The privileges the user has now and loses. */
	public Set<String> revoked() {
		Set<String> revoked = new LinkedHashSet<>(current);
		revoked.removeAll(wanted);
		return revoked;
	}
}
