package esql.domain;

import java.util.*;

public class User
{	
	private String name;
	private String password;
	private String host;
	
	private Vector grants = new Vector();
	
	public User ( )
	{
	}
	
	public User ( String host, String name , String password )
	{	
		this.name = name;
		this.password = password;
		this.host = host;
	}
	
	public String getName ()
	{	
		return name;
	}
	
	public String getPassword ()
	{
		return password;
	}
	
	public String getHost ()
	{
		return host;
	}
	
	public String toString ()
	{
		return name + "@" + host;
	}
	
	// Adds a grant if it doesn't exist, or changes an existing one
	public void addGrant ( Grant g )
	{	
		Grant tmp = this.getGrant( g.getName(), g.getLocation(), g.getType() );
		
		if( tmp != null )
		{
			tmp.setValue( g.getValue() );	
		}
		else
		{
			grants.add ( g );
		}
	}
	
	// Remove grant for this user
	public void removeGrant( Grant g )
	{
		grants.removeElement( g );
	}
	
	// Get specific grant
	public Grant getGrant ( String name, String location, int type )
	{
		for( int i = 0 ; i < grants.size() ; i++ )
		{
			Grant tmp = (Grant)grants.get( i );
			
			// Grant found, update value
			if( tmp.getName().trim().equalsIgnoreCase(name) && tmp.getLocation().trim().equalsIgnoreCase(location) && tmp.getType() == type )
			{
				return tmp;
			}
		}
		
		return null;
	}
	
	// Get grants for specific location
	public Vector getLocationGrants ( String location, int type )
	{	
		Vector locationgrants = new Vector();
		
		for( int i = 0 ; i < grants.size() ; i++ )
		{
			Grant tmp = (Grant)grants.get( i );
			
			// Grant found, update value
			if( tmp.getLocation().trim().equalsIgnoreCase(location) && tmp.getType() == type )
			{
				locationgrants.addElement( tmp );
			}
		}
		
		return locationgrants;
	}
}