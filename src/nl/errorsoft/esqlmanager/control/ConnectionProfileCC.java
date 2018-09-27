package nl.errorsoft.esqlmanager.control;

import nl.errorsoft.esqlmanager.domain.*;
import nl.errorsoft.esqlmanager.gui.*;

public class ConnectionProfileCC
{
	private ESQLManagerCC jmcc;
	private ConnectionProfileUI cpui;
	private ConnectionProfile cp;

	public ConnectionProfileCC( ESQLManagerCC jmcc )
	{
		this.jmcc = jmcc;
		
		try
		{
			cp = new ConnectionProfile();
		}
		catch( Exception e )
		{
			jmcc.println("An error occured while loading profiles.xml file!");
		}
	}
	
	public void startUI( ESQLManagerUI jmui, boolean autoConnect )
	{
		// Create Frame.
		jmui.updateStatus("Starting profile manager...", true );
		ConnectionProfile [] cpa = cp.getProfiles();
		boolean conLastUsed = false;
		
		if( autoConnect )
		{
			for( int i=0; i<cpa.length; i++ )
			{
				if( cpa[i].isAutoConnect() )
				{
					connect( cpa[i] );
					conLastUsed = true ;
				}
			}
		}
		if( !conLastUsed )
		{
			cpui = new ConnectionProfileUI( jmui,this );
			cpui.loadProfiles( cp.getProfiles() );
			jmcc.updateStatus("Ready...", false );
			cpui.show();
		}
	}
	
	public void connect( ConnectionProfile selcp )
	{
		try
		{
			cp.setLastUsed( selcp );
			jmcc.dispatchConnectionWindowUI( selcp );
			cpui.dispose();
		}
		catch( Exception e )
		{
			cpui.showErrorMessage("Error while connecting to selected connectionprofile!");
		}
	}
	
	public void addProfile( String name, ServerType type, String host, String port, String username, String password, String databases, boolean autoConnect )
	{	
		try
		{
			jmcc.updateStatus("Adding profile...", true );
			ConnectionProfile cpt = new ConnectionProfile();
			
			if( !cp.profileExists( name ) )
			{
				cpt.setName( name );
				cpt.setHost( host );
				cpt.setPort( port );
				cpt.setUsername( username );
				cpt.setPassword( password );
				cpt.setDatabases( databases );
				cpt.setAutoConnect( autoConnect );
				cpt.setServerType( type );
				cp.addProfile( cpt );
				cpui.loadProfiles( cp.getProfiles() );
				cpui.setSelectedProfile( cpt );
				jmcc.updateStatus("Ready...", false );
				cpui.showMessage("Profile added succesfully!");
			}
		}
		catch( Exception e )
		{
			cpui.showErrorMessage("Error while adding profile!");
		}
	}

	public void editProfile( ConnectionProfile cpt, String name, ServerType type, String host, String port, String username, String password, String databases, boolean autoConnect )
	{	
		try
		{
			jmcc.updateStatus("Saving profile...", true );
			cpt.setName( name );
			cpt.setHost( host );
			cpt.setPort( port );
			cpt.setUsername( username );
			cpt.setPassword( password );
			cpt.setDatabases( databases );
			cpt.setServerType( type );
			cpt.setAutoConnect( autoConnect );
			cp.editProfile( cpt );
			jmcc.updateStatus("Ready...", false );
			cpui.showMessage("Saved changes!");
		}
		catch( Exception e )
		{
			cpui.showErrorMessage("Error while saving profile!");
		}
	}
	
	public void deleteProfile( ConnectionProfile cp )
	{
		try
		{
			jmcc.updateStatus("Deleting profile...", true );
			this.cp.deleteProfile( cp );
			cpui.loadProfiles( this.cp.getProfiles() );
			jmcc.updateStatus("Ready...", false );
		}
		catch( Exception e )
		{
			cpui.showErrorMessage("Error while deleting profile!");
		}		
	}
}