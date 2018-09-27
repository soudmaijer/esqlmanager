package esql.domain;

public class Grant
{
	private String name;
	private String value;
	private String location;
	private int type;
	
	private String database = "";
	private String table = "";
	private String column = "";
	
	public static final int GLOBAL_ACCESS = 1;
	public static final int DATABASE_ACCESS = 2;
	public static final int TABLE_ACCESS = 4;
	public static final int COLUMN_ACCESS = 8;
	
	public Grant ( String name, String value, String location, int type )
	{	
		this.name = name;
		this.value = value;
		this.location = location;
		this.type = type;
		
		if( type == DATABASE_ACCESS )
		{
			database = location;
		}
		else if ( type == TABLE_ACCESS )
		{
			database = location.substring(0, location.indexOf("."));
			table = location.substring(location.indexOf(".")+1, location.length() );
		}
		else if( type == COLUMN_ACCESS )
		{
			database = location.substring(0, location.indexOf("."));
			location = location.substring(location.indexOf(".") + 1, location.length());
			table = location.substring(0, location.indexOf(".") );
			column = location.substring(location.indexOf(".") + 1, location.length() );
		}
	}
	
	public Grant ( String name, boolean value, String location, int type )
	{	
		this.name = name;
		
		if( value )
			this.value = "Y";
		else
			this.value = "N";
			
		this.location = location;
		this.type = type;
		
		if( type == DATABASE_ACCESS )
		{
			database = location;
		}
		else if ( type == TABLE_ACCESS )
		{
			database = location.substring(0, location.indexOf("."));
			table = location.substring(location.indexOf(".")+1, location.length() );
		}
		else if( type == COLUMN_ACCESS )
		{
			database = location.substring(0, location.indexOf("."));
			location = location.substring(location.indexOf(".") + 1, location.length());
			table = location.substring(0, location.indexOf(".") );
			column = location.substring(location.indexOf(".") + 1, location.length() );
		}		
	}	
	
	public String getName ()
	{
		return name;
	}
	
	public String getValue ()
	{
		return value;
	}
	
	public String getLocation ()
	{
		return location;
	}
	
	public int getType ()
	{
		return type;
	}
	
	public void setValue ( String value )
	{
		this.value = value;
	}
	
	public boolean getTranslatedValue ( )
	{
		if( value.trim().equalsIgnoreCase ( "Y" ) )
			return true;
		else
			return false;
	}
	
	public String getDatabase ()
	{	
		return database;
	}
	
	public String getTable ()
	{	
		return table;
	}	
	
	public String getColumn ()
	{	
		return column;
	}	
}