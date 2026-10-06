package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.app.DataDirectory;
import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.dialect.DialectFactory;
import nl.errorsoft.esql.driver.DriverArtifact;
import nl.errorsoft.esql.driver.DriverSource;

import nl.errorsoft.esql.table.DataType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import nl.errorsoft.esql.xml.XmlFiles;

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
	private DriverSource driverSource;
	//ivate String defaultPortNumber;
	private int type;
	private String fieldOpenChar = "";
	private String fieldCloseChar = "";
	private String dataOpenChar = "";
	private String dataCloseChar = "";
	private DataType[] dataTypes;

	/** Whether the number is one of the server types above (the ids of conf/driver.xml). */
	public static boolean isKnown(int type) {
		return type == MY_SQL || type == MS_SQL_SERVER || type == POSTGRES || type == ORACLE;
	}

	public ServerType(int type) {
		this.type = type;
		DatabaseDriver driverSource = new DatabaseDriver();
		DatabaseDriver[] drivers = driverSource.getDatabaseDrivers();

		for (int i = 0; i < drivers.length; i++) {
			if (type == drivers[i].getId()) {
				description = drivers[i].getDriverName();
				connectionURL = drivers[i].getDriverURL();
				driverName = drivers[i].getDriverClassName();
				this.driverSource = drivers[i].driverSource();
				fieldOpenChar = drivers[i].getFieldOpenChar();
				fieldCloseChar = drivers[i].getFieldCloseChar();
				dataOpenChar = drivers[i].getDataOpenChar();
				dataCloseChar = drivers[i].getDataCloseChar();
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

	public String getConnectionURL(ConnectionProfile profile, String database) {
		String url = connectionURL;

		url = url.replace("@host", profile.getHost());
		url = url.replace("@port", profile.getPort());
		url = url.replace("@username", profile.getUsername());
		url = url.replace("@password", profile.getPassword());
		url = url.replace("@database", getDialect().getConnectionDatabase(profile, database));

		return url;
	}

	public Dialect getDialect() {
		return DialectFactory.forType(type);
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

	/** The driver to download for a server type, null when the driver of that type is bundled; the only place that maps a server type to one. */
	public static DriverArtifact driverArtifact(int type) {
		return switch (type) {
			case MY_SQL -> DriverArtifact.MYSQL;
			case ORACLE -> DriverArtifact.ORACLE;
			default -> null;
		};
	}

	/** Where the JDBC driver comes from: a jar of the user, the application, or a download (from driver.xml). */
	public DriverSource driverSource() {
		return driverSource != null ? driverSource : new DriverSource(driverName, "", driverArtifact(type));
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

	/** The column types of this server from conf/datatypes.xml, empty when the file has none for it; a file that cannot be read is an {@link EsqlException}. */
	public DataType[] getDataTypes() {
		if (dataTypes != null && dataTypes.length != 0) {
			return dataTypes;
		}
		try {
			Document sdata = XmlFiles.read(DataDirectory.file("conf/datatypes.xml").toPath());

			// The file holds the datatypes of every server, pick the section of this one.
			for (Element driver : XmlFiles.children(sdata.getDocumentElement(), "driver")) {
				if (Integer.parseInt(XmlFiles.attribute(driver, "id", "").trim()) != this.getType()) {
					continue;
				}

				java.util.List<Element> types = XmlFiles.children(driver, "type");
				this.dataTypes = new DataType[types.size()];

				for (int i = 0; i < types.size(); i++) {
					Element type = types.get(i);
					this.dataTypes[i] = new DataType(XmlFiles.childText(type, "name"), options(type));
				}
				return this.dataTypes;
			}
			log.warn("No datatypes specified for server type {}", this.getType());
		} catch (EsqlException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new EsqlException("datatypes.xml cannot be read: " + e.getMessage(), e);
		}
		return new DataType[0];
	}

	/** The options a type allows, each a child element set to true (primary, index, unique, binary, notnull, unsigned, autoincrement, zerofill). */
	private java.util.Set<DataType.Option> options(Element type) {
		java.util.Set<DataType.Option> options = java.util.EnumSet.noneOf(DataType.Option.class);
		java.util.Map<String, DataType.Option> elements = java.util.Map.of("primary", DataType.Option.PRIMARY, "index", DataType.Option.INDEX, "unique",
			DataType.Option.UNIQUE, "binary", DataType.Option.BINARY, "notnull", DataType.Option.NOT_NULL, "unsigned", DataType.Option.UNSIGNED,
			"autoincrement", DataType.Option.AUTO_INCREMENT, "zerofill", DataType.Option.ZEROFILL);
		elements.forEach((element, option) -> {
			if (flag(type, element)) {
				options.add(option);
			}
		});
		return options;
	}

	private boolean flag(Element type, String name) {
		return "true".equalsIgnoreCase(XmlFiles.childText(type, name));
	}

	public static ServerType[] getServerTypes() {
		ServerType[] serverTypes = new ServerType[4];

		serverTypes[0] = new ServerType(ServerType.MY_SQL);
		serverTypes[1] = new ServerType(ServerType.MS_SQL_SERVER);
		serverTypes[2] = new ServerType(ServerType.POSTGRES);
		serverTypes[3] = new ServerType(ServerType.ORACLE);

		return serverTypes;
	}
}
