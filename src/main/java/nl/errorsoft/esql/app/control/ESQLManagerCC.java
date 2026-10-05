package nl.errorsoft.esql.app.control;

import nl.errorsoft.esql.connection.control.ConnectionProfileCC;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.connection.control.DatabaseDriverCC;
import nl.errorsoft.esql.importexport.control.ExportCC;
import nl.errorsoft.esql.importexport.control.ImportCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.dialect.Dialect;
import nl.errorsoft.esql.designer.DBCreator;
import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.app.ESQLManager;
import nl.errorsoft.esql.connection.ServerType;
import nl.errorsoft.esql.app.Settings;
import nl.errorsoft.esql.connection.ui.ConnectionWindowUI;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.ui.ImageLoader;
import nl.errorsoft.esql.app.ui.SettingsUI;
import nl.errorsoft.esql.app.ui.SplashUI;

/**
 *		Controls all users-systems actions for the ESQLManagerUI.
 */
public class ESQLManagerCC
{
	private static final Logger log = LogManager.getLogger( ESQLManagerCC.class );

	private ESQLManager jm;
	private ESQLManagerUI jmui;
 	
	public ESQLManagerCC()
	{
		// Start domein class.
		jm = new ESQLManager();
		
		// Show ESQLManager Window.
		jmui = new ESQLManagerUI( this );
		OutputPanelAppender.install( jmui );
		log.info( "{} starting on Java {} ({}), {} {}", getTitle(), System.getProperty("java.version"), System.getProperty("java.vendor"), System.getProperty("os.name"), System.getProperty("os.arch") );
		log.info( "Working directory: {}", System.getProperty("user.dir") );
		// Show splash.
		showSplashScreen( 3000 );		
		jmui.updateStatus( "Ready...", false );
	}
	
	public void splashReady()
	{
		// Show connection profile window.
		ConnectionProfileCC cpcc = new ConnectionProfileCC( this );
		cpcc.startUI( jmui, true );		
	}
	
	public void closeUI()
	{
		System.exit(0);
	}
	
	/**
	 *		Use-case: 	show ESQLManager splash screen
	 *		Requires: 	ESQLManager UI use-case
	 */	
	public void showSplashScreen( int time )
	{
	 	// Show a new Splash screen UI. 
		new SplashUI( this, jmui, time );
	}
	 
	public void showConnectionWindow( ConnectionWindowUI cwui )
	{
		jmui.addConnectionWindow( cwui );
	}
	
	public void removeConnectionWindow( ConnectionWindowUI cw )
	{
		jmui.removeConnectionWindow( cw );
	}
	
	public void dispatchConnectionWindowUI( ConnectionProfile cp )
	{
		// Connect and start window.
		ConnectionWindowCC cwcc = new ConnectionWindowCC( this, cp );
	}

	public void dispatchDriverUI()
	{
		DatabaseDriverCC cpcc = new DatabaseDriverCC( this );
		cpcc.startUI( jmui );
	}		
	
	public void dispatchConnectionProfileUI()
	{
		ConnectionProfileCC cpcc = new ConnectionProfileCC( this );
		cpcc.startUI( jmui, false );
	}

	public void dispatchSettingsUI()
	{
		SettingsUI cpcc = new SettingsUI( this, jmui );
	}	
	
	public void dispatchImportUI()
	{
		if( jmui.getConnectionWindowCount() > 0 )
		{
			ImportCC dbcc = new ImportCC( this );
			dbcc.startImportSelectionUI( jmui.getConnectionWindow().getControlClass() );
		}
	}
	
	public void dispatchExportUI()
	{
		if( jmui.getConnectionWindowCount() > 0 )
		{
			ExportCC dbcc = new ExportCC( this );
			dbcc.startExportSelectionUI( jmui.getConnectionWindow().getControlClass() );
		}
	}
	
	public void dispatchDesigner()
	{
		try
		{	if( !jmui.getConnectionWindow().getControlClass().getDatabaseConnection().getConnectionProfile().getServerType().getDialect().supports( Dialect.Feature.DESIGNER ) )   	   	
			{	jmui.showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	   }
	   catch( Exception e )
	   {
	   }
	   		
		if( jmui.getConnectionWindowCount() > 0 )
		{
			DBCreator db = new nl.errorsoft.esql.designer.DBCreator(jmui, jmui.getConnectionWindow());
		}
	}	

	public void updateStatus( String message, boolean red )
	{
		jmui.updateStatus( message, red );
	}		

	public void setStatusInfo( String info )
	{
		jmui.setStatusInfo( info );
	}
	
	public ESQLManagerUI getUI()
	{
		return jmui;
	}
	
	public String getTitle()
	{
		return getAppName() +" - "+ getAppVersion() +" ( build #"+ getAppBuild() +" )";
	}
	
	public String getAppName()
	{
		return jm.getAppName();
	}

	public String getAppVersion()
	{
		return jm.getAppVersion();
	}	

	public int getAppBuild()
	{
		return jm.getAppBuild();
	}	
	
	public boolean isPro()
	{
		return jm.isPro();			
	}
	
	public Settings getSettings()
	{
		return jm.getSettings();
	}	
	
	public ImageLoader getImageLoader()
	{
		return jm.getImageLoader();
	}	 

}