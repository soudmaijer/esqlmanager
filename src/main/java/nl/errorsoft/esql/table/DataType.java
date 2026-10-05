package nl.errorsoft.esql.table;

import java.util.EnumSet;
import java.util.Set;

/** A data type of a server (from datatypes.xml) and the column options it allows. */
public final class DataType {
	/** What a column of this type can be or have. */
	public enum Option {
		PRIMARY, INDEX, UNIQUE, BINARY, NOT_NULL, UNSIGNED, AUTO_INCREMENT, ZEROFILL
	}

	private final String name;
	private final Set<Option> options;

	public DataType(String name, Set<Option> options) {
		this.name = name;
		this.options = options.isEmpty() ? EnumSet.noneOf(Option.class) : EnumSet.copyOf(options);
	}

	/** A type that allows no options, such as one read back from a database. */
	public static DataType named(String name) {
		return new DataType(name, Set.of());
	}

	public boolean allows(Option option) {
		return options.contains(option);
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}
