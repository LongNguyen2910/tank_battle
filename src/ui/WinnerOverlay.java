package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;

public class WinnerOverlay extends JComponent {

    public interface WinnerListener {
        void onPlayAgain();
        void onMenu();
    }

    private WinnerListener listener;

    private final AnimatedTacticalButton playAgainBtn = new AnimatedTacticalButton("Play Again");
    private final AnimatedTacticalButton menuBtn = new AnimatedTacticalButton("Menu");
    private final AnimatedTacticalButton[] buttons = new AnimatedTacticalButton[]{playAgainBtn, menuBtn};
    private int currentIndex = 0;

    private String winnerText = "PLAYER 1 WIN";

    public WinnerOverlay() {
        setOpaque(false);
        setLayout(null);

        for (AnimatedTacticalButton b : buttons) {
            add(b);
        }

        playAgainBtn.addActionListener(e -> {
            if (listener != null) listener.onPlayAgain();
        });
        menuBtn.addActionListener(e -> {
            if (listener != null) listener.onMenu();
        });

        buttons[0].setTargetFocus(true);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutButtons();
            }
        });

        setupKeyBindings();
        setupMouseFocus();

        setFocusable(true);
        setVisible(false);
    }

    public void setWinnerListener(WinnerListener l) {
        this.listener = l;
    }

    public void setWinnerText(String text) {
        this.winnerText = (text == null || text.isBlank()) ? "WIN" : text;
        repaint();
    }

    public void runWinner(boolean show) {
        setVisible(show);
        if (show) requestFocusInWindow();
        repaint();
    }

    public void layoutButtons() {
        int w = getWidth();
        int h = getHeight();

        int boxW = Math.min(520, (int) (w * 0.62));
        int boxH = Math.min(320, (int) (h * 0.52));
        int boxX = (w - boxW) / 2;
        int boxY = (h - boxH) / 2;

        int btnW = Math.min(260, boxW - 80);
        int btnH = 55;
        int btnX = boxX + (boxW - btnW) / 2;
        int gap = 18;
        int totalBtnsH = btnH * 2 + gap;
        int btnY0 = boxY + boxH - 35 - totalBtnsH;

        playAgainBtn.setBounds(btnX, btnY0, btnW, btnH);
        menuBtn.setBounds(btnX, btnY0 + btnH + gap, btnW, btnH);
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("UP"), "moveUp");
        im.put(KeyStroke.getKeyStroke("DOWN"), "moveDown");
        im.put(KeyStroke.getKeyStroke("ENTER"), "select");

        am.put("moveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttons[currentIndex].setTargetFocus(false);
                currentIndex = (currentIndex - 1 + buttons.length) % buttons.length;
                buttons[currentIndex].setTargetFocus(true);
            }
        });

        am.put("moveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttons[currentIndex].setTargetFocus(false);
                currentIndex = (currentIndex + 1) % buttons.length;
                buttons[currentIndex].setTargetFocus(true);
            }
        });

        am.put("select", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttons[currentIndex].doClick();
            }
        });
    }

    private void setupMouseFocus() {
        for (int i = 0; i < buttons.length; i++) {
            final int idx = i;
            buttons[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (currentIndex != idx) {
                        buttons[currentIndex].setTargetFocus(false);
                        currentIndex = idx;
                        buttons[currentIndex].setTargetFocus(true);
                    }
                }
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isVisible()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, w, h);

        int boxW = Math.min(520, (int) (w * 0.62));
        int boxH = Math.min(320, (int) (h * 0.52));
        int boxX = (w - boxW) / 2;
        int boxY = (h - boxH) / 2;
        int arc = 26;

        g2.setPaint(new GradientPaint(0, boxY, new Color(10, 10, 10, 235), 0, boxY + boxH, new Color(30, 30, 30, 235)));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, arc, arc);

        g2.setColor(new Color(0, 255, 200, 170));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, arc, arc);

        g2.setFont(new Font("Impact", Font.BOLD, Math.max(40, Math.min(64, boxW / 9))));
        FontMetrics fm = g2.getFontMetrics();
        int tx = boxX + (boxW - fm.stringWidth(winnerText)) / 2;
        int ty = boxY + 75;

        double time = System.currentTimeMillis() / 260.0;
        float pulse = (float) ((Math.sin(time) + 1.0) / 2.0);
        int alpha = (int) (120 + (pulse * 135));

        g2.setColor(new Color(255, 69, 0, alpha));
        for (int i = 6; i > 0; i -= 2) {
            g2.drawString(winnerText, tx - i, ty - i);
            g2.drawString(winnerText, tx + i, ty + i);
        }
        g2.setColor(Color.WHITE);
        g2.drawString(winnerText, tx, ty);

        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(new Color(0, 255, 200, 190));
        String hint = "[UP]/[DOWN]  SELECT  //  [ENTER]  CONFIRM";
        int hx = boxX + (boxW - g2.getFontMetrics().stringWidth(hint)) / 2;
        g2.drawString(hint, hx, ty + 28);

        g2.dispose();
    }

    class AnimatedTacticalButton extends JButton {
        private float hoverProgress = 0f;
        private boolean isTargetFocused = false;
        private final Timer animTimer;

        public AnimatedTacticalButton(String text) {
            super(text);
            setFont(new Font("Monospaced", Font.BOLD, 20));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            animTimer = new Timer(16, null);
            animTimer.addActionListener(e -> {
                if (isTargetFocused && hoverProgress < 1f) {
                    hoverProgress = Math.min(1f, hoverProgress + 0.1f);
                    repaint();
                } else if (!isTargetFocused && hoverProgress > 0f) {
                    hoverProgress = Math.max(0f, hoverProgress - 0.1f);
                    repaint();
                } else {
                    animTimer.stop();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setTargetFocus(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setTargetFocus(false);
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
            int w = getWidth(), h = getHeight(), chamfer = 18;

            GeneralPath shape = new GeneralPath();
            shape.moveTo(chamfer, 0);
            shape.lineTo(w - chamfer, 0);
            shape.lineTo(w, chamfer);
            shape.lineTo(w, h - chamfer);
            shape.lineTo(w - chamfer, h);
            shape.lineTo(chamfer, h);
            shape.lineTo(0, h - chamfer);
            shape.lineTo(0, chamfer);
            shape.closePath();

            int r = Math.max(0, Math.min(255, (int) (40 + (255 - 40) * hoverProgress)));
            int gr = Math.max(0, Math.min(255, (int) (40 + (102 - 40) * hoverProgress)));
            int b = Math.max(0, Math.min(255, (int) (40 - 40 * hoverProgress)));

            g2.setPaint(new GradientPaint(0, 0, new Color(r, gr, b, 200), 0, h, new Color(10, 10, 10, 240)));
            g2.fill(shape);

            if (hoverProgress > 0) {
                g2.setColor(new Color(255, 102, 0, Math.max(0, Math.min(255, (int) (255 * hoverProgress)))));
                g2.setStroke(new BasicStroke(2.8f));
                g2.draw(shape);
            }

            g2.setColor(new Color(255, 255, 255, 120 + (int) (135 * hoverProgress)));
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(shape);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}

