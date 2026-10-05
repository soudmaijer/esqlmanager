package nl.errorsoft.esql.user.ui;

import nl.errorsoft.esql.app.ui.ESQLManagerUI;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import nl.errorsoft.esql.user.control.UserManagerCC;
import nl.errorsoft.esql.user.DatabaseUser;
import nl.errorsoft.esql.user.GrantTarget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Manages the accounts of the server and the privileges they have on the server, a database or a table. */
public class UserManagerUI extends JDialog
{
	private static final Logger log = LogManager.getLogger( UserManagerUI.class );
	private static final String LOADING = "Loading...";

	private final UserManagerCC cc;
	private final DefaultListModel<DatabaseUser> userModel = new DefaultListModel<DatabaseUser>();
	private final JList<DatabaseUser> users = new JList<DatabaseUser>( userModel );
	private final DefaultTreeModel treeModel = new DefaultTreeModel( new DefaultMutableTreeNode( GrantTarget.global() ) );
	private final JTree tree = new JTree( treeModel );
	private final JPanel privilegePanel = new JPanel( new GridLayout( 0, 2, 8, 4 ) );
	private final JButton apply = new JButton( "Apply" );
	private final JLabel message = new JLabel( " " );
	private final JButton changePassword = new JButton( "Password" );
	private final JButton delete = new JButton( "Delete" );

