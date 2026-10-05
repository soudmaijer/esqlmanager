package nl.errorsoft.esql.ui.icon;

import java.awt.*;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class ImageLoader {
	private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ImageLoader.class);
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
			if (m.isErrorAny()) {
				log.warn("Image {} ({}) could not be loaded", name, img);
			}
			this.expand();
			images[0] = tmp;
			names[0] = name;
		} catch (InterruptedException e) {
			// Interrupted while waiting for the image, which then stays unregistered; keep the interrupt for the caller.
			Thread.currentThread().interrupt();
			log.warn("Loading image {} ({}) was interrupted", name, img);
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

	/**
	 * Registers a brand logo (a file in icons/svg filled with its brand colour). On a dark theme the colour is made lighter so that it stays readable.
	 * The variant {@code name + "sel"} is drawn white for selected rows, with white details in the selection colour.
	 */
	public void addBrandIcon(String name, String svg, int size) {
		FlatSVGIcon icon = new FlatSVGIcon("icons/svg/" + svg + ".svg", size, size, getClass().getClassLoader());
		icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> com.formdev.flatlaf.FlatLaf.isLafDark() ? lighter(color) : color));
		icons.put(name, icon);
		FlatSVGIcon selected = new FlatSVGIcon("icons/svg/" + svg + ".svg", size, size, getClass().getClassLoader());
		selected.setColorFilter(new FlatSVGIcon.ColorFilter(ImageLoader::onSelection));
		icons.put(name + "sel", selected);
	}

	private static Color onSelection(Color color) {
		boolean white = color.getRed() > 240 && color.getGreen() > 240 && color.getBlue() > 240;
		Color selection = UIManager.getColor("Tree.selectionBackground");
		Color target = white ? (selection != null ? selection : Color.DARK_GRAY) : Color.WHITE;
		return new Color(target.getRed(), target.getGreen(), target.getBlue(), color.getAlpha());
	}

	/** The application logo (icons/logo.svg) rendered at the sizes a window and the Dock ask for. */
	public static java.util.List<Image> logoImages() {
		java.util.List<Image> images = new java.util.ArrayList<>();
		for (int size : new int[]{16, 32, 64, 128, 256, 512}) {
			images.add(new FlatSVGIcon("icons/logo.svg", size, size, ImageLoader.class.getClassLoader()).getImage());
		}
		return images;
	}

	private static Color lighter(Color color) {
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		return Color.getHSBColor(hsb[0], hsb[1] * 0.6f, Math.min(1f, Math.max(0.9f, hsb[2] + 0.25f)));
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
