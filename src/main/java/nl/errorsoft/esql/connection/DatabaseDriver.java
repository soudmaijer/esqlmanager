package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.app.DataDirectory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.xml.XmlFiles;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DatabaseDriver {
	private static final Logger log = LogManager.getLogger(DatabaseDriver.class);

	private int id;
	private String driverName;
	private String driverURL;
	private String driverClassName;
	private String fieldOpenChar;
	private String fieldCloseChar;
	private String dataOpenChar;
	private String dataCloseChar;
	private DatabaseDriver[] drivers;
	private Document driverData;

	/*
	 * @author:			S.Oudmaijer
	 * @description:	Reads the XML data from the driver.xml file in de application root.
	 */
	public DatabaseDriver() {
		this(true);
	}

	/** One entry of driver.xml ({@code readFile} false), or the reader of the whole file. */
	private DatabaseDriver(boolean readFile) {
		drivers = new DatabaseDriver[0];
		if (!readFile) {
			return;
		}

		try {
			driverData = XmlFiles.read(DataDirectory.file("conf/driver.xml").toPath());
		} catch (EsqlException e) {
			log.warn("driver.xml could not be loaded, no driver properties will be available: {}", e.getMessage());
		}
	}

	/*
	 * @author:			S.Oudmaijer
	 * @description:	Retrieves all database drivers from the driver.xml document.
	 */
	public DatabaseDriver[] getDatabaseDrivers() {
		if (driverData == null) {
			return drivers;
		}

		List<DatabaseDriver> read = new ArrayList<>();

		for (Element element : XmlFiles.children(driverData.getDocumentElement(), "driver")) {
			try {
				DatabaseDriver driver = new DatabaseDriver(false);
				driver.setId(Integer.parseInt(XmlFiles.childText(element, "id", "").trim()));
				driver.setDriverName(required(element, "driverName"));
				driver.setDriverURL(required(element, "driverURL"));
				driver.setDriverClassName(required(element, "driverClassName"));
				driver.setFieldOpenChar(required(element, "fieldOpenChar"));
				driver.setFieldCloseChar(required(element, "fieldCloseChar"));
				driver.setDataOpenChar(required(element, "dataOpenChar"));
				driver.setDataCloseChar(required(element, "dataCloseChar"));
				read.add(driver);
			} catch (RuntimeException e) {
				// One bad entry leaves out that driver only, the others can still be used.
				log.warn("Driver '{}' in driver.xml skipped: {}", XmlFiles.childText(element, "driverName"), e.getMessage());
			}
		}

		drivers = read.toArray(new DatabaseDriver[0]);
		return drivers;
	}

	private static String required(Element element, String child) {
		String text = XmlFiles.childText(element, child);
		if (text == null) {
			throw new IllegalArgumentException("<" + child + "> is missing");
		}
		return text;
	}

	/*
	 * @author: 		S.Oudmaijer.
	 * @description:	Saves the properties of one specific database driver.
	 */
	public void saveProperties(DatabaseDriver[] drivers, int id, DriverProperties properties) throws Exception {
		Document document = XmlFiles.newDocument("drivers");
		Element root = document.getDocumentElement();

		for (int i = 0; i < drivers.length; i++) {
			if (drivers[i].getId() == id) {
				drivers[i].setDriverURL(properties.url());
				drivers[i].setDriverClassName(properties.className());
				drivers[i].setFieldOpenChar(properties.identifierOpen());
				drivers[i].setFieldCloseChar(properties.identifierClose());
				drivers[i].setDataOpenChar(properties.stringOpen());
				drivers[i].setDataCloseChar(properties.stringClose());
			}

			Element driver = XmlFiles.addChild(root, "driver");
			XmlFiles.addChild(driver, "id", String.valueOf(drivers[i].getId()));
			XmlFiles.addChild(driver, "driverName", drivers[i].getDriverName());
			XmlFiles.addChild(driver, "driverURL", drivers[i].getDriverURL());
			XmlFiles.addChild(driver, "driverClassName", drivers[i].getDriverClassName());
			XmlFiles.addChild(driver, "fieldOpenChar", drivers[i].getFieldOpenChar());
			XmlFiles.addChild(driver, "fieldCloseChar", drivers[i].getFieldCloseChar());
			XmlFiles.addChild(driver, "dataOpenChar", drivers[i].getDataOpenChar());
			XmlFiles.addChild(driver, "dataCloseChar", drivers[i].getDataCloseChar());
		}

		XmlFiles.write(DataDirectory.file("conf/driver.xml").toPath(), document);
		driverData = document;
	}

	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}

	public void setDriverURL(String driverURL) {
		this.driverURL = driverURL;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	//	public void setDriverFilePath(String driverFilePath) {
	//		this.driverFilePath = driverFilePath;
	//	}

	public void setFieldOpenChar(String fieldOpenChar) {
		this.fieldOpenChar = fieldOpenChar;
	}

	public void setFieldCloseChar(String fieldCloseChar) {
		this.fieldCloseChar = fieldCloseChar;
	}

	public void setDataOpenChar(String dataOpenChar) {
		this.dataOpenChar = dataOpenChar;
	}

	public void setDataCloseChar(String dataCloseChar) {
		this.dataCloseChar = dataCloseChar;
	}

	public String getDriverName() {
		return (this.driverName);
	}

	public String getDriverURL() {
		return (this.driverURL);
	}

	public String getDriverClassName() {
		return (this.driverClassName);
	}

	public String getDriverFilePath() {
		return "";
	}

	public String getFieldOpenChar() {
		return (this.fieldOpenChar);
	}

	public String getFieldCloseChar() {
		return (this.fieldCloseChar);
	}

	public String getDataOpenChar() {
		return (this.dataOpenChar);
	}

	public String getDataCloseChar() {
		return (this.dataCloseChar);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String toString() {
		return this.getDriverName();
	}
}
