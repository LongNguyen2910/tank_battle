package entity;

public enum TankType {
    NORMAL(100,4 , 3, 5, 26,"/tanks/tank_01.png", BulletType.NORMAL),
    HEAVY(80, 3, 1, 1, 30,"/tanks/tank_02.png", BulletType.HEAVY),
    SCOUT(120, 5, 5, 6, 22,"/tanks/tank_03.png", BulletType.LIGHT),
    MODERN(120, 4, 5, 5, 22,"/tanks/tank_04.png", BulletType.ENERGY);

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