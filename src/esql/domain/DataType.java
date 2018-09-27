// Class for maintaining fieldtypes and their optional flags

package esql.domain;

public class DataType
{
	String name;
	public boolean primary;
	public boolean index;
	public boolean unique;
	public boolean binary;
	public boolean notnull;
	public boolean unsigned;
	public boolean autoincrement;
	public boolean zerofill;

	public DataType(String name, boolean primary, boolean index, boolean unique, boolean binary, boolean notnull, boolean unsigned, boolean autoincrement, boolean zerofill)
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
	{
		return name;
	}
	
	public String toString()
	{	return name;
	}
}