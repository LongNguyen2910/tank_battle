package ui;

import main.Config;
import entity.KeySetting;
import main.GamePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;

public class KeybindsDialog extends JDialog {

    private Color accentColor = new Color(0, 255, 200);
    private Color bgColor = new Color(10, 10, 10, 240);

    private GamePanel gp;

    private JButton bindingButton = null;
    private String currentAction = "";
    private int currentPlayer = 0;

    public KeybindsDialog(JDialog parent, GamePanel gp) {
        super(parent, "KEY BINDINGS", true);
        this.gp = gp;

        setUndecorated(true);
        setSize(800, 600);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight(), chamfer = 30;

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

                g2.setColor(bgColor);
                g2.fill(shape);

                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(shape);

                g2.setFont(new Font("Monospaced", Font.BOLD, 32));
                g2.drawString("KEYBINDS", 40, 50);
                g2.drawLine(40, 65, 760, 65);

                g2.dispose();
            }
        };

        mainPanel.setLayout(new BorderLayout());
        mainPanel.setOpaque(false);

        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 40, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(100, 50, 50, 50));

        contentPanel.add(createPlayerPanel(1));
        contentPanel.add(createPlayerPanel(2));

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        JButton backBtn = createStyledButton("BACK TO SETTINGS");
        backBtn.addActionListener(e -> dispose());
        bottomPanel.add(backBtn);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // ===== FIX FOCUS + KEY LISTENER =====
        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (bindingButton != null) {
                    updateKeyBinding(e.getKeyCode());
                }
            }
        });
    }

    private JPanel createPlayerPanel(int player) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel label = new JLabel("PLAYER " + player);
        label.setFont(new Font("Monospaced", Font.BOLD, 24));
        label.setForeground(accentColor);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(20));

        String[] actions = {"UP", "DOWN", "LEFT", "RIGHT", "SHOOT", "DASH", "SKILL 1", "SKILL 2"};

        for (String action : actions) {
            panel.add(createBindRow(player, action));
            panel.add(Box.createVerticalStrut(10));
        }

        return panel;
    }

    private JPanel createBindRow(int player, String action) {

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(350, 40));

        JLabel actionLabel = new JLabel(action);
        actionLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        actionLabel.setForeground(Color.WHITE);

        row.add(actionLabel, BorderLayout.WEST);

        int currentKey = getCurrentKey(player, action);

        JButton bindBtn = createStyledButton(KeyEvent.getKeyText(currentKey));
        bindBtn.setPreferredSize(new Dimension(150, 30));

        bindBtn.addActionListener(e -> {

            if (bindingButton != null) {
                bindingButton.setText(
                        KeyEvent.getKeyText(getCurrentKey(currentPlayer, currentAction))
                );
            }

            bindingButton = bindBtn;
            currentAction = action;
            currentPlayer = player;

            bindBtn.setText("...");

            requestFocusInWindow(); // FIX FOCUS
        });

        row.add(bindBtn, BorderLayout.EAST);

        return row;
    }

    private int getCurrentKey(int player, String action) {

        if (player == 1) {
            switch (action) {
                case "UP": return Config.P1_UP;
                case "DOWN": return Config.P1_DOWN;
                case "LEFT": return Config.P1_LEFT;
                case "RIGHT": return Config.P1_RIGHT;
                case "SHOOT": return Config.P1_SHOOT;
                case "DASH": return Config.P1_DASH;
                case "SKILL 1": return Config.P1_SKILL1;
                case "SKILL 2": return Config.P1_SKILL2;
            }
        } else {
            switch (action) {
                case "UP": return Config.P2_UP;
                case "DOWN": return Config.P2_DOWN;
                case "LEFT": return Config.P2_LEFT;
                case "RIGHT": return Config.P2_RIGHT;
                case "SHOOT": return Config.P2_SHOOT;
                case "DASH": return Config.P2_DASH;
                case "SKILL 1": return Config.P2_SKILL1;
                case "SKILL 2": return Config.P2_SKILL2;
            }
        }

        return 0;
    }

    private void updateKeyBinding(int keyCode) {

        if (currentPlayer == 1) {
            switch (currentAction) {
                case "UP": Config.P1_UP = keyCode; break;
                case "DOWN": Config.P1_DOWN = keyCode; break;
                case "LEFT": Config.P1_LEFT = keyCode; break;
                case "RIGHT": Config.P1_RIGHT = keyCode; break;
                case "SHOOT": Config.P1_SHOOT = keyCode; break;
                case "DASH": Config.P1_DASH = keyCode; break;
                case "SKILL 1": Config.P1_SKILL1 = keyCode; break;
                case "SKILL 2": Config.P1_SKILL2 = keyCode; break;
            }

            if (gp != null && gp.getKeySettingPlayer1() != null) {
                updateKeySetting(gp.getKeySettingPlayer1(), currentAction, keyCode);
            }

        } else {
            switch (currentAction) {
                case "UP": Config.P2_UP = keyCode; break;
                case "DOWN": Config.P2_DOWN = keyCode; break;
                case "LEFT": Config.P2_LEFT = keyCode; break;
                case "RIGHT": Config.P2_RIGHT = keyCode; break;
                case "SHOOT": Config.P2_SHOOT = keyCode; break;
                case "DASH": Config.P2_DASH = keyCode; break;
                case "SKILL 1": Config.P2_SKILL1 = keyCode; break;
                case "SKILL 2": Config.P2_SKILL2 = keyCode; break;
            }

            if (gp != null && gp.getKeySettingPlayer2() != null) {
                updateKeySetting(gp.getKeySettingPlayer2(), currentAction, keyCode);
            }
        }

        // FIX NULL SAFE
        if (bindingButton != null) {
            bindingButton.setText(KeyEvent.getKeyText(keyCode));
        }

        bindingButton = null;
    }

    private void updateKeySetting(KeySetting ks, String action, int keyCode) {

        switch (action) {
            case "UP": ks.setKeyUp(keyCode); break;
            case "DOWN": ks.setKeyDown(keyCode); break;
            case "LEFT": ks.setKeyLeft(keyCode); break;
            case "RIGHT": ks.setKeyRight(keyCode); break;
            case "SHOOT": ks.setKeyShoot(keyCode); break;
            case "DASH": ks.setKeyDash(keyCode); break;
            case "SKILL 1": ks.setKeySkill1(keyCode); break;
            case "SKILL 2": ks.setKeySkill2(keyCode); break;
        }
    }

    private JButton createStyledButton(String text) {

        JButton btn = new JButton(text) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2.setColor(accentColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(50, 50, 50));
                } else {
                    g2.setColor(new Color(30, 30, 30));
                }

                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(accentColor);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Monospaced", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        return btn;
    }
}