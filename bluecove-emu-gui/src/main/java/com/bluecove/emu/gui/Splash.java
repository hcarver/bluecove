package com.bluecove.emu.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JWindow;

public class Splash {

	/**
	 * Constructs a splash window to be displayed during the construction of the
	 * application.
	 * 
	 * @return Returns a reference to the displaying splash window.
	 */
	protected static Window createSplashWindow() {
		JWindow splashWindow = new JWindow();

		splashWindow.getContentPane().add(createSplash(), BorderLayout.CENTER);
		splashWindow.pack();

		return splashWindow;
	}

	protected static void createSplashDialog(Component parentComponent) {
		JOptionPane.showOptionDialog(parentComponent, createSplash(), "About", JOptionPane.DEFAULT_OPTION, 
				JOptionPane.PLAIN_MESSAGE, null, null, null);
	}
	
	private static JLabel createSplash() {
		JLabel image = null;
		try {
			image = new JLabel(new ImageIcon(ImageIO.read(BluecoveEmulatorUI.class
					.getResource("/images/splash.png"))));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		image.setBorder(BorderFactory.createRaisedBevelBorder());
		JLabel label = new JLabel("v" + BluecoveEmulatorUI.VERSION_NUMBER);
		image.setLayout(null);
		image.add(label);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		label.setForeground(new Color(46, 93, 248));
		label.setBounds(190, 360, 150, 30);
		return image;
	}
}
