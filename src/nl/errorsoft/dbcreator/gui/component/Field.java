package nl.errorsoft.dbcreator.gui.component;

public class Field
{
	private String name;
	private String length;
	private String dfault;
	private String comment;
	private nl.errorsoft.esqlmanager.domain.DataType type;
	
	public boolean primary = false;
	public boolean index = false;
	public boolean unique = false;
	public boolean binary = false;
	public boolean notnull = false;
	public boolean unsigned = false;
	public boolean autoincrement = false;
	public boolean zerofill = false;	
	
	public Field ( String name, nl.errorsoft.esqlmanager.domain.DataType type, String length, String dfault, String comment )
	{	this.name = name;
		this.type = type;
		this.length = length;
		this.dfault = dfault;
		this.comment = comment;
	}
	
	public String getName()
	{	return name;
	}
	
	public void setName( String name )
	{	this.name = name;
	}
	
	public nl.errorsoft.esqlmanager.domain.DataType getType()
	{	return type;
	}
	
	public void setType( nl.errorsoft.esqlmanager.domain.DataType type )
	{	this.type = type;
	}
	
	public String getLength()
	{	return length;
	}
	
	public void setLength( String length )
	{	this.length = length;
	}
	
	public String getDefault()
	{	return dfault;
	}
	
	public void setDefault( String dfault )
	{	this.dfault = dfault;
	}
	
	public String getComment()
	{	return comment;
	}
	
	public void setComment(String comment)
	{	this.comment = comment;
	}
	
	public String toString()
	{	return name;
	}
}