package ui;

import main.Config;
import main.GameConfig;
import main.GameWindow;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

public class LobbyScreen extends JFrame {
    private GameConfig config = new GameConfig();
    private JLabel playerCountLabel;
    private JLabel computerCountLabel;
    private JLabel mapLabel;
    private JLabel modeLabel;
    
    private String[] maps = {"/maps/map01.txt", "/maps/map02.txt", "/maps/map03.txt", "/maps/map04.txt"};
    private String[] mapNames = {"Classic Border", "Maze Runner", "Crossroads", "The Arena"};
    private int currentMapIndex = 0;
    
    private String[] modes = {"Deathmatch", "Capture the Flag", "Survival"};
    private int currentModeIndex = 0;

    // Map Preview logic
    private BufferedImage generateMapPreview(String path) {
        int previewScale = 10; // Kích thước mỗi ô trong bản xem trước
        BufferedImage img = new BufferedImage(Config.MAX_SCREEN_COL * previewScale, 
                                              Config.MAX_SCREEN_ROW * previewScale, 
                                              BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        
        // Nền tối cho preview
        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(0, 0, img.getWidth(), img.getHeight());

        try {
            java.io.InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
                for (int row = 0; row < Config.MAX_SCREEN_ROW; row++) {
                    String line = br.readLine();
                    if (line == null) break;
                    String[] numbers = line.split("\\s+");
                    for (int col = 0; col < Config.MAX_SCREEN_COL && col < numbers.length; col++) {
                        int num = Integer.parseInt(numbers[col]);
                        if (num == 1) { // Tường cứng
                            g2.setColor(new Color(100, 100, 100)); 
                            g2.fillRect(col * previewScale, row * previewScale, previewScale - 1, previewScale - 1);
                        } else if (num == 2) { // Tường phá được
                            g2.setColor(new Color(139, 69, 19)); 
                            g2.fillRect(col * previewScale, row * previewScale, previewScale - 1, previewScale - 1);
                        } else if (num == 3) { // Nước
                            g2.setColor(new Color(30, 144, 255));
                            g2.fillRect(col * previewScale, row * previewScale, previewScale, previewScale);
                        }
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        g2.dispose();
        return img;
    }

    public LobbyScreen() {
        setTitle("Game Configuration");
        setSize(Config.MENU_WIDTH, Config.MENU_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        AnimatedBackgroundPanel mainPanel = new AnimatedBackgroundPanel("/ui/tank.jpg");
        mainPanel.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 50, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // LEFT SIDE: SETTINGS
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Player Selection
        gbc.gridx = 0; gbc.gridy = 0;
        leftPanel.add(createLabel("PLAYERS:"), gbc);
        gbc.gridx = 1;
        JPanel playerPanel = createSelectionPanel(
            e -> { if(config.playerCount > 1) config.playerCount--; updateUI(); },
            e -> { if(config.playerCount < 2) config.playerCount++; updateUI(); }
        );
        playerCountLabel = createValueLabel(String.valueOf(config.playerCount));
        playerPanel.add(playerCountLabel, BorderLayout.CENTER);
        leftPanel.add(playerPanel, gbc);

        // Computer Selection
        gbc.gridx = 0; gbc.gridy = 1;
        leftPanel.add(createLabel("COMPUTERS:"), gbc);
        gbc.gridx = 1;
        JPanel cpuPanel = createSelectionPanel(
            e -> { if(config.computerCount > 0) config.computerCount--; updateUI(); },
            e -> { if(config.computerCount < 2) config.computerCount++; updateUI(); }
        );
        computerCountLabel = createValueLabel(String.valueOf(config.computerCount));
        cpuPanel.add(computerCountLabel, BorderLayout.CENTER);
        leftPanel.add(cpuPanel, gbc);

        // Mode Selection
        gbc.gridx = 0; gbc.gridy = 2;
        leftPanel.add(createLabel("MODE:"), gbc);
        gbc.gridx = 1;
        JPanel modePanel = createSelectionPanel(
            e -> { currentModeIndex = (currentModeIndex - 1 + modes.length) % modes.length; config.gameMode = modes[currentModeIndex]; updateUI(); },
            e -> { currentModeIndex = (currentModeIndex + 1) % modes.length; config.gameMode = modes[currentModeIndex]; updateUI(); }
        );
        modeLabel = createValueLabel(modes[currentModeIndex]);
        modePanel.add(modeLabel, BorderLayout.CENTER);
        leftPanel.add(modePanel, gbc);

        contentPanel.add(leftPanel);

        // RIGHT SIDE: MAP SELECTION
        JPanel rightPanel = new JPanel(new BorderLayout(0, 20));
        rightPanel.setOpaque(false);
        
        rightPanel.add(createLabel("SELECT MAP:"), BorderLayout.NORTH);
        
        // Map Preview Display
        JPanel mapPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage preview = generateMapPreview(maps[currentMapIndex]);
                
                // Vẽ khung và ảnh preview
                int x = (getWidth() - preview.getWidth()) / 2;
                int y = (getHeight() - preview.getHeight()) / 2;
                
                g.setColor(new Color(0, 255, 200, 30));
                g.fillRect(x - 5, y - 5, preview.getWidth() + 10, preview.getHeight() + 10);
                
                g.drawImage(preview, x, y, null);
                
                g.setColor(new Color(0, 255, 200));
                g.drawRect(x - 1, y - 1, preview.getWidth() + 1, preview.getHeight() + 1);
                
                // Tên map ở dưới ảnh
                g.setFont(new Font("Monospaced", Font.BOLD, 18));
                String text = mapNames[currentMapIndex];
                int textX = (getWidth() - g.getFontMetrics().stringWidth(text)) / 2;
                g.drawString(text, textX, y + preview.getHeight() + 25);
            }
        };
        mapPreview.setPreferredSize(new Dimension(300, 250));
        mapPreview.setOpaque(false);
        rightPanel.add(mapPreview, BorderLayout.CENTER);

        JPanel mapSelectControl = createSelectionPanel(
            e -> { currentMapIndex = (currentMapIndex - 1 + maps.length) % maps.length; config.mapPath = maps[currentMapIndex]; updateUI(); mapPreview.repaint(); },
            e -> { currentMapIndex = (currentMapIndex + 1) % maps.length; config.mapPath = maps[currentMapIndex]; updateUI(); mapPreview.repaint(); }
        );
        mapLabel = createValueLabel(mapNames[currentMapIndex]);
        mapSelectControl.add(mapLabel, BorderLayout.CENTER);
        rightPanel.add(mapSelectControl, BorderLayout.SOUTH);

        contentPanel.add(rightPanel);

        // Header
        JLabel titleLabel = new JLabel("MATCH SETTING", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.BOLD, 60));
        titleLabel.setForeground(new Color(255, 69, 0));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Footer Buttons
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 30));
        footerPanel.setOpaque(false);
        
        AnimatedTacticalButton backBtn = new AnimatedTacticalButton("BACK");
        backBtn.addActionListener(e -> {
            dispose();
            new startingscreen().setVisible(true);
        });
        footerPanel.add(backBtn);

        AnimatedTacticalButton startBtn = new AnimatedTacticalButton("START GAME");
        startBtn.addActionListener(e -> {
            dispose();
            // mark config so the created GameWindow starts immediately without showing main menu
            config.startImmediately = true;
            new GameWindow(config);
        });
        footerPanel.add(startBtn);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Monospaced", Font.BOLD, 24));
        label.setForeground(new Color(0, 255, 200));
        return label;
    }

    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Monospaced", Font.BOLD, 24));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JPanel createSelectionPanel(ActionListener leftAction, ActionListener rightAction) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(300, 50));

