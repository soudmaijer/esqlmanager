package nl.errorsoft.esqlmanager.gui;

import java.awt.*;

public class ImageLoader
{	private String imgpath;
	private Image [] images = new Image [0];
	private String [] names = new String [0];

	public ImageLoader (String imgpath)
	{	this.imgpath = imgpath;
	}

	public void addImage(String img, String name)
	{	Image tmp = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource(imgpath + img) );

		MediaTracker m = new MediaTracker(new Canvas());
		m.addImage(tmp,1);
		try
		{	m.waitForAll();
			this.expand();
			images[0] = tmp;
			names[0] = name;
		}
		catch(Exception e)
		{
		}
	}

	public Image getImage(String name)
	{	for(int i = 0 ; i < names.length; i++)
		{	if(names[i].equalsIgnoreCase(name))
			{	return images[i];
			}
		}
		return null;
	}

	private void expand()
	{	Image [] img = new Image[images.length+1];
		String [] nms = new String[names.length+1];
		for(int i = 0 ; i < images.length; i++)
		{	img[i+1] = images[i];
			nms[i+1] = names[i];
		}
		images = img;
		names = nms;
	}
}