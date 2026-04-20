package ui;

import main.Config;
import main.GamePanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;

public class SettingsDialog extends JDialog {
    private JPanel mainPanel;
    private Color accentColor = new Color(0, 255, 200);
    private Color bgColor = new Color(10, 10, 10, 230);
    private GamePanel gp;

    public SettingsDialog(JFrame parent) {
        this(parent, null);
    }

    public SettingsDialog(JFrame parent, GamePanel gp) {
        super(parent, "SETTINGS", true);
        this.gp = gp;
        setUndecorated(true);
        setSize(600, 500);
        setLocationRelativeTo(parent);

        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background with chamfered corners
                int w = getWidth(), h = getHeight(), chamfer = 30;
                GeneralPath shape = new GeneralPath();
                shape.moveTo(chamfer, 0); shape.lineTo(w - chamfer, 0);
                shape.lineTo(w, chamfer); shape.lineTo(w, h - chamfer);
                shape.lineTo(w - chamfer, h); shape.lineTo(chamfer, h);
                shape.lineTo(0, h - chamfer); shape.lineTo(0, chamfer); shape.closePath();
                
                g2.setColor(bgColor);
                g2.fill(shape);
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(shape);
                
                // Title
                g2.setFont(new Font("Monospaced", Font.BOLD, 32));
                g2.drawString("TACTICAL SETTINGS", 40, 50);
                g2.drawLine(40, 65, 560, 65);
                
                g2.dispose();
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setOpaque(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(0, 2, 20, 15));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(80, 50, 50, 50));

        // Gameplay Settings
        addSettingRow(contentPanel, "Max Health", createNumericTextField(Config.MAX_HEALTH, v -> Config.MAX_HEALTH = v));
        addSettingRow(contentPanel, "Max Fuel", createNumericTextField(Config.MAX_FUEL, v -> Config.MAX_FUEL = v));
        
        // Difficulty (Prototype)
        addSettingRow(contentPanel, "Difficulty", createCycleButton(new String[]{"EASY", "NORMAL", "HARD", "EXTREME"}, Config.DIFFICULTY, s -> Config.DIFFICULTY = s));
        
        // Match Time (Prototype)
        addSettingRow(contentPanel, "Match Time (s)", createNumericTextField(Config.MATCH_TIME, v -> Config.MATCH_TIME = v));

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));

        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftButtonPanel.setOpaque(false);
        
        JButton keybindsBtn = createStyledButton("KEYBINDS");
        keybindsBtn.addActionListener(e -> {
            new KeybindsDialog(this, gp).setVisible(true);
        });
        leftButtonPanel.add(keybindsBtn);

        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightButtonPanel.setOpaque(false);

        JButton saveBtn = createStyledButton("SAVE & APPLY");
        saveBtn.addActionListener(e -> dispose());
        
        JButton cancelBtn = createStyledButton("CANCEL");
        cancelBtn.addActionListener(e -> dispose());

        rightButtonPanel.add(cancelBtn);
        rightButtonPanel.add(saveBtn);
        
        buttonPanel.add(leftButtonPanel, BorderLayout.WEST);
        buttonPanel.add(rightButtonPanel, BorderLayout.EAST);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void addSettingRow(JPanel panel, String labelText, JComponent component) {
        JLabel label = new JLabel(labelText.toUpperCase());
        label.setFont(new Font("Monospaced", Font.BOLD, 18));
        label.setForeground(Color.WHITE);
        panel.add(label);
        panel.add(component);
    }

    private JTextField createNumericTextField(int current, java.util.function.Consumer<Integer> onUpdate) {
        JTextField textField = new JTextField(String.valueOf(current)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 30, 30));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(accentColor);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        textField.setFont(new Font("Monospaced", Font.BOLD, 16));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        textField.setOpaque(false);
        textField.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        textField.setHorizontalAlignment(JTextField.CENTER);

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    String text = textField.getText();
                    if (!text.isEmpty()) {
                        int val = Integer.parseInt(text);
                        onUpdate.accept(val);
                    }
                } catch (NumberFormatException ex) {
                    // Ignore non-numeric input
                }
            }
        });
        
        // Only allow numbers
        ((javax.swing.text.AbstractDocument) textField.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                if (text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        return textField;
    }

    private JButton createCycleButton(String[] options, String current, java.util.function.Consumer<String> onUpdate) {
        JButton btn = createStyledButton(current);
        btn.addActionListener(e -> {
            int nextIdx = 0;
            for(int i=0; i<options.length; i++) {
                if(options[i].equals(btn.getText())) {
                    nextIdx = (i + 1) % options.length;
                    break;
                }
            }
            String next = options[nextIdx];
            btn.setText(next);
            onUpdate.accept(next);
        });
        return btn;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) g2.setColor(accentColor.darker());
                else if (getModel().isRollover()) g2.setColor(new Color(50, 50, 50));
                else g2.setColor(new Color(30, 30, 30));
                
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(accentColor);
                g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Monospaced", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return btn;
    }
}
