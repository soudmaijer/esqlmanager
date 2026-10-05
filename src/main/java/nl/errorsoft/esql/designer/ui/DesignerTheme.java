package nl.errorsoft.esql.designer.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.UIManager;

/**
 * Colours and fonts of the designer, taken from the look and feel so that the diagram follows a light and a dark theme. Every value is looked up when it is
 * painted, so switching the theme needs no restart.
 */
public final class DesignerTheme {
	/** Space around a card for its shadow. The card itself starts this far inside its component. */
	public static final int SHADOW = 8;
	public static final int RADIUS = 10;

	private DesignerTheme() {
	}

	public static Color canvas() {
		return color("Panel.background", Color.WHITE);
	}

	public static Color card() {
		return isDark() ? mix(canvas(), Color.WHITE, 0.06f) : color("Table.background", Color.WHITE);
	}

	public static Color accent() {
		return color("Component.accentColor", new Color(0x2675bf));
	}

	/** Text on the accent colour. */
	public static Color onAccent() {
		return color("Tree.selectionForeground", Color.WHITE);
	}

	public static Color text() {
		return color("Label.foreground", Color.BLACK);
	}

	public static Color muted() {
		return color("Label.disabledForeground", Color.GRAY);
	}

	public static Color border() {
		return color("Component.borderColor", Color.LIGHT_GRAY);
	}

	public static Color hover() {
		return translucent(accent(), isDark() ? 60 : 30);
	}

	public static Color grid() {
		return isDark() ? mix(canvas(), Color.WHITE, 0.12f) : mix(canvas(), Color.BLACK, 0.12f);
	}

	public static Color note() {
		return isDark() ? mix(canvas(), new Color(0xffd54f), 0.18f) : new Color(0xfff6c8);
	}

	public static Color noteBorder() {
		return isDark() ? mix(canvas(), new Color(0xffd54f), 0.45f) : new Color(0xe6d27a);
	}

	/** One layer of the soft shadow; the layers are painted on top of each other. */
	public static Color shadow() {
		return new Color(0, 0, 0, isDark() ? 40 : 14);
	}

	public static Font font() {
		Font font = UIManager.getFont("Label.font");
		return font != null ? font : new Font(Font.DIALOG, Font.PLAIN, 12);
	}

	public static Font bold() {
		return font().deriveFont(Font.BOLD);
	}

	public static Font small() {
		return font().deriveFont(font().getSize2D() - 1f);
	}

	public static boolean isDark() {
		Color background = canvas();
		return (background.getRed() * 299 + background.getGreen() * 587 + background.getBlue() * 114) / 1000 < 128;
	}

	/** Anti-aliasing for shapes and text. */
	public static void smooth(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	public static Color translucent(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private static Color mix(Color from, Color to, float amount) {
		return new Color(Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
			Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
			Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
	}

	private static Color color(String key, Color fallback) {
		Color color = UIManager.getColor(key);
		return color != null ? color : fallback;
	}
}
