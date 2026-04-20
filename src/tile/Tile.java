package tile;

import java.awt.image.BufferedImage;

public class Tile {
    private BufferedImage image;
    private boolean collision = false;
    private boolean breakable = false;
    private boolean bulletCollision = false;

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
