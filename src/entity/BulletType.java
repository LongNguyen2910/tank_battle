package entity;

public enum BulletType {
    NORMAL(20, 10, 5, 8, 3, 5, 60, "/bullets/nor_bullet.png");
    private final int damage;
    private final int width;
    private final int height;
    private final int bulletSpeed;
    private final int xOffset;
    private final int yOffset;
    private final int cooldown;
    private final String filePath;

    BulletType(int damage, int width, int height, int bulletSpeed, int xOffset, int yOffset, int cooldown, String filePath) {
        this.damage = damage;
        this.width = width;
        this.height = height;
        this.bulletSpeed = bulletSpeed;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.cooldown = cooldown;
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

    public String getFilePath() {
        return filePath;
    }
}
