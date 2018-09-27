package nl.errorsoft.dbcreator.gui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Properties extends JDialog implements ActionListener
{	private Component cur;
	private JPanel cont;
	
	private JButton ok = new JButton("Ok");
	private JButton cancel = new JButton("Cancel");

	public Properties ( JFrame jm )
	{	super(jm, true);
		this.setSize(275,350);
		this.setTitle("Properties");
		this.setResizable(false);
		
		this.setLocation(jm.getLocation().x + (int)((jm.getSize().width - this.getSize().width) / 2), jm.getLocation().y + (int)((jm.getSize().height - this.getSize().height) / 2));
		
		JPanel jp = new JPanel();
		jp.setPreferredSize(new Dimension(280,340));
		this.setContentPane(jp);
		this.getContentPane().setLayout(null);
		
		cont = new JPanel();
		cont.setBounds(5,5,270,300);
		this.getContentPane().add(cont);
		cont.setLayout(new BorderLayout());
		
		cancel.setBounds(200,310,75,25);
		this.getContentPane().add(cancel);

		ok.setBounds(120,310,75,25);
		this.getContentPane().add(ok);		
		
		ok.addActionListener(this);
		cancel.addActionListener(this);
		
		this.pack();
	}
	
	public void showProperties( Object obj )
	{	if(cur != null)
			cont.remove(cur);
		if ( obj instanceof TableObject )
		{	TableProperties tp = new TableProperties((TableObject)obj);
			this.setTitle("Properties for '" + ((TableObject)obj).getName() + "'");
			this.cont.add(tp);
			cur = tp;
		}
		if ( obj instanceof DatabaseObject )
		{	DatabaseProperties tp = new DatabaseProperties((DatabaseObject)obj);
			this.setTitle("Properties for '" + ((DatabaseObject)obj).getName() + "'");
			this.cont.add(tp);
			cur = tp;
		}
		if( obj instanceof nl.errorsoft.dbcreator.gui.model.Model )
		{
			ModelProperties mp = new ModelProperties((nl.errorsoft.dbcreator.gui.model.Model)obj);	
			this.setTitle("Properties for '" + ((nl.errorsoft.dbcreator.gui.model.Model)obj).getName() + "'");
			this.cont.add(mp);
			cur = mp;
		}		
	}
	
	public void actionPerformed( ActionEvent e )
	{	if ( e.getSource() == ok )
		{	PropertiesInterface pi = (PropertiesInterface)cur; 
			pi.saveProperties();
			this.setVisible(false);
		}
		else
		{	this.setVisible(false);
		}
	}
}