package entity;

import main.Config;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class Trap {
    private enum TrapState {
        ARMED,
        TRIGGERED,
        EXPIRED
    }

    private static final int ARMED_ALPHA_PERCENT = 40;
    private static final int TRAP_HITBOX_SIZE = 20;
    private static final int TRAP_DRAW_SIZE = Config.ORIGINAL_TILE_SIZE;
    private static final int EFFECT_DRAW_SIZE = Config.TILE_SIZE;
    private static final int EFFECT_TICKS_PER_FRAME = 3;
    private static final int TRAP_DURATION_TICKS = 180;
    private static final int OWNER_IMMUNITY_TICKS = 20;

    private final Rectangle triggerArea;
    private final BufferedImage[] armedFrames;
    private final BufferedImage[] effectFrames;
    private final Tank owner;
    private TrapState state = TrapState.ARMED;
    private int animationTick = 0;
    private int ownerImmunityTicks = OWNER_IMMUNITY_TICKS;
    private int durationTicks = TRAP_DURATION_TICKS;
    private Tank trapTarget = null;
    private int effectCenterX;
    private int effectCenterY;

    public Trap(int centerX, int centerY, Tank owner) {
        int halfSize = TRAP_HITBOX_SIZE / 2;
        this.triggerArea = new Rectangle(centerX - halfSize, centerY - halfSize, TRAP_HITBOX_SIZE, TRAP_HITBOX_SIZE);
        this.effectCenterX = centerX;
        this.effectCenterY = centerY;
        this.owner = owner;
        this.armedFrames = loadFrames("/effects/trap.png", 16, 16);
        this.effectFrames = loadFrames("/effects/trap_effect.png", 16, 16);
    }

    public void update() {
        if (state == TrapState.ARMED) {
            animationTick++;
            if (ownerImmunityTicks > 0) {
                ownerImmunityTicks--;
            }
            return;
        }

        if (state == TrapState.TRIGGERED) {
            animationTick++;
            durationTicks--;

            if (trapTarget != null) {
                if (trapTarget.getState() == Tank.TankState.DEAD || trapTarget.getState() == Tank.TankState.DYING) {
                    state = TrapState.EXPIRED;
                    if (trapTarget != null) {
                        trapTarget.releaseTrap();
                        trapTarget = null;
                    }
                    return;
                }

                if (durationTicks <= 0) {
                    state = TrapState.EXPIRED;
                    if (trapTarget != null) {
                        trapTarget.releaseTrap();
                        trapTarget = null;
                    }
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (state == TrapState.EXPIRED) {
            return;
        }

        if (state == TrapState.ARMED) {
            drawArmed(g2);
            return;
        }

        drawTriggered(g2);
    }

    public boolean canTrigger() {
        return state == TrapState.ARMED;
    }

    public boolean intersects(Rectangle area) {
        return area != null && triggerArea.intersects(area);
    }

    public boolean canBeTriggeredBy(Tank tank) {
        if (!canTrigger() || tank == null) {
            return false;
        }
        // Prevent teammates from triggering owner's trap in team modes
        if (owner != null && owner.getTeamId() != 0 && owner.getTeamId() == tank.getTeamId()) {
            return false;
        }
        return tank != owner || ownerImmunityTicks <= 0;
    }

    public void trigger(Tank tank) {
        if (!canTrigger()) {
            return;
        }

        // Additional safety: do not trigger effects on teammates in team modes
        if (owner != null && tank != null && owner.getTeamId() != 0 && owner.getTeamId() == tank.getTeamId()) {
            // ignore triggering by teammate
            return;
        }

        effectCenterX = triggerArea.x + triggerArea.width / 2;
        effectCenterY = triggerArea.y + triggerArea.height / 2;
        state = TrapState.TRIGGERED;
        animationTick = 0;
        durationTicks = TRAP_DURATION_TICKS;
        trapTarget = tank;

        if (tank != null) {
            tank.trapTank(this);
        }
    }

    public boolean isExpired() {
        return state == TrapState.EXPIRED;
    }

    public Tank getTrapTarget() {
        return trapTarget;
    }

    private void drawArmed(Graphics2D g2) {
        BufferedImage frame = resolveFrame(armedFrames, animationTick / EFFECT_TICKS_PER_FRAME);
        if (frame == null) {
            return;
        }

        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ARMED_ALPHA_PERCENT / 100f));
        int drawX = triggerArea.x + triggerArea.width / 2 - TRAP_DRAW_SIZE / 2;
        int drawY = triggerArea.y + triggerArea.height / 2 - TRAP_DRAW_SIZE / 2;
        g.drawImage(frame, drawX, drawY, TRAP_DRAW_SIZE, TRAP_DRAW_SIZE, null);
        g.dispose();
    }

    private void drawTriggered(Graphics2D g2) {
        BufferedImage frame = resolveFrame(effectFrames, animationTick / EFFECT_TICKS_PER_FRAME);
        if (frame == null) {
            return;
        }

        int drawX = effectCenterX - EFFECT_DRAW_SIZE / 2;
        int drawY = effectCenterY - EFFECT_DRAW_SIZE / 2;
        g2.drawImage(frame, drawX, drawY, EFFECT_DRAW_SIZE, EFFECT_DRAW_SIZE, null);
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
            System.out.println("Error loading trap sprite " + e.getMessage());
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

