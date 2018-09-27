package nl.errorsoft.esqlmanager.gui;

import java.io.File;
import javax.swing.filechooser.*;
import javax.swing.JFileChooser;

public class ExtentionFileFilter extends FileFilter
{

	String[] extensions;
	String desc;

	public ExtentionFileFilter(String desc,String[] extensions)
	{
		this.extensions=extensions!=null?extensions:new String[]{};
		this.desc=desc==null?extList():desc+" ("+extList()+")";
	}

	private String extList()
	{
		int len=extensions.length;
		if(len>0)
		{
			String ret=extensions[0];

			for(int j=1; j<len; ret+=", "+extensions[j++]);
				return ret;
		}
		else return "";
	}

	public boolean accept(File f)
	{
		if (f.isDirectory()) {
			return true;	
    	}

		if(f.isFile())
		{
			String fname=f.getName().toLowerCase();

			for(int j=extensions.length; j-->0;)
			{
				if(fname.endsWith(extensions[j]))
					return true;
			}
		}
		return false;
	}
	public String getDescription()
	{    return desc;
	}
}