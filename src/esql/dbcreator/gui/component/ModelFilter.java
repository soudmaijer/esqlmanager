package esql.dbcreator.gui.component;

import java.io.File;

public class ModelFilter extends javax.swing.filechooser.FileFilter
{	private String filter;
	private String description;
	
	public ModelFilter ( String filter, String description )
	{	this.filter = filter;
		this.description = description;
	}
	
	public boolean accept( File f )
	{	if( f.toString().toLowerCase().endsWith(filter) || f.isDirectory() )
			return true;
		else
			return false;
	}
	
	public String getDescription() 
	{	return description;  
	}
}
