package main;

import ui.startingscreen;
import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {
    public GameWindow() {
        this.setTitle("Tank Battle");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        startingscreen menu = new startingscreen();
        GamePanel gamePanel = new GamePanel();
        gamePanel.setLayout(new BorderLayout());
        gamePanel.add(menu, BorderLayout.CENTER);

        this.add(gamePanel);
        this.pack();

        this.setLocationRelativeTo(null);
        this.setVisible(true);
        menu.requestFocusInWindow();
        gamePanel.startGameThread();
    }

    public static void main(String[] args) {
        new GameWindow();
    }
}