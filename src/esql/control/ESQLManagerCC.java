package esql.control;

import esql.domain.*;
import esql.gui.*;
import com.psychoticsoftware.guardian.Guardian;

/**
 *		Controls all users-systems actions for the ESQLManagerUI.
 */
public class ESQLManagerCC extends java.io.OutputStream
{
	private ESQLManager jm;
	private ESQLManagerUI jmui;
	private String buffer = "";
 	private Registration reg;
 	
	public ESQLManagerCC()
	{
		// Start domein class.
		jm = new ESQLManager();
		
		// Redirect output stream to eSQLManager output window.
		System.setOut( new java.io.PrintStream( this ) );
		
		// Show ESQLManager Window.
		jmui = new ESQLManagerUI( this );
		println( "------------------- "+ jm.getAppName() +" : output -------------------\n");		
		reg = new Registration( jm.getAppName() + "_"+ jm.getMajorVersion() );
		
		if( jm.isPro() )
		{
			if(reg.isTrialVersion())
			{
				jmui.showMessage("Evaluation notice!", "<html><b>This is an evaluation version of eSQLManager!</b><br>There are "+ reg.getDaysLeft() +" days left in your evaluation!<br>Please register for a fully functional version!<br>For more details about products and pricing see: http://www.errorsoft.nl</html>");
				
				if( reg.isExpired() )
				{
				  this.startRegistrationUI();
				  return;
				}
			}
		}
		else
		{	jmui.showMessage("Evaluation notice!", "<html><b>This is an evaluation version of eSQLManager!</b><br>If you want a fully functional version of eSQLManager then purchase eSQLManager Pro!<br>For more details about products and pricing see: http://www.errorsoft.nl</html>");
		}

		// Show splash.
		showSplashScreen( 3000 );		
		jmui.updateStatus( "(C) Copyright 2002-2003 - Errorsoft", false );
	}
	
	public void startRegistrationUI()
	{
		new RegistrationUI( jmui, this, reg );
	}

	public void splashReady()
	{
		// 1st Check for updates.
		jmui.setEnabled( false );
		checkForUpdates();
		jmui.setEnabled( true );		
		
		// 3th Show connection profile window.
		ConnectionProfileCC cpcc = new ConnectionProfileCC( this );
		cpcc.startUI( jmui, true );		
	}
	
	public void closeUI()
	{
		System.exit(0);
	}
	
	/**
	 *		Use-case: 	check for updates
	 *		Requires: 	ESQLManager UI use-case
	 */	
	public void checkForUpdates()
	{
		// Perform an update check.
		new CheckUpdateUI( this, jmui );			 	
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
		if( checkPro() ) return;
		if( jmui.getConnectionWindowCount() > 0 )
		{
			ImportCC dbcc = new ImportCC( this );
			dbcc.startImportSelectionUI( jmui.getConnectionWindow().getControlClass() );
		}
	}
	
	public void dispatchExportUI()
	{
		if( checkPro() ) return;
		if( jmui.getConnectionWindowCount() > 0 )
		{
			ExportCC dbcc = new ExportCC( this );
			dbcc.startExportSelectionUI( jmui.getConnectionWindow().getControlClass() );
		}
	}
	
	public void dispatchDesigner()
	{
		try
		{	if( jmui.getConnectionWindow().getControlClass().getDatabaseConnection().getConnectionProfile().getServerType().getType() != ServerType.MY_SQL )   	   	
			{	jmui.showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	   }
	   catch( Exception e )
	   {
	   }
	   		
		if( checkPro() ) return;
		if( jmui.getConnectionWindowCount() > 0 )
		{
			esql.dbcreator.gui.DBCreator db = new esql.dbcreator.gui.DBCreator(jmui, jmui.getConnectionWindow());
		}
	}	

	public void println( String line )
	{
		synchronized( this )
		{
			jmui.print( line + "\n" );
		}
	}
	
	public void print( String line )
	{
		println( line );
	}
	
	public void write ( int charCode )
	{	
		if( (char)charCode == '\n' )
		{	println( buffer );
			buffer = "";
		}
		else
		{	buffer = buffer + (char)charCode;
		}
	}
	
	public void updateStatus( String message, boolean red )
	{
		jmui.updateStatus( message, red );
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

	public boolean checkPro()
	{
		if( !isPro() )
		{
			this.jmui.showErrorMessage("This option is avaiable only in the Professional Edition of eSQLManager!");
			return true;
		}
		return false;
	}	
}