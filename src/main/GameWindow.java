package main;

import ui.startingscreen;
import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {
    public GameWindow(GameConfig config) {
        this.setTitle("Tank Battle");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        startingscreen menu = new startingscreen();
        GamePanel gamePanel = new GamePanel(config);
        gamePanel.setLayout(new BorderLayout());

        // If config requests immediate start, skip adding the main menu and show game directly
        if (!config.startImmediately) {
            gamePanel.add(menu, BorderLayout.CENTER);
        }

        this.add(gamePanel);
        this.pack();

        this.setLocationRelativeTo(null);
        this.setVisible(true);

        if (!config.startImmediately) {
            menu.requestFocusInWindow();
        } else {
            gamePanel.requestFocusInWindow();
        }
        gamePanel.startGameThread();
    }

    public static void main(String[] args) {
        new GameWindow(new GameConfig());
    }
}