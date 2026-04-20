package ui;

import main.GameWindow;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

public class startingscreen extends JFrame {

    private List<AnimatedTacticalButton> menuButtons = new ArrayList<>();
    private int currentIndex = 0;

    public startingscreen() {
        setTitle("Tank battle");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        AnimatedBackgroundPanel mainPanel = new AnimatedBackgroundPanel("/ui/tank.jpg");
        mainPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;

        JComponent titleComponent = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                String text = "TANK BATTLE";
                g2.setFont(new Font("Impact", Font.BOLD, 100));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = fm.getAscent() + 20;
                double time = System.currentTimeMillis() / 300.0;
                float pulse = (float) ((Math.sin(time) + 1.0) / 2.0);
                int alpha = (int) (100 + (pulse * 155));

                g2.setColor(new Color(255, 69, 0, alpha));
                for (int i = 8; i > 0; i -= 2) {
                    g2.drawString(text, x - i, y - i);
                    g2.drawString(text, x + i, y + i);
                }

                g2.setColor(Color.WHITE);
                g2.drawString(text, x, y);

                g2.setFont(new Font("Monospaced", Font.BOLD, 16));
                String sub = "USE [UP] [DOWN] ARROWS TO NAVIGATE  //  PRESS [ENTER] TO SELECT";
                g2.setColor(new Color(0, 255, 200, 200));
                g2.drawString(sub, (getWidth() - g2.getFontMetrics().stringWidth(sub))/2, y + 30);

                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(800, 180); }
        };
        gbc.insets = new Insets(0, 0, 60, 0);
        mainPanel.add(titleComponent, gbc);

        String[] buttonNames = {"Start", "Setting", "Quit Game"};
        gbc.insets = new Insets(15, 0, 15, 0);

        for (String name : buttonNames) {
            AnimatedTacticalButton button = new AnimatedTacticalButton(name);
            menuButtons.add(button);
            mainPanel.add(button, gbc);
        }

        menuButtons.get(0).setTargetFocus(true);

        setupKeyBindings(mainPanel);

        add(mainPanel);
    }

    private void setupKeyBindings(JPanel panel) {
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();

        im.put(KeyStroke.getKeyStroke("UP"), "moveUp");
        im.put(KeyStroke.getKeyStroke("DOWN"), "moveDown");
        im.put(KeyStroke.getKeyStroke("ENTER"), "select");

        am.put("moveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menuButtons.get(currentIndex).setTargetFocus(false);
                currentIndex = (currentIndex - 1 + menuButtons.size()) % menuButtons.size();
                menuButtons.get(currentIndex).setTargetFocus(true);
            }
        });

        am.put("moveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menuButtons.get(currentIndex).setTargetFocus(false);
                currentIndex = (currentIndex + 1) % menuButtons.size();
                menuButtons.get(currentIndex).setTargetFocus(true);
            }
        });

        am.put("select", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedName = menuButtons.get(currentIndex).getText();
                if (selectedName.equals("Quit Game")) {
                    System.exit(0);
                } else if (selectedName.equals("Start")) {
                    dispose(); // Close menu
                    new GameWindow(); // Start game
                } else {
                    JOptionPane.showMessageDialog(startingscreen.this, "Vừa ấn: " + selectedName);
                }
            }
        });
    }

    class AnimatedTacticalButton extends JButton {
        private float hoverProgress = 0f;
        private boolean isTargetFocused = false;
        private Timer animTimer;

        public AnimatedTacticalButton(String text) {
            super(text);
            setFont(new Font("Monospaced", Font.BOLD, 24));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(350, 65));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            animTimer = new Timer(16, e -> {
                if (isTargetFocused && hoverProgress < 1f) {
                    hoverProgress += 0.1f;
                    if (hoverProgress > 1f) hoverProgress = 1f;
                    repaint();
                } else if (!isTargetFocused && hoverProgress > 0f) {
                    hoverProgress -= 0.1f;
                    if (hoverProgress < 0f) hoverProgress = 0f;
                    repaint();
                } else {
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

            g2.setColor(new Color(0, 255, 200, (int)(255 * hoverProgress)));
            g2.fillRect(5, h/2 - 10, 4, 20);
            g2.fillRect(w - 9, h/2 - 10, 4, 20);

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
                } else {
                    System.err.println("Không tìm thấy ảnh: " + imagePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            startingscreen menu = new startingscreen();
            menu.setVisible(true);
        });
    }
}
