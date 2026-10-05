package nl.errorsoft.esqlmanager.domain;

import java.io.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

public class Settings
{	
	// Autoupdater
	private boolean updater_enabled = false;
	private String  update_server = "";
	
	// Table types & field types
	private String [] tabletypes = new String [0];

	public Settings () 
	{	loadSettings();
	}
	
	public void setUpdaterEnabled( boolean enabled )
	{	updater_enabled = enabled;
	}
	
	public void setUpdateServer( String server )
	{	update_server = server;
	}
	
	public boolean isUpdaterEnabled ( )
	{	return updater_enabled;
	}
	
	public String getUpdateServer ( )
	{	return update_server;
	}	
	
	public void loadSettings() 
	{	try
		{	SAXBuilder builder = new SAXBuilder();
			org.jdom.Document sdata = builder.build( new File("conf/settings.xml") );
				
			if( sdata.hasRootElement() )
			{
			 	if(sdata.getRootElement().getChild("updater_enabled").getText().equalsIgnoreCase("true"))
			 	{	updater_enabled = true;		 	}			 	
			 	update_server = sdata.getRootElement().getChild("update_server").getText();
			}
			
			sdata = null;
			builder = null;
		}
		catch(Exception e)
		{	System.out.println( e.getMessage() );
		}
	}
	
	public boolean saveSettings()
	{	String set = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + '\n';
		set =  set + "<config>" + '\n';
		set =  set + "	<updater_enabled>" + updater_enabled + "</updater_enabled>" + '\n';
		set =  set + "	<update_server>" + update_server + "</update_server>" + '\n';
		set =  set + "</config>" + '\n';
		
		try
		{	PrintWriter out = new PrintWriter(new FileWriter("conf/settings.xml"));
			out.println(set);
			out.close();
		}
		catch(Exception e)
		{	return false;
		}
		return true;
	}
}