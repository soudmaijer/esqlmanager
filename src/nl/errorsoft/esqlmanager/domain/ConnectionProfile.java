package nl.errorsoft.esqlmanager.domain;

import java.io.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

public class ConnectionProfile
{	
	private String name = "";
	private String databases = "";
	private String host = "";
	private String port = "";
	private String username = "";
	private String password = "";
	private ServerType st;
	private boolean lastUsed = false;
	private boolean autoConnect = false;
	private org.jdom.Document profileData;
	
	public ConnectionProfile()
	{
		try
		{
			SAXBuilder builder = new SAXBuilder();
			profileData = builder.build( new File("conf/profiles.xml") );
		}
		catch( Exception e )
		{
			System.out.println("Warning: profiles.xml could not be loaded, no profiles will be available!");
		}
	}

	public ConnectionProfile [] getProfiles()
	{	
		java.util.List profiles = null;
		ConnectionProfile [] p = new ConnectionProfile[0];
		
		try
		{	
			if( profileData != null )
			{
				if( profileData.hasRootElement() )
			  		profiles = profileData.getRootElement().getChildren("profile");
				  
				p = new ConnectionProfile[ profiles.size() ];
							
				for( int i=0; i<profiles.size(); i++ )
				{
					ConnectionProfile temp = new ConnectionProfile();
					temp.setName( ((org.jdom.Element)profiles.get(i)).getChild("name").getText() );
					temp.setHost( ((org.jdom.Element)profiles.get(i)).getChild("host").getText() );
					temp.setPort( ((org.jdom.Element)profiles.get(i)).getChild("port").getText() );
					temp.setUsername( ((org.jdom.Element)profiles.get(i)).getChild("username").getText() );
					temp.setPassword( ((org.jdom.Element)profiles.get(i)).getChild("password").getText() );
					temp.setServerType( new ServerType( Integer.parseInt( ((org.jdom.Element)profiles.get(i)).getChild("serverType").getText() ) ) );
					temp.setLastUsed( new Boolean( ((org.jdom.Element)profiles.get(i)).getChild("lastUsed").getText() ).booleanValue() );
					temp.setAutoConnect( new Boolean( ((org.jdom.Element)profiles.get(i)).getChild("autoConnect").getText() ).booleanValue() );
					temp.setDatabases( ((org.jdom.Element)profiles.get(i)).getChild("databases").getText() );
	
					p[i] = temp;
				}
				return p;
			}
		}
		catch(Exception e)
		{	System.out.println(e.getMessage());
		}
		
		return p;
	}
	
	public ServerType getServerType()
	{
		return st;
	}

	public void setServerType( ServerType st )
	{
		this.st = st;
	}
	
	public boolean profileExists( String name ) throws Exception
	{
		if( profileData.hasRootElement() )
		{
			java.util.List l = profileData.getRootElement().getChildren("profile");
			
			for( int i=0; i<l.size(); i++ )
			{
				if( ((org.jdom.Element)l.get(i)).getChild("name").getText().equalsIgnoreCase( name ) )
					return true;
			}
		}
		return false;
	}

	public void setLastUsed( ConnectionProfile cp ) throws Exception
	{
		java.util.List profiles = null;
		
		if( profileData.hasRootElement() )
	  		profiles = profileData.getRootElement().getChildren("profile");
	  	
	  	for( int i=0; i<profiles.size(); i++ )
		{
			org.jdom.Element temp = (org.jdom.Element)profiles.get(i);
			
			if( temp.getChild("lastUsed") != null )
			{
				if( temp.getChild("name").getText().equalsIgnoreCase( cp.getName() ) )
					((org.jdom.Element)profiles.get(i)).getChild("lastUsed").setText( "true" );
				else
					((org.jdom.Element)profiles.get(i)).getChild("lastUsed").setText( "false" );
			}
		}
		save( profileData );				
	}

