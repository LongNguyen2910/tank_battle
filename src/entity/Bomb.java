package entity;

import main.Config;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class Bomb {
    private enum BombState {
        ARMED,
        EXPLODING,
        EXPIRED
    }

    private static final int ARMED_ALPHA_PERCENT = 20;
    private static final int BOMB_HITBOX_SIZE = 20;
    private static final int BOMB_DRAW_SIZE = Config.ORIGINAL_TILE_SIZE;
    private static final int EXPLOSION_DRAW_SIZE = Config.TILE_SIZE;
    private static final int EXPLOSION_TICKS_PER_FRAME = 4;
    private static final int EXPLOSION_DAMAGE = 40;
    private static final int OWNER_IMMUNITY_TICKS = 20;

    private final Rectangle triggerArea;
    private final BufferedImage[] armedFrames;
    private final BufferedImage[] explosionFrames;
    private final Tank owner;
    private BombState state = BombState.ARMED;
    private int animationTick = 0;
    private int ownerImmunityTicks = OWNER_IMMUNITY_TICKS;
    private int explosionCenterX;
    private int explosionCenterY;

    public Bomb(int centerX, int centerY, Tank owner) {
        int halfSize = BOMB_HITBOX_SIZE / 2;
        this.triggerArea = new Rectangle(centerX - halfSize, centerY - halfSize, BOMB_HITBOX_SIZE, BOMB_HITBOX_SIZE);
        this.explosionCenterX = centerX;
        this.explosionCenterY = centerY;
        this.owner = owner;
        this.armedFrames = loadFrames("/effects/bomb.png", 16, 16);
        this.explosionFrames = loadFrames("/effects/bomb_explosion.png", 16, 16);
    }

    public void update() {
        if (state == BombState.ARMED) {
            animationTick++;
            if (ownerImmunityTicks > 0) {
                ownerImmunityTicks--;
            }
            return;
        }

        if (state == BombState.EXPLODING) {
            animationTick++;
            if (animationTick >= getExplosionDurationTicks()) {
                state = BombState.EXPIRED;
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (state == BombState.EXPIRED) {
            return;
        }

        if (state == BombState.ARMED) {
            drawArmed(g2);
            return;
        }

        drawExplosion(g2);
    }

    public boolean canTrigger() {
        return state == BombState.ARMED;
    }

    public boolean intersects(Rectangle area) {
        return area != null && triggerArea.intersects(area);
    }

    public boolean canBeTriggeredBy(Tank tank) {
        if (!canTrigger() || tank == null) {
            return false;
        }
        // In team modes, prevent teammates from triggering owner's bomb
        if (owner != null && owner.getTeamId() != 0 && owner.getTeamId() == tank.getTeamId()) {
            return false;
        }
        return tank != owner || ownerImmunityTicks <= 0;
    }

    public void trigger(Tank tank) {
        if (!canTrigger()) {
            return;
        }

        explosionCenterX = triggerArea.x + triggerArea.width / 2;
        explosionCenterY = triggerArea.y + triggerArea.height / 2;
        state = BombState.EXPLODING;
        animationTick = 0;

        if (tank != null && tank.canBeDamaged()) {
            // skip damage for teammates in team modes
            if (owner != null && owner.getTeamId() != 0 && owner.getTeamId() == tank.getTeamId()) {
                // do not apply damage to teammate
            } else {
                tank.takeDamage(EXPLOSION_DAMAGE, explosionCenterX, explosionCenterY);
            }
        }
    }

    public boolean isExpired() {
        return state == BombState.EXPIRED;
    }

    private void drawArmed(Graphics2D g2) {
        BufferedImage frame = resolveFrame(armedFrames, animationTick / EXPLOSION_TICKS_PER_FRAME);
        if (frame == null) {
            return;
        }

        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ARMED_ALPHA_PERCENT / 100f));
        int drawX = triggerArea.x + triggerArea.width / 2 - BOMB_DRAW_SIZE / 2;
        int drawY = triggerArea.y + triggerArea.height / 2 - BOMB_DRAW_SIZE / 2;
        g.drawImage(frame, drawX, drawY, BOMB_DRAW_SIZE, BOMB_DRAW_SIZE, null);
        g.dispose();
    }

    private void drawExplosion(Graphics2D g2) {
        BufferedImage frame = resolveFrame(explosionFrames, animationTick / EXPLOSION_TICKS_PER_FRAME);
        if (frame == null) {
            return;
        }

        int drawX = explosionCenterX - EXPLOSION_DRAW_SIZE / 2;
        int drawY = explosionCenterY - EXPLOSION_DRAW_SIZE / 2;
        g2.drawImage(frame, drawX, drawY, EXPLOSION_DRAW_SIZE, EXPLOSION_DRAW_SIZE, null);
    }

    private int getExplosionDurationTicks() {
        return Math.max(1, explosionFrames.length) * EXPLOSION_TICKS_PER_FRAME;
    }

    private BufferedImage resolveFrame(BufferedImage[] frames, int frameIndex) {
        if (frames.length == 0) {
            return null;
        }
        int safeIndex = Math.min(frames.length - 1, Math.max(0, frameIndex));
        return frames[safeIndex];
    }

    private BufferedImage[] loadFrames(String resourcePath, int widthSprite, int heightSprite) {
        try {
            BufferedImage spriteSheet = loadImage(resourcePath);
            if (spriteSheet.getWidth() < widthSprite || spriteSheet.getHeight() < heightSprite) {
                return new BufferedImage[] { spriteSheet };
            }

            int cols = Math.max(1, spriteSheet.getWidth() / widthSprite);
            int rows = Math.max(1, spriteSheet.getHeight() / heightSprite);
            BufferedImage[] tempFrames = new BufferedImage[cols * rows];
            int frameIndex = 0;

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int x = col * widthSprite;
                    int y = row * heightSprite;
                    if (x + widthSprite <= spriteSheet.getWidth() && y + heightSprite <= spriteSheet.getHeight()) {
                        tempFrames[frameIndex++] = spriteSheet.getSubimage(x, y, widthSprite, heightSprite);
                    }
                }
            }

            if (frameIndex == 0) {
                return new BufferedImage[] { spriteSheet };
            }
            if (frameIndex == tempFrames.length) {
                return tempFrames;
            }

            BufferedImage[] normalizedFrames = new BufferedImage[frameIndex];
            System.arraycopy(tempFrames, 0, normalizedFrames, 0, frameIndex);
            return normalizedFrames;
        } catch (IOException e) {
            System.out.println("Error loading bomb sprite " + e.getMessage());
            return new BufferedImage[0];
        }
    }

    private BufferedImage loadImage(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Missing resource: " + resourcePath);
        }
        return ImageIO.read(inputStream);
    }
}


