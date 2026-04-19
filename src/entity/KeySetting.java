package entity;

public class KeySetting {
    private final int keyUp;
    private final int keyRight;
    private final int keyDown;
    private final int keyLeft;
    private final int keyDash;
    private final int keyShoot;
    private final int keySkill1;
    private final int keySkill2;

    public KeySetting(int keyUp, int keyRight, int keyDown, int keyLeft, int keyDash, int keyShoot, int keySkill1, int keySkill2) {
        this.keyUp = keyUp;
        this.keyRight = keyRight;
        this.keyDown = keyDown;
        this.keyLeft = keyLeft;
        this.keyDash = keyDash;
        this.keyShoot = keyShoot;
        this.keySkill1 = keySkill1;
        this.keySkill2 = keySkill2;
    }

    public int getKeyUp() {
        return keyUp;
    }

    public int getKeyRight() {
        return keyRight;
    }

    public int getKeyDown() {
        return keyDown;
    }

    public int getKeyLeft() {
        return keyLeft;
    }

    public int getKeyDash() {
        return keyDash;
    }

    public int getKeyShoot() {
        return keyShoot;
    }

    public int getKeySkill1() {
        return keySkill1;
    }

    public int getKeySkill2() {
        return keySkill2;
    }
}