	public void addProfile( ConnectionProfile cp ) throws Exception
	{
		org.jdom.Element newElement = new org.jdom.Element("profile");
		
		if( profileData.hasRootElement() )
		{
			newElement.addContent( new org.jdom.Element("name").setText( cp.getName() ) );
			newElement.addContent( new org.jdom.Element("host").setText( cp.getHost() ) );
			newElement.addContent( new org.jdom.Element("port").setText( cp.getPort() ) );
			newElement.addContent( new org.jdom.Element("username").setText( cp.getUsername() ) );
			newElement.addContent( new org.jdom.Element("password").setText( cp.getPassword() ) );
			newElement.addContent( new org.jdom.Element("serverType").setText( Integer.toString( cp.getServerType().getType() ) ) );
			newElement.addContent( new org.jdom.Element("databases").setText( cp.getDatabases() ) );
			newElement.addContent( new org.jdom.Element("autoConnect").setText( new Boolean( cp.isAutoConnect() ).toString() ) );
			newElement.addContent( new org.jdom.Element("lastUsed").setText( "false" ) );
			profileData.getRootElement().addContent( newElement );
		}
		save( profileData );		
	}


	public void editProfile( ConnectionProfile profile ) throws Exception
	{
		if( profileData.hasRootElement() )
		{
			java.util.List l = profileData.getRootElement().getChildren("profile");
			
			for( int i=0; i<l.size(); i++ )
			{
				if( ((org.jdom.Element)l.get(i)).getChild("name").getText().equalsIgnoreCase( profile.getName() ) )
				{
					((org.jdom.Element)l.get(i)).getChild("host").setText( profile.getHost() );
					((org.jdom.Element)l.get(i)).getChild("port").setText( profile.getPort() );
					((org.jdom.Element)l.get(i)).getChild("username").setText( profile.getUsername() );
					((org.jdom.Element)l.get(i)).getChild("password").setText( profile.getPassword() );
					((org.jdom.Element)l.get(i)).getChild("serverType").setText( Integer.toString( profile.getServerType().getType() ) );
					((org.jdom.Element)l.get(i)).getChild("databases").setText( profile.getDatabases() );
					((org.jdom.Element)l.get(i)).getChild("autoConnect").setText( new Boolean( profile.isAutoConnect() ).toString() );
				}
				else if( profile.isAutoConnect() )
					((org.jdom.Element)l.get(i)).getChild("autoConnect").setText("false");
				
			}
			save( profileData );		
		}
	}

	public void deleteProfile( ConnectionProfile profile ) throws Exception
	{
		if( profileData.hasRootElement() )
		{
			java.util.List l = profileData.getRootElement().getChildren("profile");
			
			for( int i=0; i<l.size(); i++ )
			{
				if( ((org.jdom.Element)l.get(i)).getChild("name").getText().equalsIgnoreCase( profile.getName() ) )
				{
					profileData.getRootElement().removeContent( (org.jdom.Element)l.get(i) );
					save( profileData );		
					break;
				}
			}
		}
	}

	public void save( org.jdom.Document doc ) throws Exception
	{
		org.jdom.output.XMLOutputter xmlout = new org.jdom.output.XMLOutputter();
		xmlout.output( doc, new PrintWriter( new FileOutputStream( new File("conf/profiles.xml") ) ) );
	}

	public String getName()
	{	
		if( name == null )
			return "";
		return name;
	}
	
	public String getDatabases()
	{
		return this.databases;
	}
	
	public void setDatabases( String databases )
	{
		this.databases = databases;
	}

	public void setName( String name )
	{	
		this.name = name;
	}
	
	public void setHost( String host )
	{
		this.host = host;
	}
	
	public String getHost()
	{	return host;
	}
	
	public void setPort( String port )
	{
		this.port = port;
	}
	
	public String getPort()
	{	
		return port;
	}
	
	public void setUsername( String username )
	{
		this.username = username;
	}
	
	public String getUsername()
	{	
		if( name == null )
			return "";
		return username;
	}

	public boolean isLastUsed()
	{	
		return lastUsed;
	}
	
	public void setLastUsed( boolean lastUsed )
	{
		this.lastUsed = lastUsed;
	}

	public boolean isAutoConnect()
	{	
		return autoConnect;
	}
	
	public void setAutoConnect( boolean autoConnect )
	{
		this.autoConnect = autoConnect;
	}
	
	public void setPassword( String password )
	{
		this.password = password;
	}
	
	public String getPassword()
	{	
		if( password == null )
			return "";

		return password;
	}
	
	public String toString()
	{
		return name;
	}
}