package main;

import entity.Bullet;
import entity.KeySetting;
import entity.Tank;
import entity.TankType;
import tile.TileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable{
    final int originalTileSize = 32;
    public final int scale = 2;

    public final int tileSize = originalTileSize * scale;

    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;

    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    private final int FPS = 60;

    private Thread gameThread;
    private final KeyHandler keyH = new KeyHandler();

    private final TileManager tileM = new TileManager(this);

    private final CollisionChecker cChecker = new CollisionChecker(this);

    private final ArrayList<Bullet> bulletList = new ArrayList<>();

    private final KeySetting keySettingPlayer1 = new KeySetting(KeyEvent.VK_W, KeyEvent.VK_D,
            KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_K,KeyEvent.VK_J, KeyEvent.VK_U, KeyEvent.VK_I);

    private final KeySetting keySettingPlayer2 = new KeySetting(KeyEvent.VK_UP, KeyEvent.VK_RIGHT,
            KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_NUMPAD2, KeyEvent.VK_M, KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5);

    public final int xSpawnPlayer1 = 2 * tileSize + 10;
    public final int ySpawnPlayer1 = screenHeight - 2 * tileSize - 10;

    public final int xSpawnPlayer2 = screenWidth - 3 * tileSize;
    public final int ySpawnPlayer2 = screenHeight - 2 * tileSize - 10;

    public final int xSpawnPlayer3 = 2 * tileSize;
    public final int ySpawnPlayer3 = 2 * tileSize;

    public final int xSpawnPlayer4 = screenWidth - 3 * tileSize;
    public final int ySpawnPlayer4 = 2 * tileSize;

    private final ArrayList<Tank> tankList = new ArrayList<>();

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
        playerInit();
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = (double)1_000_000_000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        for (int i = tankList.size() - 1; i >= 0; i--) {
            Tank tank = tankList.get(i);
            tank.update();
            if (tank.isPendingRemoval()) {
                tankList.remove(i);
            }
        }
        for (int i = bulletList.size() - 1; i >= 0; i--) {
            Bullet b = bulletList.get(i);
            if (b.isAlive()) {
                b.update();
            } else {
                bulletList.remove(i);
            }
        }
        cChecker.checkHit();
        for (int i = bulletList.size() - 1; i >= 0; i--) {
            if (!bulletList.get(i).isAlive()) {
                bulletList.remove(i);
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        tileM.draw(g2);
        for (var tank : tankList) {
            tank.draw(g2);
        }
        for (Bullet b : bulletList) {
            b.draw(g2);
        }
        g2.dispose();
    }

    public void playerInit() {
        tankList.add(new Tank(this, keyH, TankType.NORMAL, 1, keySettingPlayer1));
        tankList.add(new Tank(this, keyH, TankType.NORMAL, 2, keySettingPlayer2));
        tankList.add(new Tank(this, keyH, TankType.NORMAL, 3, keySettingPlayer2));
        tankList.add(new Tank(this, keyH, TankType.NORMAL, 4, keySettingPlayer2));
    }

    public CollisionChecker getCollisionChecker() {
        return cChecker;
    }

    public TileManager getTileManager() {
        return tileM;
    }

    public ArrayList<Bullet> getBulletList() {
        return bulletList;
    }

    public ArrayList<Tank> getTankList() {
        return tankList;
    }

    public void addBullet(Bullet bullet) {
        bulletList.add(bullet);
    }
}
