package entity;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class Bullet extends GameObject {
    private enum BulletState {
        FLYING,
        IMPACT,
        DEAD
    }

    private static final int HIT_EFFECT_SPRITE_SIZE = 48;
    private static final int HIT_TICKS_PER_FRAME = 3;
    private static final int BULLET_SPRITE_SIZE = 16;

    private final GamePanel gp;
    private BulletState state = BulletState.FLYING;
    private final int damage;
    private final BufferedImage[] bulletImg;
    private BufferedImage[] impactFrames = new BufferedImage[0];
    private final BulletType bulletType;

    public final int bulletScale;
    private int impactTick = 0;
    private int impactCenterX;
    private int impactCenterY;

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
            bulletImg[0] = img.getSubimage(0, 0, BULLET_SPRITE_SIZE, BULLET_SPRITE_SIZE);
            bulletImg[1] = img.getSubimage(0, 16, BULLET_SPRITE_SIZE, BULLET_SPRITE_SIZE);
            bulletImg[2] = img.getSubimage(0, 32, BULLET_SPRITE_SIZE, BULLET_SPRITE_SIZE);
            bulletImg[3] = img.getSubimage(0, 48, BULLET_SPRITE_SIZE, BULLET_SPRITE_SIZE);
            impactFrames = loadEffectFrames("/tanks/bullet_hit.png", HIT_EFFECT_SPRITE_SIZE);
        } catch (IOException e) {
            System.out.println("Error loading bullet's sprite" + e.getMessage());
        }
    }

    private BufferedImage[] loadEffectFrames(String resourcePath, int sizeSprite) throws IOException {
        BufferedImage spriteSheet = loadImage(resourcePath);
        int cols = Math.max(1, spriteSheet.getWidth() / sizeSprite);
        int rows = Math.max(1, spriteSheet.getHeight() / sizeSprite);
        BufferedImage[] frames = new BufferedImage[cols * rows];
        int frameIndex = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * sizeSprite;
                int y = row * sizeSprite;
                if (x + sizeSprite <= spriteSheet.getWidth() && y + sizeSprite <= spriteSheet.getHeight()) {
                    frames[frameIndex++] = spriteSheet.getSubimage(x, y, sizeSprite, sizeSprite);
                }
            }
        }

        if (frameIndex == frames.length) {
            return frames;
        }

        BufferedImage[] normalizedFrames = new BufferedImage[frameIndex];
        System.arraycopy(frames, 0, normalizedFrames, 0, frameIndex);
        return normalizedFrames;
    }

    private BufferedImage loadImage(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Missing resource: " + resourcePath);
        }
        return ImageIO.read(inputStream);
    }

    public void update() {
        if (state == BulletState.DEAD) {
            return;
        }

        if (state == BulletState.IMPACT) {
            impactTick++;
            if (impactTick >= getTotalImpactTicks()) {
                state = BulletState.DEAD;
            }
            return;
        }

        collisionOn = false;
        gp.getCollisionChecker().checkTile(this);

        if (collisionOn) {
            startImpact();
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

    private int getTotalImpactTicks() {
        return Math.max(1, impactFrames.length) * HIT_TICKS_PER_FRAME;
    }

    private void cacheImpactCenter() {
        impactCenterX = solidArea.x + solidArea.width / 2;
        impactCenterY = solidArea.y + solidArea.height / 2;
    }

    public void startImpact() {
        if (state != BulletState.FLYING) {
            return;
        }

        cacheImpactCenter();
        impactTick = 0;
        state = BulletState.IMPACT;
    }

    public boolean canDamage() {
        return state == BulletState.FLYING;
    }

    public void draw(Graphics2D g2) {
        if (state == BulletState.DEAD) {
            return;
        }

        if (state == BulletState.IMPACT) {
            drawImpact(g2);
            return;
        }

        g2.setColor(Color.YELLOW);
        switch (direction) {
            case UP:
                g2.drawImage(bulletImg[0], solidArea.x - bulletType.getyOffset() * bulletScale, solidArea.y - bulletType.getxOffset() * bulletScale,
                        BULLET_SPRITE_SIZE * bulletScale, BULLET_SPRITE_SIZE * bulletScale, null);
                break;
            case RIGHT:
                g2.drawImage(bulletImg[1], solidArea.x - bulletType.getxOffset() * bulletScale, solidArea.y - bulletType.getyOffset() * bulletScale,
                        BULLET_SPRITE_SIZE * bulletScale, BULLET_SPRITE_SIZE * bulletScale, null);
                break;
            case DOWN:
                g2.drawImage(bulletImg[2], solidArea.x - bulletType.getyOffset() * bulletScale, solidArea.y - bulletType.getxOffset() * bulletScale,
                        BULLET_SPRITE_SIZE * bulletScale, BULLET_SPRITE_SIZE * bulletScale, null);
                break;
            case LEFT:
                g2.drawImage(bulletImg[3], solidArea.x - bulletType.getxOffset() * bulletScale, solidArea.y - bulletType.getyOffset() * bulletScale,
                        BULLET_SPRITE_SIZE * bulletScale, BULLET_SPRITE_SIZE * bulletScale, null);
                break;
        }
        g2.setColor(Color.RED);
        g2.draw(solidArea);
    }

    private void drawImpact(Graphics2D g2) {
        if (impactFrames.length == 0) {
            return;
        }

        int frame = Math.min(impactFrames.length - 1, impactTick / HIT_TICKS_PER_FRAME);
        BufferedImage effect = impactFrames[frame];
        if (effect == null) {
            return;
        }

        int drawSize = HIT_EFFECT_SPRITE_SIZE * gp.scale;
        int drawX = switch (direction) {
            case RIGHT ->  impactCenterX - drawSize / 2 + 10;
            case LEFT ->  impactCenterX - drawSize / 2 - 10;
            default ->  impactCenterX - drawSize / 2;
        };
        int drawY = switch (direction) {
            case UP ->  impactCenterY - drawSize / 2 - 15;
            case DOWN ->  impactCenterY - drawSize / 2 + 10;
            default ->  impactCenterY - drawSize / 2;
        };
        if (direction == Direction.UP || direction == Direction.DOWN) {

            java.awt.geom.AffineTransform oldTransform = g2.getTransform();

            int pivotX = drawX + drawSize / 2;
            int pivotY = drawY + drawSize / 2;

            g2.rotate(Math.toRadians(90), pivotX, pivotY);

            g2.drawImage(effect, drawX, drawY, drawSize, drawSize, null);

            g2.setTransform(oldTransform);
        } else {
            g2.drawImage(effect, drawX, drawY, drawSize, drawSize, null);
        }
    }

    public boolean isAlive() {
        return state != BulletState.DEAD;
    }

    public void setAlive(boolean alive) {
        if (!alive) {
            startImpact();
        }
    }

    public void destroyImmediately() {
        state = BulletState.DEAD;
    }

    public int getDamage() {
        return damage;
    }
}
