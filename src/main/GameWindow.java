package main;

import javax.swing.*;

public class GameWindow extends JFrame {
    public GameWindow() {
        this.setTitle("Tank Battle");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);

        GamePanel gamePanel = new GamePanel();
        this.add(gamePanel);
        this.pack();

        this.setLocationRelativeTo(null);
        this.setVisible(true);

        gamePanel.startGameThread();
    }

    public static void main(String[] args) {
        new GameWindow();
    }
}
