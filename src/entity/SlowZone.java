package entity;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class SlowZone {
    private static final int SLOW_EFFECT_SPRITE_SIZE = 16;
    private static final int SLOW_TICKS_PER_FRAME = 4;
    private static final int DEFAULT_DURATION_TICKS = 180;
    private static final float DEFAULT_SLOW_MULTIPLIER = 0.5f;
    private static final int DEFAULT_DRAW_SIZE_MULTIPLIER = 3;

    private final BufferedImage[] frames;
    private final int centerX;
    private final int centerY;
    private final int radius;
    private final int drawSize;
    private final float slowMultiplier;
    private int lifeTicks;
    private int animationTick = 0;

    public SlowZone(main.GamePanel gp, int centerX, int centerY, int durationTicks, float slowMultiplier, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.drawSize = radius * 2;
        this.slowMultiplier = slowMultiplier;
        this.lifeTicks = Math.max(1, durationTicks);
        this.frames = loadFrames("/effects/slow.png", SLOW_EFFECT_SPRITE_SIZE, SLOW_EFFECT_SPRITE_SIZE);
    }

    public void update() {
        if (lifeTicks > 0) {
            lifeTicks--;
        }
        animationTick++;
    }

    public void draw(Graphics2D g2) {
        if (isExpired() || frames.length == 0) {
            return;
        }

        int frame = (animationTick / SLOW_TICKS_PER_FRAME) % frames.length;
        BufferedImage image = frames[frame];
        if (image == null) {
            return;
        }

        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
        int drawX = centerX - drawSize / 2;
        int drawY = centerY - drawSize / 2;
        g.drawImage(image, drawX, drawY, drawSize, drawSize, null);
        g.dispose();
    }

    public boolean contains(Rectangle area) {
        if (area == null) {
            return false;
        }

        int areaCenterX = area.x + area.width / 2;
        int areaCenterY = area.y + area.height / 2;
        int dx = areaCenterX - centerX;
        int dy = areaCenterY - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    public float getSlowMultiplier() {
        return slowMultiplier;
    }

    public boolean isExpired() {
        return lifeTicks <= 0;
    }

    private BufferedImage[] loadFrames(String resourcePath, int widthSprite, int heightSprite) {
        try {
            BufferedImage spriteSheet = loadImage(resourcePath);
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

            if (frameIndex == tempFrames.length) {
                return tempFrames;
            }

            BufferedImage[] normalizedFrames = new BufferedImage[frameIndex];
            System.arraycopy(tempFrames, 0, normalizedFrames, 0, frameIndex);
            return normalizedFrames;
        } catch (IOException e) {
            System.out.println("Error loading slow zone sprite" + e.getMessage());
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


