//Source file: d:\\roseoutput\\esql\\esql\\table\\TableColumn.java

package nl.errorsoft.esqlmanager.domain;

import java.util.Vector;

public class TableColumn 
{
	private Table table;
   private String name;
   private String typeName;
   private String className;
   private String nativeTypeName;
   private String defaultValue;
   private boolean autoIncrement;
   private boolean signed;
   private boolean nullable;
   private boolean binary;
   private boolean writable;
   private boolean primary;
   private boolean hasIndex;
   private boolean hasUniqueIndex;
   private int size;
   private int type;
   private int indexPosition;
 
   /**
    * @roseuid 3E05A70B02A3
    */
   public TableColumn( Table table ) 
   { 	this.table = table;
   }
   
   public Table getTable()
   { 	return table;
   }
   
	public void setName( String name )
	{	this.name = name;
	}
	
   public String getName()
   { 	return this.name;
   }

	public String toString()
   { 	return this.name;
   }
   
   public void setIndexed( boolean indexed )
   {
   	this.hasIndex = indexed;
   }

	public void setHasUniqueIndex( boolean uniqueIndexed )
   {
   	this.hasUniqueIndex = uniqueIndexed;
   }
	
	public boolean hasIndex()
	{
		return hasIndex;
	}

	public boolean hasUniqueIndex()
	{
		return hasUniqueIndex;
	}
	
	public void setTable(Table table) {
		this.table = table; 
	}

	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement; 
	}

	public void setSigned(boolean signed) {
		this.signed = signed; 
	}

	public void setWritable(boolean writable) {
		this.writable = writable; 
	}
	
	public void setNullable(boolean nullable) {
		this.nullable = nullable; 
	}

	public boolean isPrimary() 
	{
		return primary;
	}
	public void setPrimary( boolean primary ) 
	{
		this.primary = primary;
	}

	public boolean isAutoIncrement() {
		return (this.autoIncrement); 
	}
	public boolean isBinary() {
		return (this.binary); 
	}
	public boolean isSigned() {
		return (this.signed); 
	}

	public boolean isNullable() {
		return (this.nullable); 
	}   
	
	public boolean isWritable() {
		return (this.writable); 
	}   
	
	public String getTypeName()
	{	return this.typeName;
	}
	public void setTypeName( String typeName )
	{	this.typeName = typeName;
	}
	
	public String getDefault()
	{	return this.defaultValue;
	}
	public void setDefault( String defaultValue )
	{	this.defaultValue = defaultValue;
	}
	
	public String getNativeTypeName()
	{	return this.nativeTypeName;
	}
	public void setNativeTypeName( String nativeTypeName )
	{	this.nativeTypeName = nativeTypeName;
	}
	
	public int getSize()
	{	return this.size;
	}
	public void setSize( int size )
	{	this.size = size;
	}	
	public String getClassName()
	{	return this.className;
	}
	public void setClassName( String className )
	{	this.className = className;
	}

	public int getType()
	{	return this.type;
	}
	
	public void setType( int type )
	{	
		if( type == java.sql.Types.BINARY || type == java.sql.Types.VARBINARY || type == java.sql.Types.LONGVARBINARY || type == java.sql.Types.BLOB || type == java.sql.Types.CLOB )
			this.binary = true;
		this.type = type;
	}		
	
	public void setIndexPosition( int indexPosition )
	{
		this.indexPosition = indexPosition;
	}
	
	public int getIndexPosition()
	{
		return indexPosition;
	}
}
