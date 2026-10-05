//Source file: d:\\roseoutput\\esql\\esql\\table\\TableCC.java

package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;
import nl.errorsoft.esql.connection.control.ConnectionWindowCC;

import nl.errorsoft.esql.table.*;
import nl.errorsoft.esql.table.ui.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.domain.dialect.Dialect;
import nl.errorsoft.esql.data.*;
import nl.errorsoft.esql.domain.*;
import nl.errorsoft.esql.table.TableService;

public class IndexesCC 
{
	private static final Logger log = LogManager.getLogger( IndexesCC.class );

  	private ConnectionWindowCC cwcc;
	private Table t;
	private IndexesUI iu;

   public IndexesCC( ConnectionWindowCC cwcc, Table t ) 
   {
		this.cwcc = cwcc;
		this.t = t;
   }
   
	public void startUI( ESQLManagerUI eu ) throws Exception
	{
		try
		{	if( !cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getDialect().supports( Dialect.Feature.INDEXES ) )   	   	
			{	cwcc.getUI().showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	   }
	   catch( Exception e )
	   {
	   }
	   
		iu = new IndexesUI( cwcc.getUI(), this );
		service().loadColumns( t );
		service().loadIndexes( t );
		iu.loadIndexes( t.getIndexes() );
	}
	
	public void addIndex( TableIndex ti, TableColumn [] tc, String type )
	{
		try
		{		
			if( tc.length <= 0 )
				iu.showErrorMessage("No columns specified!");
			else
			{
				service().addIndex( t, ti, tc, type );
				iu.loadIndexes( t.getIndexes() );
				cwcc.tableSelected( t, true );
			}
		}
		catch( Exception e )
		{
			iu.showErrorMessage("Error while adding the index! " + e.getMessage() );
			log.error( e.getMessage(), e );
		}
	}

	public void modifyIndex( TableIndex ti, TableColumn [] tc, String type )
	{
		try
		{		
			if( tc.length <= 0 )
				iu.showErrorMessage("No columns specified!");
			else
			{
				if( ti.isNew() )
					service().addIndex( t, ti, tc, type );
				else
					service().modifyIndex( t, ti, tc, type );
				
				iu.loadIndexes( t.getIndexes() );
				cwcc.tableSelected( t, true );
			}
		}
		catch( Exception e )
		{
			iu.showErrorMessage("Error while saving changed to the index! " + e.getMessage() );
			log.error( e.getMessage(), e );
		}
	}	
	
	public void dropIndex( TableIndex ti )
	{
		try
		{		
			service().dropIndex( t, ti );
			iu.loadIndexes( t.getIndexes() );
			cwcc.tableSelected( t, true );
		}
		catch( Exception e )
		{
			iu.showErrorMessage("Error while dropping the index! " + e.getMessage() );
			log.error( e.getMessage(), e );
		}
	}

	private TableService service() throws Exception
	{
		return new TableService( cwcc.getDatabaseConnection() );
	}

	public void addNew( String name )
	{
		TableIndex ti = new TableIndex(t);
		ti.setName( name );
		ti.setNew( true );
		iu.addNewIndex( ti, t.getColumns() );
	}

	public void addPrimary()
	{
		TableIndex ti = new TableIndex(t);
		ti.setName("PRIMARY");
		ti.setNew( true );
		iu.addNewIndex( ti, t.getColumns() );
	}	
}