	public UserManagerUI( ESQLManagerUI owner, UserManagerCC cc ) throws Exception
	{
		super( owner, "User manager", false );
		this.cc = cc;

		JPanel userPanel = new JPanel( new BorderLayout( 0, 6 ) );
		userPanel.setBorder( BorderFactory.createTitledBorder( "Users" ) );
		userPanel.add( new JScrollPane( users ), BorderLayout.CENTER );
		userPanel.add( userButtons(), BorderLayout.SOUTH );

		JPanel privileges = new JPanel( new BorderLayout( 0, 6 ) );
		privileges.setBorder( BorderFactory.createTitledBorder( "Privileges" ) );
		privileges.add( new JScrollPane( tree ), BorderLayout.CENTER );

		JPanel boxes = new JPanel( new BorderLayout( 0, 6 ) );
		// Keeps the checkboxes together at the top instead of spreading them over the height.
		boxes.add( privilegePanel, BorderLayout.NORTH );
		boxes.add( apply, BorderLayout.SOUTH );

		JSplitPane right = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, privileges, boxes );
		right.setResizeWeight( 0.5 );
		JSplitPane main = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, userPanel, right );
		main.setResizeWeight( 0.25 );

		message.setBorder( BorderFactory.createEmptyBorder( 4, 8, 4, 8 ) );
		getContentPane().add( main, BorderLayout.CENTER );
		getContentPane().add( message, BorderLayout.SOUTH );

		initTree();
		users.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		users.addListSelectionListener( e -> { if( !e.getValueIsAdjusting() ) userSelected(); } );
		tree.addTreeSelectionListener( ( TreeSelectionEvent e ) -> showGrants() );
		apply.addActionListener( e -> applyGrants() );
		loadUsers();
		showGrants();

		setSize( 900, 480 );
		main.setDividerLocation( 270 );
		right.setDividerLocation( 250 );
		setLocationRelativeTo( owner );
	}

	private JPanel userButtons()
	{
		JButton add = new JButton( "Add..." );
		add.addActionListener( e -> addUser() );
		changePassword.addActionListener( e -> changePassword() );
		delete.addActionListener( e -> deleteUser() );

		JPanel buttons = new JPanel( new GridLayout( 1, 3, 4, 0 ) );
		buttons.add( add );
		buttons.add( changePassword );
		buttons.add( delete );
		return buttons;
	}

	/** The root is the server, databases are added below it and load their tables when they are opened. */
	private void initTree() throws Exception
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)treeModel.getRoot();

		for( String database : cc.getDatabaseNames() )
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode( GrantTarget.database( database ) );
			node.add( new DefaultMutableTreeNode( LOADING ) );
			root.add( node );
		}

		tree.setRootVisible( true );
		tree.expandRow( 0 );
		tree.setSelectionRow( 0 );
		tree.addTreeWillExpandListener( new TreeWillExpandListener()
		{
			public void treeWillExpand( TreeExpansionEvent event )
			{
				loadTables( (DefaultMutableTreeNode)event.getPath().getLastPathComponent() );
			}

			public void treeWillCollapse( TreeExpansionEvent event )
			{
			}
		} );
	}

	private void loadTables( DefaultMutableTreeNode databaseNode )
	{
		if( databaseNode.getChildCount() != 1 || !LOADING.equals( ( (DefaultMutableTreeNode)databaseNode.getFirstChild() ).getUserObject() ) )
			return;

		try
		{
			String database = ( (GrantTarget)databaseNode.getUserObject() ).getDatabase();
			databaseNode.removeAllChildren();

			for( String table : cc.getTableNames( database ) )
				databaseNode.add( new DefaultMutableTreeNode( GrantTarget.table( database, table ) ) );

			treeModel.nodeStructureChanged( databaseNode );
		}
		catch( Exception e )
		{
			showError( "Can't load the tables", e );
		}
	}

	private void loadUsers()
	{
		DatabaseUser selected = users.getSelectedValue();
		userModel.clear();

		try
		{
			for( DatabaseUser user : cc.listUsers() )
				userModel.addElement( user );
		}
		catch( Exception e )
		{
			showError( "Can't load the users", e );
		}

		if( selected != null )
			users.setSelectedValue( selected, true );

		userSelected();
	}

	private void userSelected()
	{
		boolean selected = users.getSelectedValue() != null;
		changePassword.setEnabled( selected );
		delete.setEnabled( selected );
		showGrants();
	}

	/** The target selected in the tree, null when there is none or it is a placeholder. */
	private GrantTarget selectedTarget()
	{
		TreePath path = tree.getSelectionPath();

		if( path == null )
			return null;

		Object object = ( (DefaultMutableTreeNode)path.getLastPathComponent() ).getUserObject();
		return object instanceof GrantTarget ? (GrantTarget)object : null;
	}

	private void showGrants()
	{
		privilegePanel.removeAll();
		DatabaseUser user = users.getSelectedValue();
		GrantTarget target = selectedTarget();
		apply.setEnabled( user != null && target != null );

		if( user != null && target != null )
		{
			try
			{
				Set<String> granted = cc.getGrants( user, target );

				for( String privilege : cc.getPrivileges( target.getScope() ) )
					privilegePanel.add( new JCheckBox( privilege, granted.contains( privilege ) ) );

				message.setText( user + " on " + describe( target ) );
			}
			catch( Exception e )
			{
				showError( "Can't read the privileges", e );
			}
		}
		privilegePanel.revalidate();
		privilegePanel.repaint();
	}

	private void applyGrants()
	{
		Set<String> selected = new LinkedHashSet<String>();

		for( Component component : privilegePanel.getComponents() )
		{
			JCheckBox box = (JCheckBox)component;

			if( box.isSelected() )
				selected.add( box.getText() );
		}

		try
		{
			cc.setGrants( users.getSelectedValue(), selectedTarget(), selected );
			showGrants();
			message.setText( "Privileges saved for " + users.getSelectedValue() + " on " + describe( selectedTarget() ) );
		}
		catch( Exception e )
		{
			showError( "Can't save the privileges", e );
		}
	}

	private void addUser()
	{
		JTextField name = new JTextField( 16 );
		JTextField host = new JTextField( "%", 16 );
		JPasswordField password = new JPasswordField( 16 );
		JPanel form = new JPanel( new GridLayout( 0, 2, 6, 6 ) );
		form.add( new JLabel( "Name" ) );
		form.add( name );

		if( cc.usesHost() )
		{
			form.add( new JLabel( "Host" ) );
			form.add( host );
		}
		form.add( new JLabel( "Password" ) );
		form.add( password );

		if( JOptionPane.showConfirmDialog( this, form, "Add user", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE ) != JOptionPane.OK_OPTION )
			return;

		if( name.getText().trim().length() == 0 )
		{
			message.setText( "A user needs a name" );
			return;
		}

		try
		{
			DatabaseUser user = new DatabaseUser( name.getText().trim(), cc.usesHost() ? host.getText().trim() : null );
			cc.createUser( user, new String( password.getPassword() ) );
			loadUsers();
			message.setText( "Created user " + user );
		}
		catch( Exception e )
		{
			showError( "Can't create the user", e );
		}
	}

	private void changePassword()
	{
		JPasswordField password = new JPasswordField( 16 );

		if( JOptionPane.showConfirmDialog( this, password, "New password for " + users.getSelectedValue(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE ) != JOptionPane.OK_OPTION )
			return;

		try
		{
			cc.changePassword( users.getSelectedValue(), new String( password.getPassword() ) );
			message.setText( "Changed the password of " + users.getSelectedValue() );
		}
		catch( Exception e )
		{
			showError( "Can't change the password", e );
		}
	}

	private void deleteUser()
	{
		DatabaseUser user = users.getSelectedValue();

		if( JOptionPane.showConfirmDialog( this, "Delete user " + user + "?", "Delete user", JOptionPane.YES_NO_OPTION ) != JOptionPane.YES_OPTION )
			return;

		try
		{
			cc.dropUser( user );
			loadUsers();
			message.setText( "Deleted user " + user );
		}
		catch( Exception e )
		{
			showError( "Can't delete the user", e );
		}
	}

	private String describe( GrantTarget target )
	{
		switch( target.getScope() )
		{
			case GLOBAL: return "the server";
			case DATABASE: return "database " + target.getDatabase();
			default: return "table " + target.getDatabase() + "." + target.getTable();
		}
	}

	private void showError( String text, Exception e )
	{
		log.error( text, e );
		message.setText( text + ": " + e.getMessage() );
		JOptionPane.showMessageDialog( this, text + ": " + e.getMessage(), getTitle(), JOptionPane.WARNING_MESSAGE );
	}
}
