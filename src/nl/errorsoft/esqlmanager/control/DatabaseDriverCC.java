package nl.errorsoft.esqlmanager.control;

import nl.errorsoft.esqlmanager.domain.*;
import nl.errorsoft.esqlmanager.gui.*;
import nl.errorsoft.esqlmanager.data.*;
import java.util.Vector;

public class DatabaseDriverCC 
{
   private ESQLManagerCC cwcc;
   private DatabaseDriver [] drivers;
   private DriverUI du;
   
   public DatabaseDriverCC( ESQLManagerCC cwcc ) 
   {
		this.cwcc = cwcc;
   }
   
   public void startUI( ESQLManagerUI emui )
   {
   	du = new DriverUI( this, emui );
   	du.loadDrivers( getDatabaseDrivers() );
		du.show();
   }
   
   public DatabaseDriver[] getDatabaseDrivers()
   {
   	drivers = new DatabaseDriver().getDatabaseDrivers();
   	return drivers;
   }
   
   public void saveProperties( int id, String name, String url, String className, String filePath, String fieldOpen, String fieldClose, String dataOpen, String dataClose )
   {
   	try
   	{
   		new DatabaseDriver().saveProperties( drivers, id, name, url, className, filePath, fieldOpen, fieldClose, dataOpen, dataClose );
   		du.showMessage( "Saved changed successfully!");
   	}
   	catch( Exception e )
   	{
   		du.showErrorMessage( "An error occured while saving the changes! "+ e.getMessage() ); 
   	}
   }
}