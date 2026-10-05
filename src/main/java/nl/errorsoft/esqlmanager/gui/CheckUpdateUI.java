package nl.errorsoft.esqlmanager.gui;

import java.net.*;
import java.io.*;
import javax.swing.*;
import java.util.zip.*;
import java.awt.event.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import nl.errorsoft.esqlmanager.control.*;

public class CheckUpdateUI extends JDialog implements ActionListener, Runnable
{	
	private JProgressBar jp = new JProgressBar();
	private JProgressBar jp2 = new JProgressBar();
	private JButton stop = new JButton("Quit");
	
	private ESQLManagerUI jm;
	private int build;
	private int newestbuild;
	private JLabel jl3;
	private ESQLManagerCC jmcc;

	public CheckUpdateUI( ESQLManagerCC jmcc, ESQLManagerUI jm ) 
	{	
		super((JFrame)jm, false);
		
		this.jmcc = jmcc;
		this.jm = jm;
		this.build = jmcc.getAppBuild();		
		
		Thread t = new Thread( this );
		t.start();
	}
	
	public void run()
	{
		this.setSize(320,200);
		this.setLocation(jm.getLocation().x + (int)((jm.getSize().width - this.getSize().width) / 2), jm.getLocation().y + (int)((jm.getSize().height - this.getSize().height) / 2));
		this.getContentPane().setLayout(null);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle( "Auto-updater");
		
		Update [] updates;
		
		if( (updates = checkUpdate(build)) != null )
		{	
			JOptionPane pane = new JOptionPane();
			pane.setMessageType(JOptionPane.WARNING_MESSAGE);
			
			if( pane.showConfirmDialog( jm, "There is a new version of available, build " + newestbuild + "! \n Do you want to download and install the updates now ? ", "Web update detected", JOptionPane.YES_NO_OPTION ) != JOptionPane.YES_OPTION)
			{	
				this.dispose();
			}
			else
			{	
				JLabel jl = new JLabel("Preparing to install update...");
				jl.setBounds(10,10,300,15);
				this.getContentPane().add(jl);
				
				JLabel jl1 = new JLabel("Downloading...");
				jl1.setBounds(10,30,300,15);
				this.getContentPane().add(jl1);
				
				jp.setBounds(10,50,290,15);
				this.getContentPane().add(jp);
				
				JLabel jl2 = new JLabel("Installing update...");
				jl2.setBounds(10,70,290,15);
				this.getContentPane().add(jl2);
				
				jp2.setBounds(10,90,290,15);
				this.getContentPane().add(jp2);			
				
				jl3 = new JLabel("You must restart the program for changes to take effect.");
				jl3.setBounds(10,110,290,15);
				jl3.setVisible(false);
				this.getContentPane().add(jl3);	
				
				stop.setBounds(10,135,100,25);
				this.getContentPane().add(stop);
				stop.addActionListener(this);
				stop.setEnabled(false);		
				
				this.show();	
				
				try
				{	downloadAndUpdate( updates );
				}
				catch( Exception e )
				{	jl3.setText("Update installation failed... Restart application and try again!");
				}
				
				jl3.setVisible(true);
				stop.setEnabled(true);
			}
		}
		else
		{	
			jmcc.println("Auto-update: there are no new updates available.");
		}		
	}
	
	public Update [] checkUpdate(int build)
	{	
		// Ombouwen!!! pro en le moeten apparte update files krijgen!
		try 
		{	
			String fileext = ( jmcc.isPro() ) ? "pro" : "le";
			SAXBuilder builder = new SAXBuilder();
			org.jdom.Document sdata = builder.build( new URL("http://" + jmcc.getSettings().getUpdateServer() + "/update_"+ fileext +".xml" ) );
			
			Update [] updates = null;
			int loc = 0;
			if( sdata.hasRootElement() )
			{ 	java.util.List update = sdata.getRootElement().getChildren("update");

				updates = new Update [update.size()];

				for(int i = 0 ; i < update.size(); i++)
				{	int serverbuild = Integer.parseInt( ((Element)update.get(i)).getChild("build").getText());
			 		String download = ((Element)update.get(i)).getChild("file").getText();
			 		
			 		java.util.List mirrors = ((Element)update.get(i)).getChild("mirrors").getChildren("mirror");	
			 		
			 		if(serverbuild > build)
			 		{	updates [loc] = new Update(serverbuild, download, mirrors);
			 			newestbuild = serverbuild;
			 			loc ++;
			 		}
				}	
			}
			if( loc != 0 )
			{	return updates;
			}
		}
		catch(Exception e)
		{	
		}
		return null;
	}
	
