package esql.domain;

import esql.data.*;
import java.io.File;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

public class ServerType
{
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
	private DataType [] dt;
	String [] mysqlTableTypes;

	public ServerType( int type )
	{
		this.type = type;
		DatabaseDriver d = new DatabaseDriver();
		DatabaseDriver [] da = d.getDatabaseDrivers();
		
		for( int i=0; i<da.length; i++ )
		{
			if( type == da[i].getId() )
			{
				description = da[i].getDriverName();
				connectionURL = da[i].getDriverURL();
				driverName = da[i].getDriverClassName();
				fieldOpenChar = da[i].getFieldOpenChar();
				fieldCloseChar = da[i].getFieldCloseChar();
				dataOpenChar = da[i].getDataOpenChar();
				dataCloseChar = da[i].getDataCloseChar();
			}
		}
/*		else if( type == MY_SQL )
		{
			name = "mysql";
			description = "MySQL Server";
			connectionURL = "jdbc:mysql://@host:@port/";
			driverName = "com.mysql.jdbc.Driver";
			fieldOpenChar = "`";
			fieldCloseChar = "`";
			dataOpenChar = "'";
			dataCloseChar = "'";			
			defaultPortNumber = "3306";
		}
		else if( type == POSTGRES )
		{
			name = "postgresql";
			description = "Postgres SQL";
			connectionURL = "jdbc:postgresql://@host:@port/@database";
			driverName = "org.postgresql.Driver";
			fieldOpenChar = "\"";
			fieldCloseChar = "\"";
			dataOpenChar = "'";
			dataCloseChar = "'";			
			defaultPortNumber = "5432";
		}
		else if( type == ORACLE )
		{
			name = "oracle";
			description = "Oracle";
			connectionURL = "jdbc:oracle:thin:@@host:@port:@database";
			driverName = "oracle.jdbc.driver.OracleDriver";
			fieldOpenChar = "";
			fieldCloseChar = "";
			dataOpenChar = "'";
			dataCloseChar = "'";	
			defaultPortNumber = "1521";		
		}		*/
	}
	
	public String getFieldOpenChar()
	{
		return fieldOpenChar;
	}

	public String getFieldCloseChar()
	{
		return fieldCloseChar;
	}

	public String getDataOpenChar()
	{
		return dataOpenChar;
	}

	public String getDataCloseChar()
	{
		return dataCloseChar;
	}	
	
	public String getConnectionURL( ConnectionProfile cp )
	{
		String url = connectionURL;
		
		url = url.replaceAll("@host", cp.getHost() );		
		url = url.replaceAll("@port", cp.getPort() );
		url = url.replaceAll("@username", cp.getUsername() );
		url = url.replaceAll("@password", cp.getPassword() );
		
		if( url.indexOf( "database" ) > 0 )
			url = url.replaceAll("@database", cp.getDatabases() );

		return url;
	}

	public String getDriverName()
	{
		return driverName;
	}
	
	public int getType()
	{
		return type;
	}
	
	public String getDescription()
	{
		return description;
	}

	public String toString()
	{
		return description;
	}
	
	public DataType [] getDataTypes()
	{	
		if( dt!= null && dt.length != 0)
		{	return dt;
		}
		try
		{	SAXBuilder builder = new SAXBuilder();
			org.jdom.Document sdata = builder.build( new File("conf/datatypes.xml") );
				
			if( sdata.hasRootElement() )
			{
			 	Element fieldtypes = sdata.getRootElement().getChild( "driver" );
			 	java.util.List types = fieldtypes.getChildren("type");
			 	
			 	if( types != null )
			 	{
			 		int typeid = 0;
			 		
			 		try
			 		{
			 			typeid = Integer.parseInt( fieldtypes.getAttributeValue("id") );
			 		}
			 		catch( Exception e )
			 		{
			 			System.out.println( "Warning: no datatype specified for this servertype!" );
			 		}
			 		
				 	if( typeid == this.getType() )
				 	{
					 	this.dt = new DataType [types.size()];
					 	
					 	for( int i=0; i<types.size(); i++ )
					 	{	boolean pri = ( ((Element)types.get(i)).getChild("primary").getText().equalsIgnoreCase("true") )? true : false;
					 		boolean ind = ( ((Element)types.get(i)).getChild("index").getText().equalsIgnoreCase("true") )? true : false;
					 		boolean uni = ( ((Element)types.get(i)).getChild("unique").getText().equalsIgnoreCase("true") )? true : false;
					 		boolean bin = ( ((Element)types.get(i)).getChild("binary").getText().equalsIgnoreCase("true") )? true : false;
					 		boolean not = ( ((Element)types.get(i)).getChild("notnull").getText().equalsIgnoreCase("true") )? true : false;
					 		boolean uns = ( ((Element)types.get(i)).getChild("unsigned").getText().equalsIgnoreCase("true") )? true : false;
					 		boolean aut = ( ((Element)types.get(i)).getChild("autoincrement").getText().equalsIgnoreCase("true") )? true : false;
					 		boolean zer = ( ((Element)types.get(i)).getChild("zerofill").getText().equalsIgnoreCase("true") )? true : false;
					 		this.dt[i] = new DataType( ((Element)types.get(i)).getChild("name").getText(), pri, ind, uni, bin, not, uns, aut, zer);
					 	}	
					 	return this.dt;
					 }
				 }
			}
		}
		catch(Exception e)
		{	e.printStackTrace();
		}	
		return new DataType[0];
	}	

	public static ServerType [] getServerTypes()
	{
		ServerType [] st = new ServerType[4];
		
		st[0] = new ServerType( ServerType.MY_SQL );
		st[1] = new ServerType( ServerType.MS_SQL_SERVER );
		st[2] = new ServerType( ServerType.POSTGRES );
		st[3] = new ServerType( ServerType.ORACLE );
		
		return st;
	} 	

	public String [] getMySQLTableTypes()
	{	
		if( mysqlTableTypes != null && mysqlTableTypes.length != 0)
		{	return mysqlTableTypes;
		}
		try
		{	SAXBuilder builder = new SAXBuilder();
			org.jdom.Document sdata = builder.build( new File("conf/mysql.xml") );
				
			if( sdata.hasRootElement() )
			{
			 	Element tabletypes = sdata.getRootElement().getChild("table");
			 	java.util.List types = tabletypes.getChildren("type");
			 	
			 	this.mysqlTableTypes = new String [types.size()];
			 	for( int i=0; i<types.size(); i++ )
			 	{	this.mysqlTableTypes[i] = ((Element)types.get(i)).getText();
			 	}
			 	
			 	return this.mysqlTableTypes;
			}
		}
		catch(Exception e)
		{	e.printStackTrace();
		}	
		return mysqlTableTypes;
	}	
}