        JButton leftBtn = new JButton("<");
        styleArrowButton(leftBtn);
        leftBtn.addActionListener(leftAction);

        JButton rightBtn = new JButton(">");
        styleArrowButton(rightBtn);
        rightBtn.addActionListener(rightAction);

        panel.add(leftBtn, BorderLayout.WEST);
        panel.add(rightBtn, BorderLayout.EAST);
        
        return panel;
    }

    private void styleArrowButton(JButton btn) {
        btn.setFont(new Font("Monospaced", Font.BOLD, 24));
        btn.setForeground(new Color(255, 102, 0));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void updateUI() {
        playerCountLabel.setText(String.valueOf(config.playerCount));
        computerCountLabel.setText(String.valueOf(config.computerCount));
        mapLabel.setText(mapNames[currentMapIndex]);
        modeLabel.setText(modes[currentModeIndex]);
    }

    // Copied from startingscreen for consistency
    class AnimatedTacticalButton extends JButton {
        private float hoverProgress = 0f;
        private boolean isTargetFocused = false;
        private Timer animTimer;

        public AnimatedTacticalButton(String text) {
            super(text);
            setFont(new Font("Monospaced", Font.BOLD, 24));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(300, 60));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { setTargetFocus(true); }
                @Override
                public void mouseExited(MouseEvent e) { setTargetFocus(false); }
            });

            animTimer = new Timer(16, e -> {
                if (isTargetFocused && hoverProgress < 1f) {
                    hoverProgress += 0.1f;
                    if (hoverProgress > 1f) hoverProgress = 1f;
                    repaint();
                } else if (!isTargetFocused && hoverProgress > 0f) {
                    hoverProgress -= 0.1f;
                    if (hoverProgress < 0f) hoverProgress = 0f;
                    repaint();
                } else if (hoverProgress <= 0f && !isTargetFocused) {
                    animTimer.stop();
                }
            });
        }

        public void setTargetFocus(boolean focused) {
            this.isTargetFocused = focused;
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), chamfer = 20;

            GeneralPath shape = new GeneralPath();
            shape.moveTo(chamfer, 0); shape.lineTo(w - chamfer, 0);
            shape.lineTo(w, chamfer); shape.lineTo(w, h - chamfer);
            shape.lineTo(w - chamfer, h); shape.lineTo(chamfer, h);
            shape.lineTo(0, h - chamfer); shape.lineTo(0, chamfer); shape.closePath();

            int r = (int) (40 + (255 - 40) * hoverProgress);
            int gr = (int) (40 + (102 - 40) * hoverProgress);
            int b = (int) (40 + (0 - 40) * hoverProgress);

            g2.setPaint(new GradientPaint(0, 0, new Color(r, gr, b, 200), 0, h, new Color(10, 10, 10, 240)));
            g2.fill(shape);

            if (hoverProgress > 0) {
                g2.setColor(new Color(255, 102, 0, (int)(255 * hoverProgress)));
                g2.setStroke(new BasicStroke(3f + (3f * hoverProgress)));
                g2.draw(shape);
            }

            g2.setColor(new Color(255, 255, 255, (int)(100 + 155 * hoverProgress)));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(shape);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    class AnimatedBackgroundPanel extends JPanel {
        private Image bgImage;
        private Timer engineTimer;
        private List<Particle> particles = new ArrayList<>();
        private Random rand = new Random();
        private int scanlineY = 0;

        public AnimatedBackgroundPanel(String imagePath) {
            try {
                java.io.InputStream is = getClass().getResourceAsStream(imagePath);
                if (is != null) {
                    bgImage = ImageIO.read(is);
                }
            } catch (IOException e) { e.printStackTrace(); }

            for (int i = 0; i < 40; i++) particles.add(new Particle());

            engineTimer = new Timer(16, e -> {
                for (Particle p : particles) {
                    p.y -= p.speed;
                    p.x += Math.sin(p.y / 20.0) * 1.5;
                    p.alpha -= 1;
                    if (p.y < 0 || p.alpha <= 0) p.reset(getWidth(), getHeight());
                }
                scanlineY += 3;
                if (scanlineY > getHeight()) scanlineY = -50;
                repaint();
            });
            engineTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (bgImage != null) g2.drawImage(bgImage, 0, 0, w, h, this);

            RadialGradientPaint vignette = new RadialGradientPaint(
                    w/2f, h/2f, w/1.2f,
                    new float[]{0.0f, 1.0f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 220)}
            );
            g2.setPaint(vignette);
            g2.fillRect(0, 0, w, h);

            for (Particle p : particles) {
                if (p.alpha > 0) {
                    g2.setColor(new Color(255, p.green, 0, p.alpha));
                    g2.fillOval((int)p.x, (int)p.y, p.size, p.size);
                }
            }

            g2.setColor(new Color(255, 255, 255, 15));
            for (int i = 0; i < w; i += 40) g2.drawLine(i, 0, i, h);
            for (int i = 0; i < h; i += 40) g2.drawLine(0, i, w, i);

            g2.setPaint(new GradientPaint(0, scanlineY, new Color(0, 255, 200, 0), 0, scanlineY + 50, new Color(0, 255, 200, 30)));
            g2.fillRect(0, scanlineY, w, 50);
            g2.setColor(new Color(0, 255, 200, 80));
            g2.drawLine(0, scanlineY + 50, w, scanlineY + 50);

            g2.dispose();
        }

        class Particle {
            float x, y, speed;
            int size, alpha, green;
            void reset(int w, int h) {
                x = rand.nextInt(w > 0 ? w : 1000);
                y = h + rand.nextInt(100);
                speed = 1f + rand.nextFloat() * 2f;
                size = 2 + rand.nextInt(4);
                alpha = 150 + rand.nextInt(105);
                green = rand.nextInt(120);
            }
        }
    }
}
