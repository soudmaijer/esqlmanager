package nl.errorsoft.esqlmanager.gui;

import java.io.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class Syntax implements DocumentListener
{	
	private SyntaxObject syn = null;
	private MutableAttributeSet keywords;
	private MutableAttributeSet standard;
	private MutableAttributeSet string;

	private boolean active = true;
	
	public Syntax ()
	{	
		keywords = new javax.swing.text.SimpleAttributeSet();		
		StyleConstants.setBold(keywords, true);		
		StyleConstants.setForeground(keywords, java.awt.Color.blue);
		
		standard = new javax.swing.text.SimpleAttributeSet();		
		StyleConstants.setBold(standard, false);		
		StyleConstants.setForeground(standard, java.awt.Color.black);
		
		string = new javax.swing.text.SimpleAttributeSet();		
		StyleConstants.setBold(string, false);		
		StyleConstants.setForeground(string, java.awt.Color.GREEN.darker().darker().darker());		
		
     	// Euhm hiero de syntax keywords laden? jarno hellup!!!
     	try
		{
			SAXBuilder saxbuilder = new SAXBuilder();
		  	Document driverData = saxbuilder.build(new File("conf/syntax.xml"));
		  	SyntaxObject current = null;
			
    		List list = null;

    		if( driverData.hasRootElement() )
				list = driverData.getRootElement().getChildren("keyword");
	
			for(int i = 0; i < list.size(); i++)
			{		
				
				
				if( syn == null )
				{	syn = new SyntaxObject(((Element)list.get(i)).getText());
					current = syn;
				}
				else
				{	SyntaxObject syn2 = new SyntaxObject(((Element)list.get(i)).getText());
					current.next = syn2;
					current = syn2;
				}				
				//keywords.add ( ((Element)list.get(i)).getText()	);*/
			}
		}
		catch(Exception exception)
		{
		   System.out.println("Warning: syntax.xml could not be loaded, no syntax highlighting will be available! " + exception.getMessage() );
		   exception.printStackTrace();
		}	
		/*
		try
		{	
		
		BufferedReader in = new BufferedReader(new FileReader(file));
			String input;
			SyntaxObject current = null;
			
			while( (input = in.readLine()) != null )
			{	if( syn == null )
				{	syn = new SyntaxObject(input);
					current = syn;
				}
				else
				{	SyntaxObject syn2 = new SyntaxObject(input);
					current.next = syn2;
					current = syn2;
				}
			}
		}
		catch(Exception e)
		{	
		}*/
	}
	
	private Point checkKeyword ( String word )
	{	SyntaxObject cur = syn;
		while(cur != null)
		{	if(word.toLowerCase().equals(cur.syntax.toLowerCase()))
			{	return new Point(word.toLowerCase().indexOf(cur.syntax.toLowerCase()), cur.syntax.length());
			}
			cur = cur.next;
		}
		return new Point(-1, -1);
	}
	
	private int containsSentenceChar(String word)
	{	int count = 0;
		for(int i = 0 ; i < word.length(); i++)
		{	if(word.charAt(i) == '"')
			{	count ++;
			}
		}
		for(int i = 0 ; i < word.length(); i++)
		{	if(word.charAt(i) == '\'')
			{	count ++;
			}
		}
		for(int i = 0 ; i < word.length(); i++)
		{	if(word.charAt(i) == '`')
			{	count ++;
			}
		}			
		return count;	
	}
	
	public void updateHighlight (StyledDocument sd, int sta, int len)
	{	try
		{	String text = sd.getText(0, sd.getLength());
			int endlength = sta + len;
			
			StringTokenizer st2 = new StringTokenizer(text, "\n", true);
			
			int offset = 0;
			int length;
			boolean range = false;
			
			int sentc = 0;
			int sentc2 = 0;
			int sentc3 = 0;
			boolean sent = false;
					
			while(st2.hasMoreTokens())
			{	String sentence = st2.nextToken();
				
				if(offset+sentence.length() >= sta && offset < endlength)
				{	range = true;
				}	
				if( sentence.equals("\n") )
				{	range = false;
				}
				
				if(range)
				{	StringTokenizer st = new StringTokenizer(sentence, " ", true);
					String token;
					while(st.hasMoreTokens())
					{	token = st.nextToken();
						length = token.length();
						
						Point keyword = checkKeyword(token);
						
						sentc2 = sentc;
						sentc = (sentc + this.containsSentenceChar(token));
						sentc3 = this.containsSentenceChar(token);
						
						if(sentc%2 != 0 && sentc > 0)
						{	sent = true;	}
						else
						{	if(sentc2%2 != 0 && sentc2 > 0)
							{	sent = true;	}
						}
						if(sentc3 > 0 && sentc3%2==0)
						{	sent = true;
						}
						
						if(sent)
						{	sd.setCharacterAttributes(offset, length, string, true);
						}
						else if(keyword.getX() != -1)
						{	sd.setCharacterAttributes(offset + (int)keyword.getX(), (int)keyword.getY(), keywords, true);
						}
						else
						{	sd.setCharacterAttributes(offset, length, standard, true);
						}						
						
						sent = false;
						
						offset += length;
					}
				}
				else
				{	offset += sentence.length();
				}
			}
		}
		catch(Exception ex)
		{	
		}
	}	
	
	public void insertUpdate(DocumentEvent e)
	{	if(active)
		{	final StyledDocument sd = (StyledDocument)e.getDocument();
			final DocumentEvent tmp = e;
			if(e.getDocument() instanceof StyledDocument)
			{
				SwingUtilities.invokeLater(new Runnable()		
				{	public void run()		
					{	updateHighlight(sd, tmp.getOffset(), tmp.getLength());			
					}		
				});
			}
		}
	}

	public void removeUpdate(DocumentEvent e)
	{	if(active)
		{	final StyledDocument sd = (StyledDocument)e.getDocument();
			final DocumentEvent tmp = e;
			if(e.getDocument() instanceof StyledDocument)
			{
				SwingUtilities.invokeLater(new Runnable()		
				{	public void run()		
					{	updateHighlight(sd, tmp.getOffset(), tmp.getLength());			
					}		
				});
			}
		}
	}	
	
	public void changedUpdate(DocumentEvent e)
	{
	}
	
	public void setActive(boolean active)
	{	this.active = active;
	}	
}

class SyntaxObject
{	String syntax;
	SyntaxObject prev = null;
	SyntaxObject next = null;

	public SyntaxObject (String syntax)
	{	this.syntax = syntax;
	}
}