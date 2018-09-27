//Source file: d:\\roseoutput\\esql\\esql\\database\\DatabaseDataView.java

package esql.gui;

import javax.swing.*;
import javax.swing.tree.*;
import esql.control.*;
import esql.domain.*;

public class DatabaseTreeView extends JTree
{
   private DatabaseCC dcc;
   private DefaultTreeModel dtm;
   private DefaultMutableTreeNode rootNode;
   
   /**
    * @roseuid 3E05A70C029B
    */
   public DatabaseTreeView( DatabaseCC dcc, String title ) 
   {
		this.dcc = dcc;
		this.rootNode = new DefaultMutableTreeNode( title );
		this.setCellRenderer( new DatabaseTreeViewCellRenderer( dcc.getImageLoader() ) );
   }
   
   public void addDatabase( Database db )
   {
   	rootNode.add( new DefaultMutableTreeNode( db ) );
   	SwingUtilities.invokeLater( new Runnable(){ public void run() { updateUI(); }} );
   }

	public void deleteDatabase( Database database )
	{
   	for( int i=0; i<rootNode.getChildCount();i++ )
   	{
   		DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
   		
   		if( ( (Database)node.getUserObject() ).getName().equalsIgnoreCase( database.getName() ) )
   		{
   			rootNode.remove( node );
   			dtm.reload( rootNode );
   			break;
   		}
   	}
   	SwingUtilities.invokeLater( new Runnable(){ public void run() { updateUI(); }} );
	}

	public void deleteTable( Table table )
	{
   	for( int i=0; i<rootNode.getChildCount();i++ )
   	{
   		DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
   		
   		if( ( (Database)node.getUserObject() ).getName().equalsIgnoreCase( table.getDatabase().getName() ) )
   		{
   			for( int j=0; j<node.getChildCount(); j++ )
   			{
   				DefaultMutableTreeNode node1 = (DefaultMutableTreeNode)node.getChildAt(j);
   				
   				if( ( (Table)node1.getUserObject() ).getName().equalsIgnoreCase( table.getName() ) )
   				{	node.remove( node1 );
   					dtm.reload( node );
   					return;
   				}
   			}
   			
   		}
   	}
	}	

	public void deleteTableColumn( esql.domain.TableColumn tc )
	{
   	for( int i=0; i<rootNode.getChildCount();i++ )
   	{
   		DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
   		
   		if( ( (Database)node.getUserObject() ).getName().equalsIgnoreCase( tc.getTable().getDatabase().getName() ) )
   		{
   			for( int j=0; j<node.getChildCount(); j++ )
   			{
   				DefaultMutableTreeNode node1 = (DefaultMutableTreeNode)node.getChildAt(j);
   				
   				if( ( (Table)node1.getUserObject() ).getName().equalsIgnoreCase( tc.getTable().getName() ) )
   				{	
						for( int k=0; k<node1.getChildCount(); k++ )
   					{
		   				DefaultMutableTreeNode node2 = (DefaultMutableTreeNode)node1.getChildAt(k);
	   				
		   				if( ( (TableColumn)node2.getUserObject() ).getName().equalsIgnoreCase( tc.getName() ) )
		   				{	
	   						node1.remove( node2 );
	   						dtm.reload( node1 );
	   						return;
	   					}
	   				}
   				}
   			}
   			
   		}
   	}
	}		
	   
   public void loadDatabases( java.util.Vector v )
   {
   	dtm = new DefaultTreeModel( rootNode, false );
   	
   	for( int i=0; i<v.size();i++ )
   	{
   		rootNode.add( new DefaultMutableTreeNode( (Database)v.get(i) ) );
   	}
   	
   	setModel( dtm );
   	SwingUtilities.invokeLater( new Runnable(){ public void run() { updateUI(); }} );
   }
   
   public void loadTables( Database database, java.util.Vector tables )
   {
   	for( int i=0; i<rootNode.getChildCount();i++ )
   	{
   		DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
   		
   		if( ( (Database)node.getUserObject() ).getName().equalsIgnoreCase( database.getName() ) )
   		{
   			node.removeAllChildren();
   			
   			for( int j=0; j<tables.size(); j++ )
   				node.add( new DefaultMutableTreeNode( (Table)tables.get(j) ) );
   				
				TreePath tempPath = new TreePath( node.getPath() );
				this.scrollPathToVisible( tempPath );
				this.expandPath( tempPath );
				this.setSelectionPath( tempPath );
   			break;
   		}
   	}
   	SwingUtilities.invokeLater( new Runnable(){ public void run() { updateUI(); }} );
	}

   public void loadTableColumns( Table table, esql.domain.TableColumn[] columns )
   {
   	for( int i=0; i<rootNode.getChildCount();i++ )
   	{
   		DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
   		
   		if( ( (Database)node.getUserObject() ).getName().equalsIgnoreCase( table.getDatabase().getName() ) )
   		{
   			for( int a=0; a<node.getChildCount(); a++ )
   			{
   				DefaultMutableTreeNode tableNode = (DefaultMutableTreeNode)node.getChildAt(a);
   				
   				if( ( (Table)tableNode.getUserObject() ).getName().equalsIgnoreCase( table.getName() ) )
   				{
   					tableNode.removeAllChildren();
		
		   			for( int j=0; j<columns.length; j++ )
		   			{
   						tableNode.add( new DefaultMutableTreeNode( columns[j] ) );
   					}
   					
   					TreePath tempPath = new TreePath( tableNode.getPath() );
   					this.scrollPathToVisible( tempPath );
   					this.expandPath( tempPath );
   					this.setSelectionPath( tempPath );
   					break;
   				}
   			}
   		}
   	}
   	SwingUtilities.invokeLater( new Runnable(){ public void run() { updateUI(); }} );
	}	
	
	public void selectTableInTree( Table table )
	{
   	for( int i=0; i<rootNode.getChildCount();i++ )
   	{
   		DefaultMutableTreeNode node = (DefaultMutableTreeNode)rootNode.getChildAt(i);
   		
   		if( ( (Database)node.getUserObject() ).getName().equalsIgnoreCase( table.getDatabase().getName() ) )
   		{
   			for( int a=0; a<node.getChildCount(); a++ )
   			{
   				DefaultMutableTreeNode tableNode = (DefaultMutableTreeNode)node.getChildAt(a);
   				
   				if( ( (Table)tableNode.getUserObject() ).getName().equalsIgnoreCase( table.getName() ) )
   				{
   					TreePath tempPath = new TreePath( tableNode.getPath() );
   					this.setSelectionPath( tempPath );
   					break;
   				}
   			}
   		}
   	}		
	}
}
