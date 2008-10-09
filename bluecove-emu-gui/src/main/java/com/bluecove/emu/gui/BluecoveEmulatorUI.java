package com.bluecove.emu.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

public class BluecoveEmulatorUI extends JFrame {

	/**
	 * Global static product identifier.
	 */
	public static final String VERSION_NUMBER = "0.1";

	/**
	 * Holds the application title for dialogs.
	 */
	public static String APPTITLE = "Bluecove Emulator Monitor";

	private Action exitAction;
	private Action aboutAction;
	
	private EmulatorPane emulatorPane;
	
	BluecoveEmulatorUI() {

		try {
			UIManager
					.setLookAndFeel("com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension screenSize = tk.getScreenSize();

		Window splashWindow = Splash.createSplashWindow();

		splashWindow.setLocation(screenSize.width / 2
				- (splashWindow.getSize().width / 2), screenSize.height / 2
				- (splashWindow.getSize().height / 2));
		splashWindow.setVisible(true);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		createApplication();
		setBounds(50, 50, screenSize.width - 100, screenSize.height - 100);
		splashWindow.dispose();
		setVisible(true);
	}

	private void createApplication() {
		createActions();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Bluecove Emulator Monitor");
		setJMenuBar(createMenuBar());
		emulatorPane = new EmulatorPane();
		getContentPane().add(emulatorPane); 
	}

	private void createActions() {
		exitAction = new AbstractAction("Exit") {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		};
		
		aboutAction = new AbstractAction("About") {
			public void actionPerformed(ActionEvent event) {
				Splash.createSplashDialog(BluecoveEmulatorUI.this);
			}
		};
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		menuBar.add(fileMenu);
		
		fileMenu.add(exitAction);
		
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		menuBar.add(helpMenu);
		
		helpMenu.add(aboutAction);
		
		return menuBar;
	}

	public static void main(String[] args) {
		new BluecoveEmulatorUI();
	}
}
