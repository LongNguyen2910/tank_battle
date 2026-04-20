package main;

import entity.Bullet;
import entity.KeySetting;
import entity.Tank;
import entity.TankType;
import tile.TileManager;
import ui.startingscreen;
import ui.Pause;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
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
    private boolean paused = false;
    private Pause pauseOverlay;

    private final TileManager tileM = new TileManager(this);

    private final CollisionChecker cChecker = new CollisionChecker(this);

    private final ArrayList<Bullet> bulletList = new ArrayList<>();

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
    
    public GamePanel() {
        this.setPreferredSize(new Dimension(Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
        this.setLayout(new BorderLayout());

        // set up key binding for pause toggle (press P or ESC)
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, 0), "togglePause");
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "togglePause");
        am.put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePause();
            }
        });

        // create pause overlay and hide it initially; wire button callbacks
        pauseOverlay = new Pause();
        pauseOverlay.setPauseListener(new Pause.PauseListener() {
            @Override
            public void onResume() {
                // act like pressing resume
                if (paused) togglePause();
            }

            @Override
            public void onSetting() {
                // Ensure game remains paused and overlay stays visible while settings dialog is open
                paused = true;
                java.awt.Window win = SwingUtilities.getWindowAncestor(GamePanel.this);
                javax.swing.JFrame parent = null;
                if (win instanceof javax.swing.JFrame) parent = (javax.swing.JFrame) win;
                // Ensure overlay is in the layered pane and sized
                if (parent != null) {
                    JLayeredPane lp = parent.getLayeredPane();
                    if (pauseOverlay.getParent() != lp) lp.add(pauseOverlay, JLayeredPane.POPUP_LAYER);
                    Dimension sz = lp.getSize();
                    pauseOverlay.setBounds(0, 0, sz.width, sz.height);
                    pauseOverlay.layoutButtons();
                    pauseOverlay.setVisible(true);
                    lp.revalidate(); lp.repaint();
                }

                // open modal settings dialog; when it closes we keep paused=true and overlay visible
                new ui.SettingsDialog(parent).setVisible(true);
                // restore overlay focus
                if (pauseOverlay.getParent() instanceof JLayeredPane) {
                    pauseOverlay.requestFocusInWindow();
                }
            }

            @Override
            public void onQuit() {
                System.exit(0);
            }
        });
        pauseOverlay.setVisible(false);
        // do not add to this panel's layout; we'll place the overlay on the
        // root layered pane when pausing so it reliably appears above everything

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

            // pause toggle is handled via key bindings (P and ESC) so no polling here

            if (delta >= 1) {
                if (!paused) update();
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

        // pause overlay is a child component (Pause) and will draw itself when visible

        g2.dispose();
    }

    private void togglePause() {
        paused = !paused;
        if (pauseOverlay != null) {
            pauseOverlay.runPause(paused);
            java.awt.Window win = SwingUtilities.getWindowAncestor(this);
            if (paused) {
                if (win instanceof javax.swing.JFrame) {
                    javax.swing.JFrame frame = (javax.swing.JFrame) win;
                    JLayeredPane lp = frame.getLayeredPane();
                    // add overlay to layered pane so it is above everything
                    if (pauseOverlay.getParent() != lp) {
                        lp.add(pauseOverlay, JLayeredPane.POPUP_LAYER);
                    }
                    Dimension sz = lp.getSize();
                    pauseOverlay.setBounds(0, 0, sz.width, sz.height);
                    // position buttons after bounds set
                    pauseOverlay.layoutButtons();
                    pauseOverlay.setVisible(true);
                    pauseOverlay.requestFocusInWindow();
                    lp.revalidate();
                    lp.repaint();
                }
            } else {
                // remove overlay from layered pane
                if (pauseOverlay.getParent() instanceof JLayeredPane) {
                    JLayeredPane lp = (JLayeredPane) pauseOverlay.getParent();
                    lp.remove(pauseOverlay);
                    lp.revalidate();
                    lp.repaint();
                }
                this.requestFocusInWindow();
            }
        }
    }
    

    public void playerInit() {
        tankList.add(new Tank(this, keyH, TankType.NORMAL, 1, keySettingPlayer1));
        tankList.add(new Tank(this, keyH, TankType.HEAVY, 2, keySettingPlayer2));
        tankList.add(new Tank(this, keyH, TankType.SCOUT, 3, keySettingPlayer2));
        tankList.add(new Tank(this, keyH, TankType.MODERN, 4, keySettingPlayer2));
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
}
