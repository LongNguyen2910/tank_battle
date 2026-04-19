package entity;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Bullet extends GameObject {
    private final GamePanel gp;
    private boolean alive = true;
    private final int damage;
    BufferedImage[] bulletImg;
    BulletType bulletType;

    public final int bulletScale;

    public Bullet(GamePanel gp, int startX, int startY, Direction direction, TankType type) {
        this.gp = gp;
        bulletType = type.getBulletType();
        bulletScale = gp.scale + 1;
        solidArea = new Rectangle(startX, startY, bulletType.getWidth() * bulletScale, bulletType.getHeight() * bulletScale);
        this.direction = direction;
        this.speed = bulletType.getBulletSpeed();
        this.damage = bulletType.getDamage();
        bulletImg = new BufferedImage[4];
        loadBullet();
    }

    public void loadBullet() {
        BufferedImage img;
        try {
            img = loadImage(bulletType.getFilePath());
            bulletImg[0] = img.getSubimage(0, 0, 16, 16);
            bulletImg[1] = img.getSubimage(0, 16, 16, 16);
            bulletImg[2] = img.getSubimage(0, 32, 16, 16);
            bulletImg[3] = img.getSubimage(0, 48, 16, 16);
        } catch (IOException e) {
            System.out.println("Error loading bullet's sprite" + e.getMessage());
        }
    }

    private BufferedImage loadImage(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Missing resource: " + resourcePath);
        }
        return ImageIO.read(inputStream);
    }

    public void update() {
        collisionOn = false;
        gp.getCollisionChecker().checkTile(this);

        if (collisionOn) {
            alive = false;
        } else {
            switch (direction) {
                case UP:
                    solidArea.y -= speed;
                    solidArea.width = bulletType.getHeight() * bulletScale;
                    solidArea.height = bulletType.getWidth() * bulletScale;
                    break;
                case DOWN:
                    solidArea.y += speed;
                    solidArea.width = bulletType.getHeight() * bulletScale;
                    solidArea.height = bulletType.getWidth() * bulletScale;
                    break;
                case LEFT:
                    solidArea.x -= speed;
                    solidArea.width = bulletType.getWidth() * bulletScale;
                    solidArea.height = bulletType.getHeight() * bulletScale;
                    break;
                case RIGHT:
                    solidArea.x += speed;
                    solidArea.width = bulletType.getWidth() * bulletScale;
                    solidArea.height = bulletType.getHeight() * bulletScale;
                    break;
                case NONE: break;
            }
        }
    }

    public void draw(Graphics2D g2) {
        g2.setColor(Color.YELLOW);
        switch (direction) {
            case UP:
                g2.drawImage(bulletImg[0], solidArea.x - bulletType.getyOffset() * bulletScale, solidArea.y - bulletType.getxOffset() * bulletScale,
                        16 * bulletScale, 16 * bulletScale, null);
                break;
            case RIGHT:
                g2.drawImage(bulletImg[1], solidArea.x - bulletType.getxOffset() * bulletScale, solidArea.y - bulletType.getyOffset() * bulletScale,
                        16 * bulletScale, 16 * bulletScale, null);
                break;
            case DOWN:
                g2.drawImage(bulletImg[2], solidArea.x - bulletType.getyOffset() * bulletScale, solidArea.y - bulletType.getxOffset() * bulletScale,
                        16 * bulletScale, 16 * bulletScale, null);
                break;
            case LEFT:
                g2.drawImage(bulletImg[3], solidArea.x - bulletType.getxOffset() * bulletScale, solidArea.y - bulletType.getyOffset() * bulletScale,
                        16 * bulletScale, 16 * bulletScale, null);
                break;
        }
        g2.setColor(Color.RED);
        g2.draw(solidArea);
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public int getDamage() {
        return damage;
    }
}
