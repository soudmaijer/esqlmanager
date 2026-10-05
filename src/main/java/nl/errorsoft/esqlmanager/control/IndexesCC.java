//Source file: d:\\roseoutput\\esql\\esql\\table\\TableCC.java

package nl.errorsoft.esqlmanager.control;

import nl.errorsoft.esqlmanager.gui.*;
import nl.errorsoft.esqlmanager.data.*;
import nl.errorsoft.esqlmanager.domain.*;

public class IndexesCC 
{
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
		{	if( cwcc.getDatabaseConnection().getConnectionProfile().getServerType().getType() != ServerType.MY_SQL )   	   	
			{	cwcc.getUI().showErrorMessage("This feature is only available for MySQL");
	   		return;
	   	}
	   }
	   catch( Exception e )
	   {
	   }
	   
		iu = new IndexesUI( cwcc.getUI(), this );
		t.setColumns( t.getColumns(t) );
		t.setIndexes( t.getIndexes(t) );
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
				t.addIndex( t, ti, tc, type );
				iu.loadIndexes( t.getIndexes() );
				cwcc.tableSelected( t, true );
			}
		}
		catch( Exception e )
		{
			iu.showErrorMessage("Error while adding the index! " + e.getMessage() );
			e.printStackTrace();
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
					t.addIndex( t, ti, tc, type );
				else
					t.modifyIndex( t, ti, tc, type );
				
				iu.loadIndexes( t.getIndexes() );
				cwcc.tableSelected( t, true );
			}
		}
		catch( Exception e )
		{
			iu.showErrorMessage("Error while saving changed to the index! " + e.getMessage() );
			e.printStackTrace();
		}
	}	
	
	public void dropIndex( TableIndex ti )
	{
		try
		{		
			t.dropIndex( t, ti );
			iu.loadIndexes( t.getIndexes() );
			cwcc.tableSelected( t, true );
		}
		catch( Exception e )
		{
			iu.showErrorMessage("Error while dropping the index! " + e.getMessage() );
			e.printStackTrace();
		}
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