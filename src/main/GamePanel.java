package main;

import entity.*;
import tile.TileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class GamePanel extends JPanel implements Runnable{
    public final int tileSize = Config.TILE_SIZE;
    public final int screenWidth = Config.SCREEN_WIDTH;
    public final int screenHeight = Config.SCREEN_HEIGHT;
    public final int maxScreenCol = Config.MAX_SCREEN_COL;
    public final int maxScreenRow = Config.MAX_SCREEN_ROW;

    private final int FPS = Config.FPS;

    private Thread gameThread;
    private final KeyHandler keyH = new KeyHandler();

    private final TileManager tileM = new TileManager(this);

    private final CollisionChecker cChecker = new CollisionChecker(this);

    private final ArrayList<Bullet> bulletList = new ArrayList<>();
    private final ArrayList<SlowZone> slowZoneList = new ArrayList<>();

    private final KeySetting keySettingPlayer1 = new KeySetting(Config.P1_UP, Config.P1_RIGHT,
            Config.P1_DOWN, Config.P1_LEFT, Config.P1_SHOOT, Config.P1_DASH, Config.P1_SKILL1, Config.P1_SKILL2);

    private final KeySetting keySettingPlayer2 = new KeySetting(Config.P2_UP, Config.P2_RIGHT,
            Config.P2_DOWN, Config.P2_LEFT, Config.P2_SHOOT, Config.P2_DASH, Config.P2_SKILL1, Config.P2_SKILL2);

    public final int xSpawnPlayer1 = Config.X_SPAWN_PLAYER_1;
    public final int ySpawnPlayer1 = Config.Y_SPAWN_PLAYER_1;

    public final int xSpawnPlayer2 = Config.X_SPAWN_PLAYER_2;
    public final int ySpawnPlayer2 = Config.Y_SPAWN_PLAYER_2;

    public final int xSpawnPlayer3 = Config.X_SPAWN_PLAYER_3;
    public final int ySpawnPlayer3 = Config.Y_SPAWN_PLAYER_3;

    public final int xSpawnPlayer4 = Config.X_SPAWN_PLAYER_4;
    public final int ySpawnPlayer4 = Config.Y_SPAWN_PLAYER_4;

    private final ArrayList<Tank> tankList = new ArrayList<>();

    public ArrayList<SmokeParticle> particleList = new ArrayList<>();

    public GamePanel() {
        this.setPreferredSize(new Dimension(Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT));
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
        for (int i = slowZoneList.size() - 1; i >= 0; i--) {
            SlowZone zone = slowZoneList.get(i);
            if (zone.isExpired()) {
                slowZoneList.remove(i);
            } else {
                zone.update();
            }
        }
        for (int i = particleList.size() - 1; i >= 0; i--) {
            SmokeParticle p = particleList.get(i);
            if (p.alive) {
                p.update();
            } else {
                particleList.remove(i);
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        tileM.draw(g2);
        for (SlowZone zone : slowZoneList) {
            zone.draw(g2);
        }
        for (SmokeParticle p : particleList) {
            p.draw(g2);
        }
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
        tankList.add(new Tank(this, keyH, TankType.HEAVY, 2, keySettingPlayer1));
        tankList.add(new Tank(this, keyH, TankType.SCOUT, 3, keySettingPlayer1));
        tankList.add(new Tank(this, keyH, TankType.MODERN, 4, keySettingPlayer1));
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

    public KeySetting getKeySettingPlayer1() {
        return keySettingPlayer1;
    }

    public KeySetting getKeySettingPlayer2() {
        return keySettingPlayer2;
    }

    public void addBullet(Bullet bullet) {
        bulletList.add(bullet);
    }

    public void addSlowZone(SlowZone slowZone) {
        slowZoneList.add(slowZone);
    }

    public ArrayList<SlowZone> getSlowZoneList() {
        return slowZoneList;
    }
}
