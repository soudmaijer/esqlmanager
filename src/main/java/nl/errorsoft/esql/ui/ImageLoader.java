package nl.errorsoft.esql.ui;

import java.awt.*;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class ImageLoader {
	private String imgpath;
	private Image[] images = new Image[0];
	private String[] names = new String[0];
	private java.util.Map<String, Icon> icons = new java.util.HashMap<>();

	public ImageLoader(String imgpath) {
		this.imgpath = imgpath;
	}

	public void addImage(String img, String name) {
		Image tmp = Toolkit.getDefaultToolkit().getImage(getResource(imgpath + img));

		MediaTracker m = new MediaTracker(new Canvas());
		m.addImage(tmp, 1);
		try {
			m.waitForAll();
			this.expand();
			images[0] = tmp;
			names[0] = name;
		} catch (Exception e) {
		}
	}

	// Modern classloaders reject ".." in resource names, so normalise the path (relative to the classpath root) first
	private java.net.URL getResource(String path) {
		String name = java.nio.file.Paths.get("/", path).normalize().toString().substring(1);
		return this.getClass().getClassLoader().getResource(name);
	}

	/** Registers a vector icon (a file in icons/svg) that follows the theme: grey in the toolbar, the selection colour when it is drawn selected. */
	public void addIcon(String name, String svg, int size, boolean selected) {
		Color base = new Color(0x6e6e6e);
		String key = selected ? "Tree.selectionForeground" : "Actions.Grey";
		FlatSVGIcon icon = new FlatSVGIcon("icons/svg/" + svg + ".svg", size, size, getClass().getClassLoader());
		icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> color.equals(base) && UIManager.getColor(key) != null ? UIManager.getColor(key) : color));
		icons.put(name, icon);
	}

	/** Icons are created once per image, renderers ask for them on every repaint. */
	public Icon getIcon(String name) {
		Icon icon = icons.get(name);

		if (icon == null && getImage(name) != null) {
			icon = new ImageIcon(getImage(name));
			icons.put(name, icon);
		}
		return icon;
	}

	public Image getImage(String name) {
		for (int i = 0; i < names.length; i++) {
			if (names[i].equalsIgnoreCase(name)) {
				return images[i];
			}
		}
		return null;
	}

	private void expand() {
		Image[] img = new Image[images.length + 1];
		String[] nms = new String[names.length + 1];
		for (int i = 0; i < images.length; i++) {
			img[i + 1] = images[i];
			nms[i + 1] = names[i];
		}
		images = img;
		names = nms;
	}
}
