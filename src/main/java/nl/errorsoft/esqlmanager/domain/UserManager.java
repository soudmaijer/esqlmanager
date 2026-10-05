package nl.errorsoft.esqlmanager.domain;

import nl.errorsoft.esqlmanager.control.*;
import java.sql.*;
import java.util.Vector;

public class UserManager
{	
	private ConnectionWindowCC cwcc;

	public UserManager ( ConnectionWindowCC cwcc )
	{
		this.cwcc = cwcc;	
	}
	
	public Vector getUserAccounts () 
	{	Vector users = new Vector();
	
		try
		{			
			ResultSet rs = cwcc.getDatabaseConnection().executeQuery("SELECT * FROM `mysql`.`user`");
		
			while (rs.next())
			{				
				User u = new User(rs.getString(1), rs.getString(2), rs.getString(3));
				getUserGrants( u );
				users.add( u );
			}	
		
			rs.close();
		}
		catch( Exception e )
		{
		}
		
		return users;
	}
	
	public void getUserGrants ( User u )
	{
		try
		{
			ResultSet rs = cwcc.getDatabaseConnection().executeQuery("SELECT * FROM `mysql`.`user` WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'");
		
			if( rs.first() )
			{
				u.addGrant( new Grant("SELECT", 		rs.getString(4),	 	"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("INSERT", 		rs.getString(5), 		"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("UPDATE", 		rs.getString(6), 		"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("DELETE", 		rs.getString(7), 		"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("CREATE", 		rs.getString(8), 		"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("DROP", 		rs.getString(9), 		"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("RELOAD", 		rs.getString(10), 	"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("SHUTDOWN", 	rs.getString(11), 	"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("PROCESS", 	rs.getString(12), 	"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("FILE", 		rs.getString(13),		"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("GRANT", 		rs.getString(14), 	"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("REFERENCES", rs.getString(15), 	"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("INDEX", 		rs.getString(16), 	"GLOBAL" , Grant.GLOBAL_ACCESS ) );
				u.addGrant( new Grant("ALTER", 		rs.getString(17),		"GLOBAL" , Grant.GLOBAL_ACCESS ) );
			}
			
			rs.close();
			
			rs = cwcc.getDatabaseConnection().executeQuery("SELECT * FROM `mysql`.`db` WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'");
			
			while( rs.next() )
			{
				u.addGrant( new Grant("SELECT", 		rs.getString(4),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("INSERT", 		rs.getString(5),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("UPDATE", 		rs.getString(6),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("DELETE", 		rs.getString(7),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("CREATE", 		rs.getString(8),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("DROP", 		rs.getString(9),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("GRANT", 		rs.getString(10),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("REFERENCES",	rs.getString(11),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("INDEX", 		rs.getString(12),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
				u.addGrant( new Grant("ALTER", 		rs.getString(13),	 	rs.getString(2) , Grant.DATABASE_ACCESS ) );
			}
			
			rs.close();
			
			rs = cwcc.getDatabaseConnection().executeQuery("SELECT * FROM `mysql`.`tables_priv` WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'");
			
			while( rs.next() )
			{	
				String grants = rs.getString (7).toUpperCase();
				
				if( grants.indexOf("SELECT") != -1 )
					u.addGrant( new Grant("SELECT", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("SELECT", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				
				if( grants.indexOf("INSERT") != -1 )
					u.addGrant( new Grant("INSERT", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("INSERT", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );					
					
				if( grants.indexOf("UPDATE") != -1 )
					u.addGrant( new Grant("UPDATE", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("UPDATE", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );										
					
				if( grants.indexOf("DELETE") != -1 )
					u.addGrant( new Grant("DELETE", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("DELETE", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );	
					
				if( grants.indexOf("CREATE") != -1 )
					u.addGrant( new Grant("CREATE", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("CREATE", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );																				
					
				if( grants.indexOf("DROP") != -1 )
					u.addGrant( new Grant("DROP", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("DROP", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );	
					
				if( grants.indexOf("GRANT") != -1 )
					u.addGrant( new Grant("GRANT", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("GRANT", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );	

				if( grants.indexOf("REFERENCES") != -1 )
					u.addGrant( new Grant("REFERENCES", "Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("REFERENCES", "N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );														
					
				if( grants.indexOf("INDEX") != -1 )
					u.addGrant( new Grant("INDEX", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("INDEX", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );						
			
				if( grants.indexOf("ALTER") != -1 )
					u.addGrant( new Grant("ALTER", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );
				else
					u.addGrant( new Grant("ALTER", 		"N",	 	rs.getString(2) + "." + rs.getString(4) , Grant.TABLE_ACCESS ) );									
			}
			
			rs.close();
			
			rs = cwcc.getDatabaseConnection().executeQuery("SELECT * FROM `mysql`.`columns_priv` WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'");
			
			while( rs.next() )
			{	
				String grants = rs.getString (7).toUpperCase();
				
				if( grants.indexOf("SELECT") != -1 )
					u.addGrant( new Grant("SELECT", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) + "." + rs.getString(5) , Grant.COLUMN_ACCESS ) );
				else
					u.addGrant( new Grant("SELECT", 		"N",	 	rs.getString(2) + "." + rs.getString(4) + "." + rs.getString(5) , Grant.COLUMN_ACCESS ) );
				
				if( grants.indexOf("INSERT") != -1 )
					u.addGrant( new Grant("INSERT", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) + "." + rs.getString(5) , Grant.COLUMN_ACCESS ) );
				else
					u.addGrant( new Grant("INSERT", 		"N",	 	rs.getString(2) + "." + rs.getString(4) + "." + rs.getString(5) , Grant.COLUMN_ACCESS ) );					
					
				if( grants.indexOf("UPDATE") != -1 )
					u.addGrant( new Grant("UPDATE", 		"Y",	 	rs.getString(2) + "." + rs.getString(4) + "." + rs.getString(5) , Grant.COLUMN_ACCESS ) );
				else
					u.addGrant( new Grant("UPDATE", 		"N",	 	rs.getString(2) + "." + rs.getString(4) + "." + rs.getString(5) , Grant.COLUMN_ACCESS ) );										
					
				if( grants.indexOf("REFERENCES") != -1 )
					u.addGrant( new Grant("REFERENCES", "Y",	 	rs.getString(2) + "." + rs.getString(4) + "." + rs.getString(5) , Grant.COLUMN_ACCESS ) );
				else
					u.addGrant( new Grant("REFERENCES", "N",	 	rs.getString(2) + "." + rs.getString(4) + "." + rs.getString(5) , Grant.COLUMN_ACCESS ) );				
			}			
			
			rs.close();
		}
		catch( Exception e )
		{	
			
		}
	}
	
	public String addUser( User u, boolean grant )
	{
		try
		{
			if( !grant )
			{
				if( u.getPassword().trim().length() == 0 )
				{
					cwcc.getDatabaseConnection().executeUpdate("GRANT USAGE ON *.* TO '" + u.getName() + "'@'" + u.getHost() + "'");
				}	
				else
				{
					cwcc.getDatabaseConnection().executeUpdate("GRANT USAGE ON *.* TO '" + u.getName() + "'@'" + u.getHost() + "' IDENTIFIED BY '" + u.getPassword() + "'");
				}
			}
			else
			{
				if( u.getPassword().trim().length() == 0 )
				{
					cwcc.getDatabaseConnection().executeUpdate("GRANT ALL PRIVILEGES ON *.* TO '" + u.getName() + "'@'" + u.getHost() + "' WITH GRANT OPTION");
				}	
				else
				{
					cwcc.getDatabaseConnection().executeUpdate("GRANT ALL PRIVILEGES ON *.* TO '" + u.getName() + "'@'" + u.getHost() + "' IDENTIFIED BY '" + u.getPassword() + "' WITH GRANT OPTION");
				}				
			}
			cwcc.getDatabaseConnection().executeUpdate("FLUSH PRIVILEGES");
		}
		catch( Exception e )
		{	
			//System.out.println(e.getMessage());
			return e.getMessage();
		}
		
		return "OK";
	}
	
	public String editUserAccount( User nw, User old)
	{
		try
		{
			cwcc.getDatabaseConnection().executeUpdate("UPDATE mysql.user SET user='" + nw.getName() + "', host='" + nw.getHost() + "', password=password('" + nw.getPassword() + "') WHERE user='" + old.getName() + "' AND host='" + old.getHost() + "'");
			cwcc.getDatabaseConnection().executeUpdate("UPDATE mysql.db SET user='" + nw.getName() + "', host='" + nw.getHost() + "' WHERE user='" + old.getName() + "' AND host='" + old.getHost() + "'");
			cwcc.getDatabaseConnection().executeUpdate("UPDATE mysql.tables_priv SET user='" + nw.getName() + "', host='" + nw.getHost() + "' WHERE user='" + old.getName() + "' AND host='" + old.getHost() + "'");
			cwcc.getDatabaseConnection().executeUpdate("UPDATE mysql.columns_priv SET user='" + nw.getName() + "', host='" + nw.getHost() + "' WHERE user='" + old.getName() + "' AND host='" + old.getHost() + "'");
			cwcc.getDatabaseConnection().executeUpdate("FLUSH PRIVILEGES");
		}
		catch( Exception e )
		{	
			//System.out.println(e.getMessage());
			return e.getMessage();
		}
		
		return "OK";
	}
	
	public String deleteUserAccount ( User u )	
	{
		try
		{
			cwcc.getDatabaseConnection().executeUpdate("DELETE FROM mysql.user WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'");
			cwcc.getDatabaseConnection().executeUpdate("DELETE FROM mysql.db WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'");
			cwcc.getDatabaseConnection().executeUpdate("DELETE FROM mysql.tables_priv WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'");
			cwcc.getDatabaseConnection().executeUpdate("DELETE FROM mysql.columns_priv WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'");	
			cwcc.getDatabaseConnection().executeUpdate("FLUSH PRIVILEGES");
		}
		catch( Exception e )
		{	
			//System.out.println(e.getMessage());
			return e.getMessage();
		}
		
		return "OK";	
	}
	
	public boolean updateGrants( User u, Vector grants )
	{
		if( grants.size() != 0 )
		{	Grant g = (Grant)grants.get(0);	
			if( u.getLocationGrants( g.getLocation(), g.getType()).size() != 0)
			{
				String query = "";
				if( g.getType() == Grant.GLOBAL_ACCESS )
				{
					query = "UPDATE mysql.user SET ";
				}
				else if( g.getType() == Grant.DATABASE_ACCESS )
				{
					query = "UPDATE mysql.db SET ";
				}
				else if( g.getType() == Grant.TABLE_ACCESS )
				{
					query = "UPDATE mysql.tables_priv SET table_priv='";
				}
				else if( g.getType() == Grant.COLUMN_ACCESS )
				{
					query = "UPDATE mysql.columns_priv SET column_priv='";
				}
				
				if(g.getType() == Grant.DATABASE_ACCESS || g.getType() == Grant.GLOBAL_ACCESS )
				{
					for(int i = 0 ; i < grants.size(); i ++ )
					{
						Grant tmp = (Grant)(grants.get(i));
						
						if( tmp.getTranslatedValue() )
							query = query + tmp.getName() + "_priv='Y', " ;
						else
							query = query + tmp.getName() + "_priv='N', " ;
					}
					
					if( g.getType() == Grant.GLOBAL_ACCESS )
					{
						query = query.substring(0, query.length() - 2) + " WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "'";
					}
					else
					{
						query = query.substring(0, query.length() - 2) + " WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "' AND db='" + g.getDatabase() + "'";
					}
				}
				else if ( g.getType() == Grant.TABLE_ACCESS || g.getType() == Grant.COLUMN_ACCESS)
				{
					String values= "";
					
					for(int i = 0 ; i < grants.size(); i ++ )
					{
						Grant tmp = (Grant)(grants.get(i));
						
						if( tmp.getTranslatedValue() )
							values = values + tmp.getName() + ", ";
							
					}
					
					if( g.getType() == Grant.TABLE_ACCESS )
					{	try
						{	
							values = values.substring(0,values.length()-2);
						}
						catch(Exception e)
						{}
						
						query = query.substring(0, query.length()) + values + "' WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "' AND db='" + g.getDatabase() + "' AND table_name='" + g.getTable() + "'";
					}
					else if( g.getType() == Grant.COLUMN_ACCESS )
					{	try
						{	
							values = values.substring(0,values.length()-2);
						}
						catch(Exception e)
						{}
						
						query = query.substring(0, query.length()) + values + "' WHERE user='" + u.getName() + "' AND host='" + u.getHost() + "' AND db='" + g.getDatabase() + "' AND table_name='" + g.getTable() + "' AND column_name='" + g.getColumn() + "'";
					}
				}				
					
				try
				{
					int rows = cwcc.getDatabaseConnection().executeUpdate(query);
				
					cwcc.getDatabaseConnection().executeUpdate("FLUSH PRIVILEGES");
					
					if( rows != 0 )
					{	
						for(int i = 0 ; i < grants.size(); i ++ )
						{
							Grant tmp = (Grant)(grants.get(i));
							
							u.addGrant( tmp );
						}
						return true;
					}
				}
				catch( Exception e )
				{
					System.out.println(e.getMessage());
				}
			}
			else
			{	String query = "";
			
				if( g.getType() == Grant.DATABASE_ACCESS )
				{
					query = "INSERT INTO mysql.db SET db='" + g.getDatabase() + "', user='" + u.getName() + "', host='" + u.getHost().trim() + "', ";
				}
				else if( g.getType() == Grant.TABLE_ACCESS )
				{
					query = "INSERT INTO mysql.tables_priv SET db='" + g.getDatabase() + "', table_name='" + g.getTable() + "', user='" + u.getName() + "', host='" + u.getHost().trim() + "', table_priv=' ";
				}
				else if( g.getType() == Grant.COLUMN_ACCESS )
				{
					query = "INSERT INTO mysql.columns_priv SET db='" + g.getDatabase() + "', table_name='" + g.getTable() + "', column_name='" + g.getColumn() + "', user='" + u.getName() + "', host='" + u.getHost().trim() + "', column_priv='";
				}				
				
				if(g.getType() == Grant.DATABASE_ACCESS || g.getType() == Grant.GLOBAL_ACCESS )
				{
					for(int i = 0 ; i < grants.size(); i ++ )
					{
						Grant tmp = (Grant)(grants.get(i));
						
						if( tmp.getTranslatedValue() )
							query = query + tmp.getName() + "_priv='Y', " ;
						else
							query = query + tmp.getName() + "_priv='N', " ;
					}
					
					query = query.substring(0, query.length() - 2) ;
				}
				else if ( g.getType() == Grant.TABLE_ACCESS || g.getType() == Grant.COLUMN_ACCESS)
				{
					String values= "";
					
					for(int i = 0 ; i < grants.size(); i ++ )
					{
						Grant tmp = (Grant)(grants.get(i));
						
						if( tmp.getTranslatedValue() )
							values = values + tmp.getName() + ", ";		
					}
					
					try
					{
							values = values.substring(0,values.length()-2);
					}
					catch(Exception e)
					{}
					
					query = query.substring(0, query.length()) + values.substring(0,values.length()) + " '  ";
				}
					
				try
				{
					int rows = cwcc.getDatabaseConnection().executeUpdate(query);
				
					cwcc.getDatabaseConnection().executeUpdate("FLUSH PRIVILEGES");
					
					if( rows != 0 )
					{	
						for(int i = 0 ; i < grants.size(); i ++ )
						{
							Grant tmp = (Grant)(grants.get(i));
							
							u.addGrant( tmp );
						}
						
						return true;
					}
				}
				catch( Exception e )
				{
					System.out.println(e.getMessage());
				}
			}	
		}
		return false;
	}
}