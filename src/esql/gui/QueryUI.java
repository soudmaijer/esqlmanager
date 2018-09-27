package esql.gui;

import esql.control.*;
import esql.domain.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.UndoManager;

public class QueryUI extends JDialog implements ActionListener
{
	private String db;
	private JTextPane jt;
	private UndoManager ndo = new UndoManager();

	// Internal toolbar
	private JToolBar tbQuery;
	private Image imgRunQuery;
	private Image imgSaveQuery;
	private Image imgClose;
	private JButton btnSaveQuery;
	private JButton btnRunQuery;
	private JButton btnClose;
	private JCheckBox closeOnSuccess;
	private ImageLoader imgLoader;
	private Syntax syn;
	private ConnectionWindowCC cwcc;
	private JComboBox jcb;

	public QueryUI( ConnectionWindowCC cwcc, JFrame parent, Syntax syn, ImageLoader imgLoader, java.util.Vector databases, Database d )
	{
		super( parent, false);
		
		// Set vars
		this.imgLoader = imgLoader;
		this.syn = syn;
		this.cwcc = cwcc;
		this.setTitle("Run SQL query on `"+ cwcc.getTitle() +"`");

		// Load images
		imgRunQuery = this.imgLoader.getImage("imgDoRunQuery");
		imgSaveQuery = this.imgLoader.getImage("imgSave");
		imgClose = this.imgLoader.getImage("imgDeleteRow");

		// Toolbar
		tbQuery = new JToolBar( );
		tbQuery.setFloatable( false );
		tbQuery.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
		btnSaveQuery = new JButton( new ImageIcon( imgSaveQuery ) );
		btnSaveQuery.setToolTipText("Save query");
		btnSaveQuery.addActionListener( this );
		tbQuery.add( btnSaveQuery ) ;
		btnClose = new JButton( new ImageIcon( imgClose ) );
		btnClose.setToolTipText("Close");
		btnClose.addActionListener( this );
		tbQuery.add( btnClose ) ;
		btnRunQuery = new JButton( new ImageIcon( imgRunQuery ) );
		btnRunQuery.setToolTipText("Run query");
		btnRunQuery.addActionListener( this );
		tbQuery.add( btnRunQuery ) ;
		tbQuery.addSeparator();
		
		tbQuery.add( new JLabel(" Database: ") );
		jcb = new JComboBox( databases );
		jcb.setPreferredSize( new Dimension( 150, 20 ) );
		
		for( int i=0; i<jcb.getItemCount(); i++ )
		{
			if( ((Database)jcb.getItemAt(i)).getName().equals( d.getName() ) )
				jcb.setSelectedIndex(i);
		}
		
		tbQuery.add( jcb );
		tbQuery.addSeparator();
		
		closeOnSuccess = new JCheckBox("Close when ready.");
		tbQuery.add( this.closeOnSuccess );

		this.getContentPane().add( tbQuery, java.awt.BorderLayout.NORTH );
		this.setSize( (int)(parent.getToolkit().getScreenSize().getWidth()/2) , (int)(parent.getToolkit().getScreenSize().getHeight()/4) );
		this.setLocation(parent.getLocation().x + (int)((parent.getSize().width - this.getSize().width) / 2), parent.getLocation().y + (int)((parent.getSize().height - this.getSize().height) / 2));
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		jt = new JTextPane();
		jt.setBorder( new javax.swing.border.EmptyBorder( 0, 0, 0, 0 ) );
		jt.getDocument().addDocumentListener( syn );
		
		/** Listener for edits on a document. */
		UndoableEditListener undoHandler = new UndoHandler( ndo );
		jt.getDocument().addUndoableEditListener(undoHandler);
		jt.setRequestFocusEnabled( true );

		// Key listeners.
		jt.addKeyListener( new KeyAdapter()
		{
			public void keyPressed( KeyEvent evt )
 			{
 				if (evt.getKeyCode() == evt.VK_Z && evt.isControlDown() )
 				{
					if( ndo.canUndo() )
						ndo.undo();
				}
				else if (evt.getKeyCode() == evt.VK_Y && evt.isControlDown() )
				{
					if( ndo.canRedo() )
						ndo.redo();
				}
 			}
		} );

		JScrollPane jsp = new JScrollPane( jt );
		LineNumber lineNumber = new LineNumber( jt );
		jsp.setRowHeaderView( lineNumber );
		this.getContentPane().add(jsp);
		jt.requestFocus(true);

	}

	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();

		if( src == this.btnSaveQuery )
		{
			JFileChooser chooser = new JFileChooser();
  			chooser.addChoosableFileFilter( new ExtentionFileFilter("SQL file",new String[]{".sql"}));
			chooser.setAcceptAllFileFilterUsed( false );
  			chooser.setDialogTitle("Save query as...");

   		try
		   {
			  	if( chooser.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
			  	{
					String fileName = chooser.getSelectedFile().getAbsolutePath();

					if( !fileName.endsWith(".sql") )
						fileName = chooser.getSelectedFile().getAbsolutePath() + ".sql";

					java.io.PrintWriter pw = new java.io.PrintWriter( new java.io.FileOutputStream( new java.io.File( fileName ) ) );
					pw.println( jt.getText() );
					pw.close();
				}
			}
			catch( Exception err )
			{
			   JOptionPane pane = new JOptionPane();
			   pane.setMessageType(JOptionPane.OK_OPTION);
	   		pane.showMessageDialog( this, "An error occured while saving query!\n\n"+ err.getMessage(), "Save query", JOptionPane.WARNING_MESSAGE );
			}
		}
		else if( src == btnRunQuery )
		{
			if( jcb.getSelectedItem() != null && jcb.getSelectedItem() instanceof esql.domain.Database )
			{	cwcc.changeDatabase( (esql.domain.Database)jcb.getSelectedItem() );
			}
		
			String s = "";
			java.util.StringTokenizer st = new java.util.StringTokenizer( jt.getText(), ";", false );
			StringBuffer sb = new StringBuffer();
			
			for( int i=0; i<jt.getDocument().getLength(); i++ )
			{
				try
				{
					sb.append( jt.getDocument().getText(i,1) );
					
					if( sb.charAt( sb.length()-1 ) == '\n' )
					{
						String temp = sb.toString().substring( 0, sb.length()-1 ).trim();
						
						if( temp.endsWith(";") )
						{
							// execute the query.
							cwcc.runCustomSQL( temp );
							sb = new StringBuffer();
						}
					}
					else if( jt.getDocument().getLength()-1 == i )
					{
						String temp = sb.toString().substring( 0, sb.length() ).trim();

						// execute the query.
						cwcc.runCustomSQL( temp );
						sb = new StringBuffer();
					}
				}
				catch( Exception ex )
				{
					ex.printStackTrace();
				}
			}
			
			String temp = sb.toString().trim();
			
			if( temp.length() > 0 )
			{
				// execute the query.
				cwcc.runCustomSQL( temp );
			}

			if( closeOnSuccess.isSelected() )
			{
				this.dispose();
			}
	 	}
	 	else if( src == btnClose )
	 	{
	 		this.dispose();
	 	}
	}
}

