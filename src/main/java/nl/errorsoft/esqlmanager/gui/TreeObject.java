package nl.errorsoft.esqlmanager.gui;

public class TreeObject
{	private boolean root;
	private boolean database;
	private boolean table;
	private boolean field;
	private boolean key;
	private boolean user;
	
	private String name;
	
	public TreeObject(String name, boolean root, boolean database, boolean table, boolean field, boolean key)
	{	this.root = root;
		this.database = database;
		this.table = table;
		this.field = field;
		this.key = key;
		this.name = name;
	}
	
	public String getName()
	{	return name;
	}
	
	public boolean isDatabase()
	{	return database;
	}
	
	public boolean isTable()
	{	return table;
	}
	
	public boolean isRoot()
	{	return root;
	}	
	
	public boolean isField()
	{	return field;
	}
	
	public boolean isKey()
	{	return key;
	}
	
	public boolean isUser()
	{	return user;
	}
	
	public void setUser(boolean user)
	{	this.user = user;
	}
	
	public String toString()
	{	return name;
	}
}