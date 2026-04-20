package entity;

public class KeySetting {
    private int keyUp;
    private int keyRight;
    private int keyDown;
    private int keyLeft;
    private int keyDash;
    private int keyShoot;
    private int keySkill1;
    private int keySkill2;

    public KeySetting(int keyUp, int keyRight, int keyDown, int keyLeft, int keyShoot, int keyDash, int keySkill1, int keySkill2) {
        this.keyUp = keyUp;
        this.keyRight = keyRight;
        this.keyDown = keyDown;
        this.keyLeft = keyLeft;
        this.keyShoot = keyShoot;
        this.keyDash = keyDash;
        this.keySkill1 = keySkill1;
        this.keySkill2 = keySkill2;
    }

    public int getKeyUp() { return keyUp; }
    public void setKeyUp(int keyUp) { this.keyUp = keyUp; }

    public int getKeyRight() { return keyRight; }
    public void setKeyRight(int keyRight) { this.keyRight = keyRight; }

    public int getKeyDown() { return keyDown; }
    public void setKeyDown(int keyDown) { this.keyDown = keyDown; }

    public int getKeyLeft() { return keyLeft; }
    public void setKeyLeft(int keyLeft) { this.keyLeft = keyLeft; }

    public int getKeyDash() { return keyDash; }
    public void setKeyDash(int keyDash) { this.keyDash = keyDash; }

    public int getKeyShoot() { return keyShoot; }
    public void setKeyShoot(int keyShoot) { this.keyShoot = keyShoot; }

    public int getKeySkill1() { return keySkill1; }
    public void setKeySkill1(int keySkill1) { this.keySkill1 = keySkill1; }

    public int getKeySkill2() { return keySkill2; }
    public void setKeySkill2(int keySkill2) { this.keySkill2 = keySkill2; }
}