	public void downloadAndUpdate ( Update [] updates ) throws Exception
	{	int totalsize = 0;
		for(int i = 0 ; i < updates.length; i++)
		{	if( updates[i] != null )
			{	updates[i].setMirror( (int)(Math.random() * updates[i].getMirrors().size()) );
				URL url = new URL("http://" + updates[i].getMirror() + "/" + updates[i].getFile());
				totalsize += url.openConnection().getContentLength();     
			}
		}
			
		jp.setMaximum(totalsize);
			
		int round = 0;
		for(int i = 0 ; i < updates.length; i++)
		{	if( updates[i] != null )
			{	URL url = new URL("http://" + updates[i].getMirror() + "/" + updates[i].getFile());
				File file = new File(".\\updates\\" + updates[i].getFile());    
		
				byte[] data = new byte[256];     
					
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream( file ) );    
				BufferedInputStream in = new BufferedInputStream(url.openStream());   
	 
				int bytesRead;
				
				while ((bytesRead = in.read(data, 0, data.length)) != -1) 
				{	out.write(data, 0, bytesRead);    
					jp.setValue(round * data.length);
					SwingUtilities.invokeLater(new Runnable()
					{
					  public void run()
					  {       
					    jp.updateUI();
					  }
					});

					round++;
				}  
					  
				out.flush();
				out.close();
				in.close();
			}
		}
		
		unzipDirArchive (updates);	
	}

	public void unzipDirArchive(Update [] updates) throws Exception
	{	ZipEntry entry = null;        
		int count = 0;
		
		for(int i = 0 ; i < updates.length; i++)
		{	if(updates[i] != null)
			{	ZipInputStream tmp = new ZipInputStream(new FileInputStream(( new File(".\\updates\\" + updates[i].getFile()) )));    
				while ((entry = tmp.getNextEntry()) != null) 
				{	if(!entry.isDirectory()) count++;		}
				tmp.close();
			}
		}
		
		jp2.setMaximum(count);
		
		for(int i = 0 ; i < updates.length; i++)
		{	if( updates [i] != null )
			{	entry = null;
				ZipInputStream zis = new ZipInputStream(new FileInputStream(( new File(".\\updates\\" + updates[i].getFile() ))));        
			
				FileOutputStream fos = null;        
				byte buffer[] = new byte[4096];        
				int bytesRead;        
				boolean dirCreated = false;     
				
				while ((entry = zis.getNextEntry()) != null) 
				{   if( !entry.isDirectory() )
					{  	jp2.setValue(jp2.getValue() + 1);
						Runnable doAppend = new Runnable() 
						{	public void run() 
							{
								jp2.updateUI();
							}
						};
						fos = new FileOutputStream(entry.getName());            
						
						while((bytesRead = zis.read(buffer)) != -1)                
						{	fos.write(buffer, 0, bytesRead);            
						}
						fos.close();         
					}
				} 
				zis.close();  
				
				new File(".\\updates\\" + updates[i].getFile()).delete();  
			}
		}
	}
	
	public void actionPerformed(ActionEvent e)
	{	if( e.getSource() == stop )
		{	System.exit(0);
		}
	}
}

class Update
{	private int build;
	private String file;
		
	private java.util.List mirrors;
	
	private int mirror = 0;
	
	public Update (int build, String file, java.util.List mirrors)
	{	this.file = file;
		this.build = build;
		this.mirrors = mirrors;
	}
	
	public String getFile()
	{	return file;
	}
	
	public int getBuild()
	{	return build;
	}
	
	public java.util.List getMirrors()
	{	return mirrors;
	}
	
	public String getMirror()
	{	return ((Element)mirrors.get(mirror)).getText();
	}
	
	public void setMirror ( int mirror )
	{	this.mirror = mirror;
	}
}