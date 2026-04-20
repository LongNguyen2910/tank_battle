package entity;

import main.GamePanel;

import java.awt.*;

public class SmokeParticle extends GameObject {
    GamePanel gp;

    private float alpha = 1.0f;
    private float fadeSpeed = 0.04f;
    public boolean alive = true;
    private int size;

    public SmokeParticle(GamePanel gp, int x, int y, int size) {
        this.gp = gp;
        this.solidArea = new Rectangle( x - size/2, y - size/2, size, size);
        this.size = size;
    }

    public void update() {
        alpha -= fadeSpeed;
        this.solidArea.y -= 1;
        this.solidArea.x += (int)(Math.random() * 3 - 1);
        if (alpha <= 0) {
            alpha = 0;
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;
        java.awt.Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRect(this.solidArea.x, this.solidArea.y, size, size);
        g2.setComposite(originalComposite);
    }
}