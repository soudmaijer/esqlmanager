package nl.errorsoft.esqlmanager.domain;

public class CreateColumn
{	public String name = "";

	public boolean primary = false;
	public boolean index = false;
	public boolean unique = false;
	public boolean binary = false;
	public boolean notnull = false;
	public boolean unsigned = false;
	public boolean autoincrement = false;
	public boolean zerofill = false;
	
	public DataType type = null;
	
	public String defaultval = "";
	public String length = "";

	public CreateColumn ( String name )
	{	this.name = name;
	}
	
	public String toString()
	{	return name;
	}
}