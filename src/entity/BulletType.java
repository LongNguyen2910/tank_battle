package entity;

public enum BulletType {
    NORMAL(10, 10, 5, 8, 3, 5, 60, 16, "/bullets/nor_bullet.png"),
    HEAVY(15, 13, 7, 6, 1, 4, 120, 16, "/bullets/heavy_bullet.png"),
    LIGHT(8, 12, 4, 10, 2, 6, 30, 16, "/bullets/light_bullet.png"),
    ENERGY(10, 8, 5, 10, 4, 6, 60, 16, "/bullets/nor1_bullet.png"),
    BIG_BULLET(30, 32, 17, 8, 0, 7, 200, 32, "/bullets/big_bullet.png");
    private final int damage;
    private final int width;
    private final int height;
    private final int bulletSpeed;
    private final int xOffset;
    private final int yOffset;
    private final int cooldown;
    private final int spriteFrameSize;
    private final String filePath;

    BulletType(int damage, int width, int height, int bulletSpeed, int xOffset, int yOffset, int cooldown, int spriteFrameSize, String filePath) {
        this.damage = damage;
        this.width = width;
        this.height = height;
        this.bulletSpeed = bulletSpeed;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.cooldown = cooldown;
        this.spriteFrameSize = spriteFrameSize;
        this.filePath = filePath;
    }

    public int getDamage() {
        return damage;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBulletSpeed() {
        return bulletSpeed;
    }

    public int getxOffset() {
        return xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getSpriteFrameSize() {
        return spriteFrameSize;
    }

    public String getFilePath() {
        return filePath;
    }
}
