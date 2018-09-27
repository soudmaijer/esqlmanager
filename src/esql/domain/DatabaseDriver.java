package esql.domain;

import java.io.*;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class DatabaseDriver
{
	private int id;
	private String driverName;
	private String driverURL;
	private String driverClassName;
	private String fieldOpenChar;
	private String fieldCloseChar;
	private String dataOpenChar;
	private String dataCloseChar;
	private DatabaseDriver [] drivers;
	private org.jdom.Document driverData;

	/*
	 * @author:			S.Oudmaijer
	 * @description:	Reads the XML data from the driver.xml file in de application root.
	 */
	public DatabaseDriver()
	{
		drivers = new DatabaseDriver[0];

     	try
		{
			SAXBuilder saxbuilder = new SAXBuilder();
		   	driverData = saxbuilder.build(new File("conf/driver.xml"));
		}
		catch(Exception exception)
		{
		   System.out.println("Warning: driver.xml could not be loaded, no driver properties will be available!");
		}
	}

	/*
	 * @author:			S.Oudmaijer
	 * @description:	Retreives all database drivers from the JDOM document.
	 */
	public DatabaseDriver[] getDatabaseDrivers()
	{
    	if( driverData == null )
    		return drivers;

    	List list = null;

    	if( driverData.hasRootElement() )
			list = driverData.getRootElement().getChildren("driver");

     	drivers = new DatabaseDriver[list.size()];

		try
		{
			for(int i = 0; i < list.size(); i++)
			{
			   DatabaseDriver temp = new DatabaseDriver();

			   temp.setId( new Integer(((Element)list.get(i)).getChild("id").getText()).intValue() );
			   temp.setDriverName(((Element)list.get(i)).getChild("driverName").getText());
			   temp.setDriverURL(((Element)list.get(i)).getChild("driverURL").getText());
			   temp.setDriverClassName(((Element)list.get(i)).getChild("driverClassName").getText());
			   //temp.setDriverFilePath(((Element)list.get(i)).getChild("driverFilePath").getText());
			   temp.setFieldOpenChar(((Element)list.get(i)).getChild("fieldOpenChar").getText());
			   temp.setFieldCloseChar(((Element)list.get(i)).getChild("fieldCloseChar").getText());
			   temp.setDataOpenChar(((Element)list.get(i)).getChild("dataOpenChar").getText());
			   temp.setDataCloseChar(((Element)list.get(i)).getChild("dataCloseChar").getText());
			   drivers[i] = temp;
			}
		}
		catch( Exception e )
		{
			System.out.println("Warning: driver data could not be loaded, no driver properties will be available!");
		}

		return drivers;
	}

	/*
	 * @author: 		S.Oudmaijer.
	 * @description:	Saves the properties of one specific database driver.
	 */
	public void saveProperties( DatabaseDriver [] drivers, int id, String name, String url, String className, String filePath, String fieldOpen, String fieldClose, String dataOpen, String dataClose ) throws Exception
	{
		org.jdom.Element root = new org.jdom.Element("drivers");
		driverData.setRootElement( root );

		for( int i=0; i<drivers.length; i++ )
		{
			if( drivers[i].getId() == id )
			{
				drivers[i].setDriverURL( url );
				drivers[i].setDriverClassName( className );
				//drivers[i].setDriverFilePath( filePath );
				drivers[i].setFieldOpenChar( fieldOpen );
				drivers[i].setFieldCloseChar( fieldClose );
				drivers[i].setDataOpenChar( dataOpen );
				drivers[i].setDataCloseChar( dataClose );
			}

			root.addContent( new org.jdom.Element("driver")

				.addContent( new org.jdom.Element("id").setText( new String().valueOf( drivers[i].getId() ) ))
				.addContent( new org.jdom.Element("driverName").setText( drivers[i].getDriverName() ))
				.addContent( new org.jdom.Element("driverURL").setText( drivers[i].getDriverURL() ))
				.addContent( new org.jdom.Element("driverClassName").setText( drivers[i].getDriverClassName() ))
				//.addContent( new org.jdom.Element("driverFilePath").setText( drivers[i].getDriverFilePath() ))
				.addContent( new org.jdom.Element("fieldOpenChar").setText( drivers[i].getFieldOpenChar() ))
				.addContent( new org.jdom.Element("fieldCloseChar").setText( drivers[i].getFieldCloseChar() ))
				.addContent( new org.jdom.Element("dataOpenChar").setText( drivers[i].getDataOpenChar() ))
				.addContent( new org.jdom.Element("dataCloseChar").setText( drivers[i].getDataCloseChar() ))

			);
		}

		save( driverData );
	}

	/*
	 * @author: 		S.Oudmaijer.
	 * @description:	Saves the given JDOM document to the driver.xml file.
	 */
    public void save( Document document ) throws Exception
    {
        XMLOutputter xmloutputter = new XMLOutputter();
        xmloutputter.output(document, new PrintWriter(new FileOutputStream(new File("conf/driver.xml"))));
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

	public void setId( int id ) {
		this.id = id;
	}

	public static void main( String args[] ) throws Exception
	{
		DatabaseDriver dp = new DatabaseDriver();
		DatabaseDriver [] drivers = dp.getDatabaseDrivers();

		for( int i=0; i<drivers.length; i++ )
			System.out.println( drivers[i].getDriverName() );

		dp.saveProperties( drivers, 1, "MySQL", "jdbc:mysql://localhost", "com.mysql.jdbc.Driver", "c:\\", "`", "`", "'", "'" );
	}
	
	public String toString()
	{
		return this.getDriverName();
	}
}
