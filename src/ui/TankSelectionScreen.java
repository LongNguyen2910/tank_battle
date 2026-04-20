package ui;

import entity.TankType;
import main.Config;
import main.GameConfig;
import main.GameWindow;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.geom.GeneralPath;

public class TankSelectionScreen extends JFrame {
    private GameConfig config;
    private TankType[] availableTanks = {TankType.NORMAL, TankType.HEAVY, TankType.SCOUT, TankType.MODERN};
    private int p1Index = 0;
    private int p2Index = 0;
    
    private SelectionPanel p1Panel;
    private SelectionPanel p2Panel;

    public TankSelectionScreen(GameConfig config) {
        this.config = config;
        setTitle("Select Your Tank");
        setSize(Config.MENU_WIDTH, Config.MENU_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        AnimatedBackgroundPanel mainPanel = new AnimatedBackgroundPanel("/ui/tank.jpg");
        mainPanel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("SELECT YOUR WAR MACHINE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.BOLD, 50));
        titleLabel.setForeground(new Color(255, 69, 0));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel selectionContainer = new JPanel(new GridLayout(1, config.playerCount, 50, 0));
        selectionContainer.setOpaque(false);
        selectionContainer.setBorder(BorderFactory.createEmptyBorder(0, 100, 0, 100));

        p1Panel = new SelectionPanel("PLAYER 1", true);
        selectionContainer.add(p1Panel);

        if (config.playerCount > 1) {
            p2Panel = new SelectionPanel("PLAYER 2", false);
            selectionContainer.add(p2Panel);
        }

        mainPanel.add(selectionContainer, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 30));
        footer.setOpaque(false);

        AnimatedTacticalButton backBtn = new AnimatedTacticalButton("BACK");
        backBtn.addActionListener(e -> {
            dispose();
            new LobbyScreen().setVisible(true);
        });
        footer.add(backBtn);

        AnimatedTacticalButton readyBtn = new AnimatedTacticalButton("BATTLE!");
        readyBtn.addActionListener(e -> {
            config.p1Tank = availableTanks[p1Index];
            if (config.playerCount > 1) config.p2Tank = availableTanks[p2Index];
            config.startImmediately = true; // Start the game directly
            dispose();
            new GameWindow(config);
        });
        footer.add(readyBtn);

        mainPanel.add(footer, BorderLayout.SOUTH);
        add(mainPanel);

        // Keyboard support
        setupKeyBindings(mainPanel);
    }

    private void setupKeyBindings(JPanel panel) {
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();

        im.put(KeyStroke.getKeyStroke("A"), "p1Left");
        im.put(KeyStroke.getKeyStroke("D"), "p1Right");
        im.put(KeyStroke.getKeyStroke("LEFT"), "p2Left");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "p2Right");
        im.put(KeyStroke.getKeyStroke("ENTER"), "start");

        am.put("p1Left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { p1Index = (p1Index - 1 + availableTanks.length) % availableTanks.length; p1Panel.repaint(); }
        });
        am.put("p1Right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { p1Index = (p1Index + 1) % availableTanks.length; p1Panel.repaint(); }
        });
        am.put("p2Left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if(config.playerCount > 1) { p2Index = (p2Index - 1 + availableTanks.length) % availableTanks.length; p2Panel.repaint(); } }
        });
        am.put("p2Right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if(config.playerCount > 1) { p2Index = (p2Index + 1) % availableTanks.length; p2Panel.repaint(); } }
        });
        am.put("start", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { 
                config.p1Tank = availableTanks[p1Index];
                if (config.playerCount > 1) config.p2Tank = availableTanks[p2Index];
                config.startImmediately = true; // Start the game directly
                dispose();
                new GameWindow(config);
            }
        });
    }

    class SelectionPanel extends JPanel {
        private String title;
        private boolean isP1;
        private BufferedImage[] tankSprites;

        public SelectionPanel(String title, boolean isP1) {
            this.title = title;
            this.isP1 = isP1;
            setOpaque(false);
            loadSprites();
        }

        private void loadSprites() {
            tankSprites = new BufferedImage[availableTanks.length];
            try {
                for (int i = 0; i < availableTanks.length; i++) {
                    BufferedImage sheet = ImageIO.read(getClass().getResourceAsStream(availableTanks[i].getImagePrefix()));
                    tankSprites[i] = sheet.getSubimage(0, 0, 32, 32); // Face up sprite
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int currentIndex = isP1 ? p1Index : p2Index;
            TankType currentType = availableTanks[currentIndex];

            // Draw Border
            g2.setColor(new Color(0, 255, 200, 50));
            g2.fillRect(10, 10, getWidth() - 20, getHeight() - 20);
            g2.setColor(new Color(0, 255, 200));
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(10, 10, getWidth() - 20, getHeight() - 20);

            // Title
            g2.setFont(new Font("Monospaced", Font.BOLD, 24));
            g2.setColor(Color.WHITE);
            g2.drawString(title, (getWidth() - g2.getFontMetrics().stringWidth(title))/2, 50);

            // Tank Sprite
            if (tankSprites[currentIndex] != null) {
                int spriteSize = 128;
                g2.drawImage(tankSprites[currentIndex], (getWidth() - spriteSize)/2, 80, spriteSize, spriteSize, null);
            }

            // Stats
            g2.setFont(new Font("Monospaced", Font.BOLD, 20));
            g2.setColor(new Color(255, 102, 0));
            String name = currentType.name();
            g2.drawString(name, (getWidth() - g2.getFontMetrics().stringWidth(name))/2, 250);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
            g2.setColor(Color.LIGHT_GRAY);
            String speed = "SPEED: " + currentType.getSpeed();
            String fuel = "FUEL: " + currentType.getFuel();
            g2.drawString(speed, (getWidth() - g2.getFontMetrics().stringWidth(speed))/2, 280);
            g2.drawString(fuel, (getWidth() - g2.getFontMetrics().stringWidth(fuel))/2, 305);
            
            // Controls hint
            g2.setFont(new Font("Monospaced", Font.ITALIC, 14));
            g2.setColor(new Color(0, 255, 200));
            String hint = isP1 ? "[A] PREV | NEXT [D]" : "[LEFT] PREV | NEXT [RIGHT]";
            g2.drawString(hint, (getWidth() - g2.getFontMetrics().stringWidth(hint))/2, getHeight() - 40);

            g2.dispose();
        }
    }

    // Reusing components from startingscreen
    class AnimatedTacticalButton extends JButton {
        private float hoverProgress = 0f;
        private boolean isTargetFocused = false;
        private Timer animTimer;

        public AnimatedTacticalButton(String text) {
            super(text);
            setFont(new Font("Monospaced", Font.BOLD, 24));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(250, 60));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isTargetFocused = true; animTimer.start(); }
                @Override public void mouseExited(MouseEvent e) { isTargetFocused = false; animTimer.start(); }
            });

            animTimer = new Timer(16, e -> {
                if (isTargetFocused && hoverProgress < 1f) {
                    hoverProgress = Math.min(1f, hoverProgress + 0.1f);
                    repaint();
                }
                else if (!isTargetFocused && hoverProgress > 0f) {
                    hoverProgress = Math.max(0f, hoverProgress - 0.1f);
                    repaint();
                }
                else animTimer.stop();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), chamfer = 20;
            GeneralPath shape = new GeneralPath();
            shape.moveTo(chamfer, 0); shape.lineTo(w-chamfer, 0); shape.lineTo(w, chamfer); shape.lineTo(w, h-chamfer);
            shape.lineTo(w-chamfer, h); shape.lineTo(chamfer, h); shape.lineTo(0, h-chamfer); shape.lineTo(0, chamfer); shape.closePath();
            int r = Math.max(0, Math.min(255, (int)(40 + 215 * hoverProgress)));
            int gr = Math.max(0, Math.min(255, (int)(40 + 62 * hoverProgress)));
            int b = Math.max(0, Math.min(255, (int)(40 - 40 * hoverProgress)));
            g2.setPaint(new GradientPaint(0,0, new Color(r,gr,b,200), 0,h, new Color(10,10,10,240)));
            g2.fill(shape);
            g2.setColor(new Color(255, 102, 0, Math.max(0, Math.min(255, (int)(255 * hoverProgress)))));
            g2.setStroke(new BasicStroke(2));
            g2.draw(shape);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class AnimatedBackgroundPanel extends JPanel {
        private Image bgImage;
        private List<Particle> particles = new ArrayList<>();
        private Random rand = new Random();
        private int scanlineY = 0;

        public AnimatedBackgroundPanel(String imagePath) {
            try { bgImage = ImageIO.read(getClass().getResourceAsStream(imagePath)); } catch (Exception e) {}
            for (int i = 0; i < 40; i++) particles.add(new Particle());
            new Timer(16, e -> {
                for (Particle p : particles) {
                    p.y -= p.speed;
                    p.alpha -= 1;
                    if (p.y < 0 || p.alpha <= 0) p.reset(getWidth(), getHeight());
                }
                scanlineY = (scanlineY + 3) % (getHeight() + 50);
                repaint();
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (bgImage != null) g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
            for (Particle p : particles) {
                if (p.alpha > 0) {
                    g2.setColor(new Color(255, Math.max(0, Math.min(255, p.green)), 0, Math.max(0, Math.min(255, p.alpha))));
                    g2.fillOval((int)p.x, (int)p.y, p.size, p.size);
                }
            }
            g2.dispose();
        }

        class Particle {
            float x, y, speed; int size, alpha, green;
            void reset(int w, int h) { x = rand.nextInt(w > 0 ? w : 1000); y = h + rand.nextInt(100); speed = 1 + rand.nextFloat() * 2; size = 2 + rand.nextInt(4); alpha = 150 + rand.nextInt(105); green = rand.nextInt(120); }
        }
    }
}
