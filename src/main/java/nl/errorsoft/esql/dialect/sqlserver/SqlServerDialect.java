package nl.errorsoft.esql.dialect.sqlserver;

import nl.errorsoft.esql.dialect.AbstractDialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.connection.ServerType;

public class SqlServerDialect extends AbstractDialect {
	public int getType() {
		return ServerType.MS_SQL_SERVER;
	}

	public String getDefaultPort() {
		return "1433";
	}

	public String getDefaultUsername() {
		return "sa";
	}

	public List<String> listDatabases(DatabaseConnection dbc) throws SQLException {
		List<String> names = new ArrayList<>();

		try (CallableStatement cs = dbc.getConnection().prepareCall("{call sp_databases}"); ResultSet rs = cs.executeQuery()) {
			while (rs.next()) {
				names.add(rs.getString(1));
			}
		}
		return names;
	}
}
