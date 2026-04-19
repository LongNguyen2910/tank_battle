package entity;

public enum TankType {
    NORMAL(100,4 , 3, 5, 26,"/tanks/tank_01.png", BulletType.NORMAL);
//    HEAVY(40, 60, 4, 4,"/tanks/heavy_"), // Xe tăng bọc thép: Bắn đau, hitbox to (dễ trúng đạn), ít nhiên liệu
//    SCOUT(10, 150, 12, 12,"/tanks/scout_"); // Xe trinh sát: Bắn yếu, hitbox nhỏ (dễ né), nhiều nhiên liệu

    private final int fuel;
    private final int hitboxX;
    private final int hitboxY;
    private final int hitboxSize;
    private final String imagePrefix;
    private final int speed;
    private final BulletType bulletType;

    TankType(int fuel, int speed, int hitboxX, int hitboxY, int hitboxSize, String imagePrefix, BulletType bulletType) {
        this.fuel = fuel;
        this.speed = speed;
        this.hitboxX = hitboxX;
        this.hitboxY = hitboxY;
        this.hitboxSize = hitboxSize;
        this.imagePrefix = imagePrefix;
        this.bulletType = bulletType;
    }

    public int getFuel() {
        return fuel;
    }

    public int getSpeed() {
        return speed;
    }

    public int getHitboxX() {
        return hitboxX;
    }

    public int getHitboxY() {
        return hitboxY;
    }

    public int getHitboxSize() {
        return hitboxSize;
    }

    public String getImagePrefix() {
        return imagePrefix;
    }

    public BulletType getBulletType() {
        return bulletType;
    }
}