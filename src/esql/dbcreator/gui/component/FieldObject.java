package esql.dbcreator.gui.component;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import java.io.*;

public class FieldObject
{	private String name;
	private String comment = "";
	private String length = "";
	private FieldObject type;
	
	public boolean primary = false;
	public boolean index = false;
	public boolean unique = false;
	public boolean binary = false;
	public boolean notnull = false;
	public boolean unsigned = false;
	public boolean autoincrement = false;
	public boolean zerofill = false;	
		
	public FieldObject ( String name, boolean primary, boolean index, boolean unique, boolean binary, boolean notnull, boolean unsigned, boolean autoincrement, boolean zerofill )
	{	this.name = name;
		this.primary = primary;
		this.index = index;
		this.unique = unique;
		this.binary = binary;
		this.notnull = notnull;
		this.unsigned = unsigned;
		this.autoincrement = autoincrement;
		this.zerofill = zerofill;	
	}
	
	public String getName()
	{	return this.name;
	}
	
	public static esql.domain.DataType [] getFieldTypes()
	{	
		esql.domain.ServerType t = new esql.domain.ServerType( esql.domain.ServerType.MY_SQL );
		return t.getDataTypes();

	}
	
	public String toString()
	{	return name;
	}		
}