package tile;

import java.awt.image.BufferedImage;

public class Tile {
    private BufferedImage image;

    private boolean collision = false;
    private boolean breakable = false;
    private boolean bulletCollision = false;

    private boolean isAnimated = false;
    private BufferedImage[] frames;
    private int currentFrame = 0;

    public int getFrameCounter() {
        return frameCounter;
    }

    public void setFrameCounter(int frameCounter) {
        this.frameCounter = frameCounter;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public BufferedImage[] getFrames() {
        return frames;
    }

    public void setFrames(BufferedImage[] frames) {
        this.frames = frames;
    }

    public boolean isAnimated() {
        return isAnimated;
    }

    public void setAnimated(boolean animated) {
        isAnimated = animated;
    }

    private int frameCounter = 0;

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public boolean isCollision() {
        return collision;
    }

    public void setCollision(boolean collision) {
        this.collision = collision;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public void setBreakable(boolean breakable) {
        this.breakable = breakable;
    }

    public boolean isBulletCollision() {
        return bulletCollision;
    }

    public void setBulletCollision(boolean bulletCollision) {
        this.bulletCollision = bulletCollision;
    }
}
