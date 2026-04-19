package entity;

import main.GamePanel;
import main.KeyHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class Tank extends GameObject {
    public enum TankState {
        IDLE,
        MOVING,
        TAKING_DAMAGE,
        ACID,
        BURNING,
        DYING,
        DEAD
    }

    private static final int EFFECT_SPRITE_SIZE = 48;
    private static final int HIT_TICKS_PER_FRAME = 3;
    private static final int DEATH_TICKS_PER_FRAME = 4;

    private final GamePanel gp;
    private final KeyHandler keyH;

    private final TankType type;
    private int currentHealth;
    private final int maxFuel;
    private int currentFuel;
    private BufferedImage sprite;
    private BufferedImage slotSkill1, slotSkill2;
    private BufferedImage[] healthStatus, fuelStatus;
    private BufferedImage[] hitEffectFrames = new BufferedImage[0];
    private BufferedImage[] deathEffectFrames = new BufferedImage[0];
    private final int playerNum;
    private final KeySetting keySetting;

    private int shotCooldown = 0;
    private TankState state = TankState.IDLE;
    private int hitEffectTick = -1;
    private int deathEffectTick = 0;
    private boolean pendingRemoval = false;

    public Tank(GamePanel gp, KeyHandler keyH, TankType type, int playerNum, KeySetting keySetting) {
        this.gp = gp;
        this.keyH = keyH;
        this.type = type;

        this.maxFuel = type.getFuel();
        this.currentFuel = type.getFuel();
        this.playerNum = playerNum;
        this.keySetting = keySetting;

        healthStatus =  new BufferedImage[9];
        fuelStatus =  new BufferedImage[9];

        setDefaultValues();
        getSprite();
    }

    public void setDefaultValues() {
        speed = type.getSpeed();
        direction = Direction.UP;
        solidArea = new Rectangle(100, 100,
                type.getHitboxSize() * gp.scale, type.getHitboxSize() * gp.scale);
        if (playerNum == 1) {
            solidArea.x = gp.xSpawnPlayer1;
            solidArea.y = gp.ySpawnPlayer1;
        } else if (playerNum == 2) {
            solidArea.x = gp.xSpawnPlayer2;
            solidArea.y = gp.ySpawnPlayer2;
        } else  if (playerNum == 3) {
            solidArea.x = gp.xSpawnPlayer3;
            solidArea.y = gp.ySpawnPlayer3;
        } else if (playerNum == 4) {
            solidArea.x = gp.xSpawnPlayer4;
            solidArea.y = gp.ySpawnPlayer4;
        }
        currentHealth = 100;
        currentFuel = maxFuel;
        state = TankState.IDLE;
        hitEffectTick = -1;
        deathEffectTick = 0;
        pendingRemoval = false;
    }

    public void getSprite() {
        try {
            String prefix = type.getImagePrefix();
            sprite = loadImage(prefix);
            BufferedImage tempSprite, tempSprite2;
            tempSprite = loadImage("/ui/fuel_status.png");
            tempSprite2 = loadImage("/ui/health_status.png");
            for (int i = 0; i < 9; i++) {
                healthStatus[i] = tempSprite2.getSubimage(i * 48, 0, 48, 48);
                fuelStatus[i] = tempSprite.getSubimage(i * 48, 0, 48, 48);
            }
            slotSkill1 = slotSkill2 = loadImage("/ui/inventory.png");

            hitEffectFrames = loadEffectFrames("/tanks/bullet_hit.png", EFFECT_SPRITE_SIZE);
            deathEffectFrames = loadEffectFrames("/tanks/explosion.png", 64);
        } catch (IOException e) {
            System.out.println("Error loading tank's sprite" + e.getMessage());
        }
    }

    private BufferedImage[] loadEffectFrames(String resourcePath, int sizeSprite) throws IOException {
        BufferedImage spriteSheet = loadImage(resourcePath);
        int cols = Math.max(1, spriteSheet.getWidth() / sizeSprite);
        int rows = Math.max(1, spriteSheet.getHeight() / sizeSprite);
        int frameCount = cols * rows;
        BufferedImage[] frames = new BufferedImage[frameCount];
        int frameIndex = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * sizeSprite;
                int y = row * sizeSprite;
                if (x + sizeSprite <= spriteSheet.getWidth() && y + sizeSprite <= spriteSheet.getHeight()) {
                    frames[frameIndex++] = spriteSheet.getSubimage(x, y, sizeSprite, sizeSprite);
                }
            }
        }

        if (frameIndex == frames.length) {
            return frames;
        }

        BufferedImage[] normalizedFrames = new BufferedImage[frameIndex];
        System.arraycopy(frames, 0, normalizedFrames, 0, frameIndex);
        return normalizedFrames;
    }

    private BufferedImage loadImage(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Missing resource: " + resourcePath);
        }
        return ImageIO.read(inputStream);
    }

    public void update() {
        if (state == TankState.DEAD) {
            return;
        }

        if (state == TankState.DYING) {
            updateDeathState();
            return;
        }

        updateHitState();

        int baseSpeed = type.getSpeed();
        speed = baseSpeed;

        if (keyH.isPressed(keySetting.getKeyDash()) && currentFuel > 0) {
            speed = baseSpeed + 2;

            currentFuel--;
            System.out.println(currentFuel);
        }

        Direction inputDirection = readInputDirection();

        if (inputDirection != Direction.NONE) {
            if (state != TankState.TAKING_DAMAGE) {
                state = TankState.MOVING;
            }
            direction = inputDirection;
            collisionOn = false;
            gp.getCollisionChecker().checkTile(this);
            if (!collisionOn) {
                move(direction);
            }
        } else if (state == TankState.MOVING) {
            state = TankState.IDLE;
        }

        if (shotCooldown > 0) {
            shotCooldown--;
        }

        if (keyH.isPressed(keySetting.getKeyShoot()) && shotCooldown == 0) {
            shoot();
        }
    }

    private Direction readInputDirection() {
        if (keyH.isPressed(keySetting.getKeyUp())) {
            return Direction.UP;
        }
        if (keyH.isPressed(keySetting.getKeyDown())) {
            return Direction.DOWN;
        }
        if (keyH.isPressed(keySetting.getKeyLeft())) {
            return Direction.LEFT;
        }
        if (keyH.isPressed(keySetting.getKeyRight())) {
            return Direction.RIGHT;
        }
        return Direction.NONE;
    }

    private void move(Direction moveDirection) {
        switch (moveDirection) {
            case UP:
                solidArea.y -= speed;
                break;
            case DOWN:
                solidArea.y += speed;
                break;
            case LEFT:
                solidArea.x -= speed;
                break;
            case RIGHT:
                solidArea.x += speed;
                break;
            case NONE:
                break;
        }
    }

    private void shoot() {
        int bulletX = solidArea.x;
        int bulletY = solidArea.y;

        switch (direction) {
            case UP:
                bulletX = solidArea.x + (solidArea.width / 2 - type.getBulletType().getWidth() + 2);
                bulletY = solidArea.y - solidArea.height / 2 - 5;
                break;
            case RIGHT:
                bulletX = solidArea.x + solidArea.width + 1;
                bulletY = solidArea.y + (solidArea.height / 2 - type.getBulletType().getHeight() - 3);
                break;
            case DOWN:
                bulletX = solidArea.x + (solidArea.width / 2 - type.getBulletType().getWidth() + 2);
                bulletY = solidArea.y + solidArea.height;
                break;
            case LEFT:
                bulletX = solidArea.x - solidArea.width / 2 - 3;
                bulletY = solidArea.y + (solidArea.height / 2 - type.getBulletType().getWidth() + 2);
                break;
            case NONE:
                return;
        }

        gp.addBullet(new Bullet(gp, bulletX, bulletY, direction, type));
        shotCooldown = type.getBulletType().getCooldown();
    }

    private void updateHitState() {
        if (hitEffectTick < 0) {
            return;
        }

        hitEffectTick++;
        if (hitEffectTick >= getTotalHitAnimationTicks()) {
            hitEffectTick = -1;
            if (state == TankState.TAKING_DAMAGE) {
                state = TankState.IDLE;
            }
        }
    }

    private void updateDeathState() {
        deathEffectTick++;
        if (deathEffectTick >= getTotalDeathAnimationTicks()) {
            state = TankState.DEAD;
            pendingRemoval = true;
        }
    }

    private int getTotalHitAnimationTicks() {
        return Math.max(1, hitEffectFrames.length) * HIT_TICKS_PER_FRAME;
    }

    private int getTotalDeathAnimationTicks() {
        return Math.max(1, deathEffectFrames.length) * DEATH_TICKS_PER_FRAME;
    }

    private void startHitEffect() {
        hitEffectTick = 0;
        if (state != TankState.DYING && state != TankState.DEAD) {
            state = TankState.TAKING_DAMAGE;
        }
    }

    private void startDeath() {
        state = TankState.DYING;
        deathEffectTick = 0;
        shotCooldown = 0;
    }

    public void draw(Graphics2D g2) {
        if (state == TankState.DEAD) {
            return;
        }

        if (state != TankState.DYING) {
            drawUI(g2);
            drawTank(g2);
            drawHitEffect(g2);

            g2.setColor(Color.RED);
            g2.draw(solidArea);
            return;
        }

        drawDeathEffect(g2);
    }

    private void drawHitEffect(Graphics2D g2) {
        if (hitEffectTick < 0 || hitEffectFrames.length == 0) {
            return;
        }

        int frame = Math.min(hitEffectFrames.length - 1, hitEffectTick / HIT_TICKS_PER_FRAME);
        drawCenteredEffect(g2, hitEffectFrames[frame], 48 * gp.scale);
    }

    private void drawDeathEffect(Graphics2D g2) {
        if (deathEffectFrames.length == 0) {
            return;
        }

        int frame = Math.min(deathEffectFrames.length - 1, deathEffectTick / DEATH_TICKS_PER_FRAME);
        drawCenteredEffect(g2, deathEffectFrames[frame], 64 * gp.scale);
    }

    private void drawCenteredEffect(Graphics2D g2, BufferedImage frame, int drawSize) {
        if (frame == null) {
            return;
        }

        int centerX = solidArea.x + solidArea.width / 2;
        int centerY = solidArea.y + solidArea.height / 2;
        int drawX = centerX - drawSize / 2;
        int drawY = centerY - drawSize / 2;
        g2.drawImage(frame, drawX, drawY, drawSize, drawSize, null);
    }

    public void drawTank(Graphics2D g2) {
        BufferedImage tank;
        String name = "PLAYER " +  playerNum;
        g2.setFont(new java.awt.Font("m6x11plus", Font.PLAIN, 14));
        g2.setColor(java.awt.Color.WHITE);
        int textLength = (int) g2.getFontMetrics().getStringBounds(name, g2).getWidth();
        int textX, textY;
        final int cooldownBar = gp.tileSize / 2;
        double cooldownBarHeight = (double)shotCooldown / type.getBulletType().getCooldown() * cooldownBar;
        switch (direction) {
            case UP:
                tank =  sprite.getSubimage(0, 0, 32, 32);
                g2.drawImage(tank, solidArea.x - type.getHitboxX() * gp.scale, solidArea.y - type.getHitboxY() * gp.scale,
                        gp.tileSize, gp.tileSize, null);

                textX = solidArea.x + (solidArea.width / 2) - (textLength / 2);
                textY = solidArea.y - type.getHitboxY() * gp.scale + 32 * gp.scale + 15;
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString(name, textX + 1, textY + 1);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(name, textX, textY);
                if (shotCooldown > 0) {
                    g2.fillRect(solidArea.x - type.getHitboxX() - 15, (int)(solidArea.y - type.getHitboxY() + cooldownBarHeight),
                            5, (int)(cooldownBar - cooldownBarHeight));
                }
                break;
            case RIGHT:
                tank =  sprite.getSubimage(0, 32, 32, 32);
                g2.drawImage(tank, solidArea.x - (32 - type.getHitboxY() - type.getHitboxSize()) * gp.scale, solidArea.y - type.getHitboxX() * gp.scale,
                        gp.tileSize, gp.tileSize, null);

                textX = solidArea.x + (solidArea.width / 2) - (textLength / 2);
                textY = solidArea.y +  solidArea.height + 15;
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString(name, textX + 1, textY + 1);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(name, textX, textY);
                if (shotCooldown > 0) {
                    g2.fillRect(solidArea.x - type.getHitboxX() * gp.scale - 10, (int)(solidArea.y + cooldownBarHeight), 5, (int)(cooldownBar - cooldownBarHeight));
                }
                break;
            case DOWN:
                tank =  sprite.getSubimage(0, 64, 32, 32);
                g2.drawImage(tank, solidArea.x - type.getHitboxX() * gp.scale, solidArea.y- (32 - type.getHitboxY() - type.getHitboxSize()) * gp.scale,
                        gp.tileSize, gp.tileSize, null);

                textX = solidArea.x + (solidArea.width / 2) - (textLength / 2);
                textY = solidArea.y - type.getHitboxY() * gp.scale - 5;
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString(name, textX + 1, textY + 1);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(name, textX, textY);
                if (shotCooldown > 0) {
                    g2.fillRect(solidArea.x - 15, (int)(solidArea.y + cooldownBarHeight), 5, (int)(cooldownBar - cooldownBarHeight));
                }
                break;
            case LEFT:
                tank =  sprite.getSubimage(0, 96, 32, 32);
                g2.drawImage(tank, solidArea.x - type.getHitboxY() * gp.scale, solidArea.y - type.getHitboxX() * gp.scale,
                        gp.tileSize, gp.tileSize, null);

                textX = solidArea.x + (solidArea.width / 2) - (textLength / 2);
                textY = solidArea.y +  solidArea.height + 15;
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString(name, textX + 1, textY + 1);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(name, textX, textY);
                if (shotCooldown > 0) {
                    g2.fillRect(solidArea.x, solidArea.y - 15, (int)(cooldownBar - cooldownBarHeight), 5);
                }
                break;
            case NONE:
                break;
        }
    }


    public void drawUI(Graphics2D g2) {
        int numSprite = 7 - (int)(currentHealth / (double)(100/7));
        int numSpriteFuel = 7 - (int)(currentFuel / (double)(type.getFuel() / 7));
        switch (playerNum) {
            case 1:
                //bottom left
                g2.drawImage(healthStatus[numSprite], 5, gp.screenHeight - 98,48, 48, null);
                g2.drawImage(fuelStatus[numSpriteFuel], 5, gp.screenHeight - 48,48 , 48, null);
                g2.drawImage(slotSkill1, 52, gp.screenHeight - 30, 32, 32, null);
                g2.drawImage(slotSkill2, 80, gp.screenHeight - 30, 32, 32, null);
                break;
            case 2:
                //bottom right
                g2.drawImage(healthStatus[numSprite], gp.screenWidth - 48 - 1, gp.screenHeight - 98,
                        48, 48, null);
                g2.drawImage(fuelStatus[numSpriteFuel], gp.screenWidth - 48 - 1, gp.screenHeight - 48,
                        48, 48, null);
                g2.drawImage(slotSkill1, gp.screenWidth - 52 - 27 , gp.screenHeight - 30, 32, 32, null);
                g2.drawImage(slotSkill2, gp.screenWidth - 80 - 27, gp.screenHeight - 30, 32, 32, null);

                break;
            case 3:
                //top left
                g2.drawImage(healthStatus[numSprite], 5, 0,
                        48, 48, null);
                g2.drawImage(fuelStatus[numSpriteFuel], 5, 50,
                        48, 48, null);
                g2.drawImage(slotSkill1, 52 , 0, 32, 32, null);
                g2.drawImage(slotSkill2, 80, 0, 32, 32, null);

                break;
            case 4:
                //top right
                g2.drawImage(healthStatus[numSprite], gp.screenWidth - 48 - 1, 0,
                        48, 48, null);
                g2.drawImage(fuelStatus[numSpriteFuel], gp.screenWidth - 48 - 1, 50,
                        48, 48, null);
                g2.drawImage(slotSkill1, gp.screenWidth - 52 - 27 , 0, 32, 32, null);
                g2.drawImage(slotSkill2, gp.screenWidth - 80 - 27, 0, 32, 32, null);

                break;
        }
    }

    public void takeDamage(int damage) {
        if (!canBeDamaged()) {
            return;
        }

        currentHealth = Math.max(0, currentHealth - damage);
        startHitEffect();

        if (currentHealth == 0) {
            startDeath();
        }

    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public boolean canBeDamaged() {
        return state != TankState.DYING && state != TankState.DEAD;
    }

    public boolean isPendingRemoval() {
        return pendingRemoval;
    }

    public TankState getState() {
        return state;
    }
}
