package nl.errorsoft.esqlmanager.gui;

import nl.errorsoft.esqlmanager.control.*;
import javax.swing.*;

public class HyperLinkListener implements javax.swing.event.HyperlinkListener 
{	
	ConnectionWindowCC cwcc;

	public HyperLinkListener( ConnectionWindowCC cwcc )
	{	
		this.cwcc = cwcc;
	}

	public void hyperlinkUpdate( javax.swing.event.HyperlinkEvent e ) 
	{	
		if( e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED ) 
		{	
			JEditorPane pane = (JEditorPane) e.getSource();
			
			try 
			{	if( e.getURL().getFile().indexOf("USERMANAGER") != -1 )
				{	
					//UserPriviliges up = new UserPriviliges(jm, cw);
				}
				else
				{	pane.setPage(e.getURL());
				}
			}
			catch (Throwable t) 
			{	System.out.println(t.getMessage());
			}
		}
	}
}
