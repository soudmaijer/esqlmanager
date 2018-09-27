package nl.errorsoft.esqlmanager.domain;

import java.io.*;
import java.net.*;
import nl.errorsoft.esqlmanager.control.*;

public class Main
{
	// ClassPath problem fixed, source: http://forum.java.sun.com/thread.jsp?forum=32&thread=300557
	public Main() throws Exception
	{
		String version = System.getProperty("java.specification.version");
		
		if( version.matches("1.3") || version.matches("1.2") || version.matches("1.1") )
		{
			throw new Exception("\n\nWarning: you are using a wrong Java[TM] Runtime\nJava[TM] Runtime 1.4 or higher is required for eSQLManager!\nDetected Java[TM] version: "+ version +"\n");
		}
		
		DynamicLoader dl = new DynamicLoader();
		File f = new File( new URI( this.getClass().getClassLoader().getResource(".").toString() +"lib" ) );
		
		if( f.isDirectory() )
		{
			File [] fa = f.listFiles();
			URL [] u = new URL[ fa.length ];
			
			for( int i=0; i<fa.length; i++ )
			{
				dl.addURL( fa[i].toURL() );
			}
		}
		
		new nl.errorsoft.esqlmanager.control.ESQLManagerCC();
	}
	
	public static void main( String args[] ) throws Exception
	{
		new Main();
	}
}
