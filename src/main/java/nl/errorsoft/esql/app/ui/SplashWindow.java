package nl.errorsoft.esql.app.ui;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.app.control.MainController;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JFrame;
import javax.swing.Timer;

public class SplashWindow extends javax.swing.JWindow implements MouseListener {
	private MainWindow mainWindow;
	private MainController mainController;
	private int time = 0;
	private CreditsPanel credits;
	private Image splash;

	public SplashWindow(MainController mainController, MainWindow mainWindow, int time) {
		super((JFrame) mainWindow);

		this.mainWindow = mainWindow;
		this.mainController = mainController;

		this.setSize(400, 240);
		this.setLocation(mainWindow.getLocation().x + (int) ((mainWindow.getSize().width - this.getSize().width) / 2),
			mainWindow.getLocation().y + (int) ((mainWindow.getSize().height - this.getSize().height) / 2));
		this.time = time;
		this.addMouseListener(this);

		if (time == 0) {
			this.getContentPane().setLayout(null);
			credits = new CreditsPanel();
			credits.setBounds(0, 165, 400, 75);
			this.getContentPane().add(credits);
		}

		splash = ApplicationContext.get().imageLoader().getImage("esql");
		this.setVisible(true);

		if (time > 0) {
			// A Swing timer, so that closing the splash and what follows run on the event thread.
			Timer timer = new Timer(time, e -> {
				cleanUp();
				mainController.splashReady();
			});
			timer.setRepeats(false);
			timer.start();
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (time == 0) {
			cleanUp();
		}
	}

	public void cleanUp() {
		if (credits != null) {
			credits.switchoff();
			credits = null;
		}
		dispose();
	}

	public void mouseReleased(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mousePressed(MouseEvent e) {
	}

	public void paint(Graphics g) {
		java.util.Calendar cal = java.util.Calendar.getInstance();
		Graphics2D g1 = (Graphics2D) this.getGraphics();
		g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g1.drawImage(splash, 0, 0, this);
		g1.setColor(Color.black);
		g1.setFont(new Font("Arial", Font.BOLD, 12));
		g1.drawString(mainController.getAppName(), 17, 196);
		g1.setFont(new Font("Arial", Font.PLAIN, 11));
		g1.drawString("Version " + mainController.getAppVersion(), 17, 212);
		g1.drawString("Commit " + mainController.getAppCommit(), 17, 227);
		g1.drawString("http://www.errorsoft.nl", 274, 212);
		g1.drawString("© Copyright Errorsoft 2002-" + cal.get(java.util.Calendar.YEAR), 230, 227);
	}

	public void update(Graphics g) {
		paint(g);
	}
}
