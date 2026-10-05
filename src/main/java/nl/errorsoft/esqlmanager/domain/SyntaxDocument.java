package nl.errorsoft.esqlmanager.domain;

import javax.swing.text.*;
import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class SyntaxDocument extends DefaultStyledDocument
{	
	Vector keywords = new Vector();
	
	public SyntaxDocument()
	{	
     	try
		{
			SAXBuilder saxbuilder = new SAXBuilder();
		  	Document driverData = saxbuilder.build(new File("conf/syntax.xml"));
			
    		List list = null;

    		if( driverData.hasRootElement() )
				list = driverData.getRootElement().getChildren("keyword");
	
			for(int i = 0; i < list.size(); i++)
			{		
				keywords.add ( ((Element)list.get(i)).getText()	);
			}
		}
		catch(Exception exception)
		{
		   System.out.println("Warning: syntax.xml could not be loaded, no syntax highlighting will be available!");
		}		
	}	
	
	public void append ( String text )
	{	text = text.replaceAll("'", " ' ");
		text = text.replaceAll("\"", " \" ");
		text = text.replaceAll("`", " ` ");	
		StringTokenizer st = new StringTokenizer (text, " ");

		boolean inquote  = false;
		String qstr = "";
		
		while ( st.hasMoreTokens( ) )
		{	String t = st.nextToken();
		
			if( t.indexOf("'") != -1 && !inquote)
			{	this.appendKeyword(  t.substring( 0, t.indexOf("'") ));
				this.append(t.substring( t.indexOf("'") , t.length()) + " ", new Color(71,134,41) , false );
				qstr = "'";
				inquote = true;
			}
			else if( t.indexOf("\"") != -1 && !inquote)
			{	this.appendKeyword(  t.substring( 0, t.indexOf("\"") ));
				this.append(t.substring( t.indexOf("\"") , t.length()) + " ", new Color(71,134,41) , false );
				qstr = "\"";
				inquote = true;
			}
			else if( t.indexOf("`") != -1 && !inquote)
			{	this.appendKeyword(  t.substring( 0, t.indexOf("`") ));
				this.append(t.substring( t.indexOf("`") , t.length()) + " ", new Color(71,134,41) , false );
				qstr = "`";
				inquote = true;
			}			
			else if ( inquote )
			{	if( t.indexOf(qstr) == -1 )
				{	
					this.append(t + " ", new Color(71,134,41), false );
				}
				else
				{	
					this.append(t.substring( 0, t.indexOf(qstr) + 1 ) + " ", new Color(71,134,41) , false );
					this.appendKeyword( t.substring( t.indexOf(qstr) + 1, t.length() ) + " ");
					inquote = false;
				}
			}						
			else
			{
				this.appendKeyword( t + " " );
			}
		}		
	}
	
	public void appendKeyword ( String text )
	{	for( int i = 0; i < keywords.size(); i ++ )
		{	
			if( keywords.get( i ).toString().equalsIgnoreCase( text.trim() ) )
			{	
				this.append( text, Color.blue, true );		
				return;
			}
		}
		this.append( text, Color.black, false );					
	}
	
	private void append( String text, Color c, boolean bold )
	{	
		SimpleAttributeSet sas = new SimpleAttributeSet ();
		StyleConstants.setBold(sas, bold);
		StyleConstants.setForeground(sas, c);
		try
		{	
			this.insertString(this.getLength(), text, sas);	
		}
		catch( Exception e )
		{
		}
	}
}