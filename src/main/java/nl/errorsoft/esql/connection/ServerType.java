package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.app.DataDirectory;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.dialect.Dialects;

import nl.errorsoft.esql.table.DataType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

public class ServerType {
	private static final Logger log = LogManager.getLogger(ServerType.class);

	public static final int MY_SQL = 0;
	public static final int MS_SQL_SERVER = 1;
	public static final int POSTGRES = 2;
	public static final int ORACLE = 3;
	//private String name;
	private String description;
	private String connectionURL;
	private String driverName;
	//ivate String defaultPortNumber;
	private int type;
	private String fieldOpenChar = "";
	private String fieldCloseChar = "";
	private String dataOpenChar = "";
	private String dataCloseChar = "";
	private DataType[] dt;

	public ServerType(int type) {
		this.type = type;
		DatabaseDriver d = new DatabaseDriver();
		DatabaseDriver[] da = d.getDatabaseDrivers();

		for (int i = 0; i < da.length; i++) {
			if (type == da[i].getId()) {
				description = da[i].getDriverName();
				connectionURL = da[i].getDriverURL();
				driverName = da[i].getDriverClassName();
				fieldOpenChar = da[i].getFieldOpenChar();
				fieldCloseChar = da[i].getFieldCloseChar();
				dataOpenChar = da[i].getDataOpenChar();
				dataCloseChar = da[i].getDataCloseChar();
			}
		}
	}

	public String getFieldOpenChar() {
		return fieldOpenChar;
	}

	public String getFieldCloseChar() {
		return fieldCloseChar;
	}

	public String getDataOpenChar() {
		return dataOpenChar;
	}

	public String getDataCloseChar() {
		return dataCloseChar;
	}

	public String getConnectionURL(ConnectionProfile cp, String database) {
		String url = connectionURL;

		url = url.replace("@host", cp.getHost());
		url = url.replace("@port", cp.getPort());
		url = url.replace("@username", cp.getUsername());
		url = url.replace("@password", cp.getPassword());
		url = url.replace("@database", getDialect().getConnectionDatabase(cp, database));

		return url;
	}

	public Dialect getDialect() {
		return Dialects.forType(type);
	}

	/** The name of this server's brand icon in the {@code ImageLoader}, the only place that maps a server type to an icon. */
	public String iconName() {
		return switch (type) {
			case POSTGRES -> "serverPostgres";
			case MY_SQL -> "serverMySql";
			case ORACLE -> "serverOracle";
			case MS_SQL_SERVER -> "serverSqlServer";
			default -> "pc";
		};
	}

	public String getDriverName() {
		return driverName;
	}

	public int getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public String toString() {
		return description;
	}

	public DataType[] getDataTypes() {
		if (dt != null && dt.length != 0) {
			return dt;
		}
		try {
			SAXBuilder builder = new SAXBuilder();
			org.jdom.Document sdata = builder.build(DataDirectory.file("conf/datatypes.xml"));

			// The file holds the datatypes of every server, pick the section of this one.
			for (Object driver : sdata.getRootElement().getChildren("driver")) {
				if (Integer.parseInt(((Element) driver).getAttributeValue("id")) != this.getType()) {
					continue;
				}

				java.util.List<?> types = ((Element) driver).getChildren("type");
				this.dt = new DataType[types.size()];

				for (int i = 0; i < types.size(); i++) {
					Element type = (Element) types.get(i);
					this.dt[i] = new DataType(type.getChildText("name"), flag(type, "primary"), flag(type, "index"), flag(type, "unique"),
						flag(type, "binary"),
						flag(type, "notnull"), flag(type, "unsigned"), flag(type, "autoincrement"), flag(type, "zerofill"));
				}
				return this.dt;
			}
			log.warn("No datatypes specified for server type {}", this.getType());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return new DataType[0];
	}

	private boolean flag(Element type, String name) {
		return "true".equalsIgnoreCase(type.getChildText(name));
	}

	public static ServerType[] getServerTypes() {
		ServerType[] st = new ServerType[4];

		st[0] = new ServerType(ServerType.MY_SQL);
		st[1] = new ServerType(ServerType.MS_SQL_SERVER);
		st[2] = new ServerType(ServerType.POSTGRES);
		st[3] = new ServerType(ServerType.ORACLE);

		return st;
	}
}
