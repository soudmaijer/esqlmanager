package nl.errorsoft.esql.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.dialect.mysql.MySqlDialect;
import nl.errorsoft.esql.dialect.postgres.PostgresDialect;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.TableName;

/** The type, length and engine of a table come from text the user typed or a model file, the dialect refuses what is not one. */
class ColumnDefinitionSafetyTest {
	private static final String HOSTILE_LENGTH = "10) ; DROP TABLE x; --";

	private final List<Dialect> dialects = List.of(new MySqlDialect(), new PostgresDialect());

	@Test
	void refusesALengthThatIsNotANumber() {
		for (Dialect dialect : dialects) {
			CreateColumn column = column("varchar", HOSTILE_LENGTH);
			assertThrows(EsqlException.class, () -> dialect.createTableSql(TableName.of("t"), List.of(column), null, ""));
			assertThrows(EsqlException.class, () -> dialect.addColumnSql(TableName.of("t"), column));
			assertThrows(EsqlException.class, () -> dialect.modifyColumnSql(TableName.of("t"), "a", column));
		}
	}

	@Test
	void refusesATypeNameThatIsNotAName() {
		for (Dialect dialect : dialects) {
			CreateColumn column = column("int); DROP TABLE x; --", "");
			assertThrows(EsqlException.class, () -> dialect.createTableSql(TableName.of("t"), List.of(column), null, ""));
		}
	}

	@Test
	void acceptsLengthsAndQuotesEnumValues() {
		for (Dialect dialect : dialects) {
			assertTrue(dialect.createTableSql(TableName.of("t"), List.of(column("decimal", "10, 2")), null, "").get(0).contains("decimal (10, 2)"));
			assertTrue(dialect.createTableSql(TableName.of("t"), List.of(column("character varying", "50")), null, "").get(0)
				.contains("character varying (50)"));
		}
		String sql = new MySqlDialect().createTableSql(TableName.of("t"), List.of(column("ENUM", "'a', 'it''s', b')")), null, "").get(0);
		assertTrue(sql.contains("ENUM ('a', 'it''s', 'b'')')"), sql);
		assertThrows(EsqlException.class,
			() -> new MySqlDialect().createTableSql(TableName.of("t"), List.of(column("ENUM", "'a') ; DROP TABLE x; --")), null, ""));
	}

	@Test
	void mysqlRefusesAnUnknownEngine() {
		MySqlDialect dialect = new MySqlDialect();
		assertThrows(EsqlException.class, () -> dialect.createTableSql(TableName.of("t"), List.of(column("int", "")), "InnoDB; DROP DATABASE shop", ""));
		assertThrows(EsqlException.class, () -> dialect.setTableTypeSql(TableName.of("t"), "InnoDB; DROP DATABASE shop"));
		assertEquals("ALTER TABLE `t` ENGINE=InnoDB", dialect.setTableTypeSql(TableName.of("t"), "innodb").get(0));
	}

	private static CreateColumn column(String type, String length) {
		CreateColumn column = new CreateColumn("c");
		column.type = DataType.named(type);
		column.length = length;
		return column;
	}
}
