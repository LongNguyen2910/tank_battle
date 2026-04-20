package entity;

import main.Config;
import main.GamePanel;
import main.KeyHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private static final int INVINCIBILITY_DURATION_TICKS = 60;
    private static final int BLINK_TICKS_PER_STEP = 5;
    private static final float BLINK_ALPHA = 0.35f;

    private static final int POISON_TICKS_PER_FRAME = 6;
    private static final int TOXIC_DURATION_TICKS = 180;
    private static final int TOXIC_TICK_INTERVAL = 30;
    private static final int TOXIC_DAMAGE_PER_TICK = 10;
    private static final int SLOW_DEBUFF_TICKS = 120;
    private static final int SLOW_VISUAL_TICKS = 12;
    private static final int SLOW_RADIUS_TILES = 2;
    private static final float SLOW_DEBUFF_MULTIPLIER = 0.5f;
    private static final int SLOW_TRAIL_SPAWN_INTERVAL = 2;
    private static final int SLOW_TRAIL_SEGMENTS_PER_SPAWN = 3;
    private static final int SLOW_TRAIL_SEGMENT_LIFETIME = 24;
    private static final int SLOW_TRAIL_MAX_SEGMENTS = 48;

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
    private BufferedImage[] poisonEffectFrames = new BufferedImage[0];
    private final int playerNum;
    private final KeySetting keySetting;

    private int shotCooldown = 0;
    private TankState state = TankState.IDLE;
    private int hitEffectTick = -1;
    private int invincibilityTick = -1;
    private int deathEffectTick = 0;

    private boolean pendingRemoval = false;

    int particleCounter = 0;

    private boolean hasShield = false;
    private int shieldDuration = 0;
    private BulletEffectType armedBulletEffect = BulletEffectType.NONE;
    private boolean skill1WasPressed = false;
    private boolean skill2WasPressed = false;
    private int slowDebuffTicksRemaining = 0;
    private float slowDebuffMultiplier = 1.0f;
    private int slowTrailSpawnCounter = 0;
    private final List<SlowTrailSegment> slowTrailSegments = new ArrayList<>();
    private int poisonTicksRemaining = 0;
    private int poisonTickCounter = 0;
    private int poisonEffectTick = 0;

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
                type.getHitboxSize() * Config.SCALE, type.getHitboxSize() * Config.SCALE);
        if (playerNum == 1) {
            solidArea.x = Config.X_SPAWN_PLAYER_1;
            solidArea.y = Config.Y_SPAWN_PLAYER_1;
        } else if (playerNum == 2) {
            solidArea.x = Config.X_SPAWN_PLAYER_2;
            solidArea.y = Config.Y_SPAWN_PLAYER_2;
        } else  if (playerNum == 3) {
            solidArea.x = Config.X_SPAWN_PLAYER_3;
            solidArea.y = Config.Y_SPAWN_PLAYER_3;
        } else if (playerNum == 4) {
            solidArea.x = Config.X_SPAWN_PLAYER_4;
            solidArea.y = Config.Y_SPAWN_PLAYER_4;
        }
        currentHealth = Config.MAX_HEALTH;
        state = TankState.IDLE;
        hitEffectTick = -1;
        invincibilityTick = -1;
        deathEffectTick = 0;
        pendingRemoval = false;
        armedBulletEffect = BulletEffectType.NONE;
        skill1WasPressed = false;
        skill2WasPressed = false;
        slowDebuffTicksRemaining = 0;
        slowDebuffMultiplier = 1.0f;
        slowTrailSpawnCounter = 0;
        slowTrailSegments.clear();
        poisonTicksRemaining = 0;
        poisonTickCounter = 0;
        poisonEffectTick = 0;
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

            hitEffectFrames = loadEffectFrames("/tanks/bullet_hit.png", EFFECT_SPRITE_SIZE, EFFECT_SPRITE_SIZE);
            deathEffectFrames = loadEffectFrames("/tanks/explosion.png", 64, 64);
            poisonEffectFrames = loadEffectFrames("/effects/poison.png", 16, 32);
        } catch (IOException e) {
            System.out.println("Error loading tank's sprite" + e.getMessage());
        }
    }

    private BufferedImage[] loadEffectFrames(String resourcePath, int widthSprite, int heightSprite) throws IOException {
        BufferedImage spriteSheet = loadImage(resourcePath);
        int cols = Math.max(1, spriteSheet.getWidth() / widthSprite);
        int rows = Math.max(1, spriteSheet.getHeight() / heightSprite);
        int frameCount = cols * rows;
        BufferedImage[] frames = new BufferedImage[frameCount];
        int frameIndex = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * widthSprite;
                int y = row * heightSprite;
                if (x + widthSprite <= spriteSheet.getWidth() && y + heightSprite <= spriteSheet.getHeight()) {
                    frames[frameIndex++] = spriteSheet.getSubimage(x, y, widthSprite, heightSprite);
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

        if (hasShield) {
            shieldDuration--;

            if (shieldDuration <= 0) {
                hasShield = false;
            }
        }

        updateHitState();
        updateInvincibilityState();
        updateSlowDebuffState();
        updatePoisonState();

        int baseSpeed = type.getSpeed();
        int desiredSpeed = baseSpeed;

        if (keyH.isPressed(keySetting.getKeyDash()) && currentFuel > 0) {
            desiredSpeed = baseSpeed + 2;

            currentFuel--;

            particleCounter++;

            if (particleCounter >= 3) {

                int smokeX = 0;
                int smokeY = 0;

                switch (direction) {
                    case UP:
                        smokeX = solidArea.x + gp.tileSize / 2;
                        smokeY = solidArea.y + gp.tileSize; // Đuôi nằm ở cạnh dưới
                        break;
                    case DOWN:
                        smokeX = solidArea.x + gp.tileSize / 2;
                        smokeY = solidArea.y; // Đuôi nằm ở cạnh trên
                        break;
                    case LEFT:
                        smokeX = solidArea.x + gp.tileSize; // Đuôi nằm ở cạnh phải
                        smokeY = solidArea.y + gp.tileSize / 2;
                        break;
                    case RIGHT:
                        smokeX = solidArea.x; // Đuôi nằm ở cạnh trái
                        smokeY = solidArea.y + gp.tileSize / 2;
                        break;
                }

                smokeX += (int) (Math.random() * 11 - 5);

                gp.particleList.add(new SmokeParticle(gp, smokeX, smokeY, 10));

                particleCounter = 0;
            }
        }

        handleSkillInput(keySetting.getKeySkill1(), SkillType.SHIELD);
        handleSkillInput(keySetting.getKeySkill2(), SkillType.TRIPLE_SHOT);

        speed = applySlowDebuff(desiredSpeed);

        Direction inputDirection = readInputDirection();
        boolean movedThisTick = false;

        if (inputDirection != Direction.NONE) {
            if (state != TankState.TAKING_DAMAGE) {
                state = TankState.MOVING;
            }
            direction = inputDirection;
            collisionOn = false;
            gp.getCollisionChecker().checkTile(this);
            if (!collisionOn) {
                move(direction);
                movedThisTick = true;
            }
        } else if (state == TankState.MOVING) {
            state = TankState.IDLE;
        }

        updateSlowTrailState(movedThisTick);

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
        BulletEffectType shotEffect = armedBulletEffect;
        armedBulletEffect = BulletEffectType.NONE;
        spawnBullet(direction, shotEffect);
        shotCooldown = type.getBulletType().getCooldown();
    }

    private void spawnBullet(Direction bulletDirection, BulletEffectType effectType) {
        if (bulletDirection == Direction.NONE) {
            return;
        }

        Point spawnPoint = calculateBulletSpawnPoint(bulletDirection);
        gp.addBullet(new Bullet(gp, spawnPoint.x, spawnPoint.y, bulletDirection, type, effectType));
    }

    private Point calculateBulletSpawnPoint(Direction bulletDirection) {
        int bulletX = solidArea.x;
        int bulletY = solidArea.y;

        switch (bulletDirection) {
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
                bulletX = solidArea.x - solidArea.width / 2 - 6;
                bulletY = solidArea.y + (solidArea.height / 2 - type.getBulletType().getWidth() + 2);
                break;
            case NONE:
                break;
        }

        return new Point(bulletX, bulletY);
    }

    public void activateTripleShot() {
        if (shotCooldown > 0 || direction == Direction.NONE) {
            return;
        }

        Direction leftDirection = getLeftDirection(direction);
        Direction rightDirection = getRightDirection(direction);

        spawnBullet(direction, BulletEffectType.NONE);
        spawnBullet(leftDirection, BulletEffectType.NONE);
        spawnBullet(rightDirection, BulletEffectType.NONE);

        shotCooldown = type.getBulletType().getCooldown();
    }

    private void handleSkillInput(int skillKey, SkillType skill) {
        boolean isPressed = keyH.isPressed(skillKey);
        boolean wasPressed = skillKey == keySetting.getKeySkill1() ? skill1WasPressed : skill2WasPressed;

        if (isPressed && !wasPressed) {
            tryActivateSkill(skillKey, skill);
        }

        if (skillKey == keySetting.getKeySkill1()) {
            skill1WasPressed = isPressed;
        } else if (skillKey == keySetting.getKeySkill2()) {
            skill2WasPressed = isPressed;
        }
    }

    private void tryActivateSkill(int skillKey, SkillType skill) {
        if (!keyH.isPressed(skillKey) || skill == SkillType.NONE || currentFuel < skill.getFuelCost()) {
            return;
        }

        if (!canActivateSkill(skill)) {
            return;
        }

        BulletEffectType requestedEffect = getSkillBulletEffect(skill);
        if (requestedEffect != BulletEffectType.NONE && armedBulletEffect == requestedEffect) {
            return;
        }

        currentFuel -= skill.getFuelCost();
        skill.activate(this);
    }

    private BulletEffectType getSkillBulletEffect(SkillType skill) {
        return switch (skill) {
            case TOXIC -> BulletEffectType.TOXIC;
            case PHASE_SHOT -> BulletEffectType.PIERCING;
            default -> BulletEffectType.NONE;
        };
    }

    private boolean canActivateSkill(SkillType skill) {
        return switch (skill) {
            case SHIELD -> !hasShield;
            case TRIPLE_SHOT -> shotCooldown == 0 && direction != Direction.NONE;
            default -> true;
        };
    }

    private int applySlowDebuff(int baseSpeed) {
        if (slowDebuffTicksRemaining <= 0) {
            return baseSpeed;
        }
        return Math.max(1, Math.round(baseSpeed * slowDebuffMultiplier));
    }

    private void updateSlowDebuffState() {
        if (slowDebuffTicksRemaining <= 0) {
            slowDebuffTicksRemaining = 0;
            slowDebuffMultiplier = 1.0f;
            return;
        }
        slowDebuffTicksRemaining--;
    }

    private void updateSlowTrailState(boolean movedThisTick) {
        Iterator<SlowTrailSegment> iterator = slowTrailSegments.iterator();
        while (iterator.hasNext()) {
            SlowTrailSegment segment = iterator.next();
            segment.lifeTicks--;
            if (segment.lifeTicks <= 0) {
                iterator.remove();
            }
        }

        if (slowDebuffTicksRemaining <= 0 || !movedThisTick) {
            return;
        }

        slowTrailSpawnCounter++;
        if (slowTrailSpawnCounter < SLOW_TRAIL_SPAWN_INTERVAL) {
            return;
        }
        slowTrailSpawnCounter = 0;

        for (int i = 0; i < SLOW_TRAIL_SEGMENTS_PER_SPAWN; i++) {
            slowTrailSegments.add(createSlowTrailSegment());
            if (slowTrailSegments.size() > SLOW_TRAIL_MAX_SEGMENTS) {
                slowTrailSegments.remove(0);
            }
        }
    }

    private SlowTrailSegment createSlowTrailSegment() {
        int centerX = solidArea.x + solidArea.width / 2;
        int centerY = solidArea.y + solidArea.height / 2;
        int sideSpread = Math.max(4, solidArea.width / 3);
        int rearOffset = Math.max(6, solidArea.height / 2);

        int baseX = centerX;
        int baseY = centerY;
        switch (direction) {
            case UP:
                baseY += rearOffset;
                baseX += (int) (Math.random() * (sideSpread * 2 + 1)) - sideSpread;
                break;
            case DOWN:
                baseY -= rearOffset;
                baseX += (int) (Math.random() * (sideSpread * 2 + 1)) - sideSpread;
                break;
            case LEFT:
                baseX += rearOffset;
                baseY += (int) (Math.random() * (sideSpread * 2 + 1)) - sideSpread;
                break;
            case RIGHT:
                baseX -= rearOffset;
                baseY += (int) (Math.random() * (sideSpread * 2 + 1)) - sideSpread;
                break;
            case NONE:
                break;
        }

        int lateralJitter = (int) (Math.random() * 9) - 4;
        int dx = switch (direction) {
            case LEFT -> 8;
            case RIGHT -> -8;
            default -> lateralJitter;
        };
        int dy = switch (direction) {
            case UP -> 8;
            case DOWN -> -8;
            default -> lateralJitter;
        };

        return new SlowTrailSegment(baseX, baseY, baseX + dx, baseY + dy, SLOW_TRAIL_SEGMENT_LIFETIME);
    }

    private void updatePoisonState() {
        if (!isPoisoned() || state == TankState.DYING || state == TankState.DEAD) {
            return;
        }

        poisonTicksRemaining--;
        poisonTickCounter++;
        poisonEffectTick++;

        if (poisonTickCounter >= TOXIC_TICK_INTERVAL) {
            poisonTickCounter = 0;
            applyPoisonTickDamage(TOXIC_DAMAGE_PER_TICK);
        }
    }

    private void applyPoisonTickDamage(int damage) {
        if (state == TankState.DYING || state == TankState.DEAD) {
            return;
        }

        currentHealth = Math.max(0, currentHealth - Math.max(1, damage));
        if (currentHealth == 0) {
            startDeath();
        }
    }

    private void updateHitState() {
        if (hitEffectTick < 0) {
            return;
        }

        hitEffectTick++;
        if (hitEffectTick >= getTotalHitAnimationTicks()) {
            hitEffectTick = -1;
        }
    }

    private void updateInvincibilityState() {
        if (invincibilityTick < 0) {
            return;
        }

        invincibilityTick++;
        if (invincibilityTick >= INVINCIBILITY_DURATION_TICKS) {
            invincibilityTick = -1;
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

    private void startInvincibility() {
        invincibilityTick = 0;
    }

    private void startDeath() {
        state = TankState.DYING;
        deathEffectTick = 0;
        hitEffectTick = -1;
        invincibilityTick = -1;
        shotCooldown = 0;
        slowTrailSegments.clear();
    }

    public void draw(Graphics2D g2) {
        if (state == TankState.DEAD) {
            return;
        }

        if (state != TankState.DYING) {
            drawUI(g2);
            drawSlowTrail(g2);
            drawTank(g2);
            drawPoisonEffect(g2);
            drawShieldEffect(g2);
            drawHitEffect(g2);
            return;
        }

        drawDeathEffect(g2);
    }

    private void drawShieldEffect(Graphics2D g2) {
        if (hasShield) {
            Graphics2D g = (Graphics2D) g2.create();
            java.awt.Stroke oldStroke = g.getStroke();

            int paddingOuter = 10;
            int paddingInner = 5;
            int xOuter = solidArea.x - paddingOuter;
            int yOuter = solidArea.y - paddingOuter;
            int wOuter = solidArea.width + paddingOuter * 2;
            int hOuter = solidArea.height + paddingOuter * 2;
            int xInner = solidArea.x - paddingInner;
            int yInner = solidArea.y - paddingInner;
            int wInner = solidArea.width + paddingInner * 2;
            int hInner = solidArea.height + paddingInner * 2;

            int shieldArc = 110;
            int centerAngle = getDirectionAngle(direction);
            int startAngle = centerAngle - shieldArc / 2;


            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g.setColor(new Color(170, 255, 255));
            g.setStroke(new BasicStroke(3));
            g.drawArc(xOuter, yOuter, wOuter, hOuter, startAngle, shieldArc);
            g.setStroke(new BasicStroke(2));
            g.drawArc(xInner, yInner, wInner, hInner, startAngle, shieldArc);

            g.setStroke(oldStroke);
            g.dispose();
        }
    }

    private int getDirectionAngle(Direction currentDirection) {
        return switch (currentDirection) {
            case RIGHT -> 0;
            case UP -> 90;
            case LEFT -> 180;
            case DOWN -> 270;
            case NONE -> 90;
        };
    }

    private void drawHitEffect(Graphics2D g2) {
        if (hitEffectTick < 0 || hitEffectFrames.length == 0) {
            return;
        }

        int frame = Math.min(hitEffectFrames.length - 1, hitEffectTick / HIT_TICKS_PER_FRAME);
        drawCenteredEffect(g2, hitEffectFrames[frame], 48 * Config.SCALE, 48 * Config.SCALE);
    }

    private void drawDeathEffect(Graphics2D g2) {
        if (deathEffectFrames.length == 0) {
            return;
        }

        int frame = Math.min(deathEffectFrames.length - 1, deathEffectTick / DEATH_TICKS_PER_FRAME);
        drawCenteredEffect(g2, deathEffectFrames[frame], 64 * Config.SCALE, 64 * Config.SCALE);
    }

    private void drawPoisonEffect(Graphics2D g2) {
        if (!isPoisoned() || poisonEffectFrames.length == 0) {
            return;
        }

        int frame = (poisonEffectTick / POISON_TICKS_PER_FRAME) % poisonEffectFrames.length;
        drawCenteredEffect(g2, poisonEffectFrames[frame], 16 * (Config.SCALE + 1), 21 * (Config.SCALE + 1));
    }

    private void drawSlowTrail(Graphics2D g2) {
        if (slowTrailSegments.isEmpty()) {
            return;
        }

        Graphics2D g = (Graphics2D) g2.create();
        Stroke oldStroke = g.getStroke();
        for (SlowTrailSegment segment : slowTrailSegments) {
            float alpha = Math.max(0f, segment.lifeTicks / (float) segment.maxLifeTicks) * 0.75f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(96, 74, 46));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(segment.x1, segment.y1, segment.x2, segment.y2);
        }
        g.setStroke(oldStroke);
        g.dispose();
    }

    private void drawCenteredEffect(Graphics2D g2, BufferedImage frame, int width, int height) {
        if (frame == null) {
            return;
        }

        int centerX = solidArea.x + solidArea.width / 2;
        int centerY = solidArea.y + solidArea.height / 2;
        int drawX = centerX - width / 2;
        int drawY = centerY - height / 2;
        g2.drawImage(frame, drawX, drawY, width, height, null);
    }

    public void drawTank(Graphics2D g2) {
        if (shouldSkipBlinkFrame()) {
            return;
        }

        Graphics2D g = (Graphics2D) g2.create();
        if (state == TankState.TAKING_DAMAGE) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, BLINK_ALPHA));
        }

        BufferedImage tank;
        String name = "PLAYER " +  playerNum;
        g.setFont(new java.awt.Font("m6x11plus", Font.PLAIN, 14));
        g.setColor(java.awt.Color.WHITE);
        int textLength = (int) g.getFontMetrics().getStringBounds(name, g).getWidth();
        int textX, textY;
        final int cooldownBar = Config.TILE_SIZE / 2;
        double cooldownBarHeight = (double)shotCooldown / type.getBulletType().getCooldown() * cooldownBar;
        switch (direction) {
            case UP:
                tank =  sprite.getSubimage(0, 0, 32, 32);
                g2.drawImage(tank, solidArea.x - type.getHitboxX() * Config.SCALE, solidArea.y - type.getHitboxY() * Config.SCALE,
                        Config.TILE_SIZE, Config.TILE_SIZE, null);

                textX = solidArea.x + (solidArea.width / 2) - (textLength / 2);
                textY = solidArea.y - type.getHitboxY() * Config.SCALE + 32 * Config.SCALE + 15;
                g.setColor(java.awt.Color.BLACK);
                g.drawString(name, textX + 1, textY + 1);
                g.setColor(java.awt.Color.WHITE);
                g.drawString(name, textX, textY);
                if (shotCooldown > 0) {
                    g.fillRect(solidArea.x - type.getHitboxX() - 15, (int)(solidArea.y - type.getHitboxY() + cooldownBarHeight),
                            5, (int)(cooldownBar - cooldownBarHeight));
                }
                break;
            case RIGHT:
                tank =  sprite.getSubimage(0, 32, 32, 32);
                g2.drawImage(tank, solidArea.x - (32 - type.getHitboxY() - type.getHitboxSize()) * Config.SCALE, solidArea.y - type.getHitboxX() * Config.SCALE,
                        Config.TILE_SIZE, Config.TILE_SIZE, null);

                textX = solidArea.x + (solidArea.width / 2) - (textLength / 2);
                textY = solidArea.y +  solidArea.height + 15;
                g.setColor(java.awt.Color.BLACK);
                g.drawString(name, textX + 1, textY + 1);
                g.setColor(java.awt.Color.WHITE);
                g.drawString(name, textX, textY);
                if (shotCooldown > 0) {
                    g.fillRect(solidArea.x - type.getHitboxX() * Config.SCALE - 10, (int)(solidArea.y + cooldownBarHeight), 5, (int)(cooldownBar - cooldownBarHeight));
                }
                break;
            case DOWN:
                tank =  sprite.getSubimage(0, 64, 32, 32);
                g2.drawImage(tank, solidArea.x - type.getHitboxX() * Config.SCALE, solidArea.y- (32 - type.getHitboxY() - type.getHitboxSize()) * Config.SCALE,
                        Config.TILE_SIZE, Config.TILE_SIZE, null);

                textX = solidArea.x + (solidArea.width / 2) - (textLength / 2);
                textY = solidArea.y - type.getHitboxY() * Config.SCALE - 5;
                g.setColor(java.awt.Color.BLACK);
                g.drawString(name, textX + 1, textY + 1);
                g.setColor(java.awt.Color.WHITE);
                g.drawString(name, textX, textY);
                if (shotCooldown > 0) {
                    g.fillRect(solidArea.x - 15, (int)(solidArea.y + cooldownBarHeight), 5, (int)(cooldownBar - cooldownBarHeight));
                }
                break;
            case LEFT:
                tank =  sprite.getSubimage(0, 96, 32, 32);
                g2.drawImage(tank, solidArea.x - type.getHitboxY() * Config.SCALE, solidArea.y - type.getHitboxX() * Config.SCALE,
                        Config.TILE_SIZE, Config.TILE_SIZE, null);

                textX = solidArea.x + (solidArea.width / 2) - (textLength / 2);
                textY = solidArea.y +  solidArea.height + 15;
                g.setColor(java.awt.Color.BLACK);
                g.drawString(name, textX + 1, textY + 1);
                g.setColor(java.awt.Color.WHITE);
                g.drawString(name, textX, textY);
                if (shotCooldown > 0) {
                    g.fillRect(solidArea.x, solidArea.y - 15, (int)(cooldownBar - cooldownBarHeight), 5);
                }
                break;
            case NONE:
                break;
        }
        g2.setColor(java.awt.Color.RED);
        g2.draw(solidArea);
    }


    public void drawUI(Graphics2D g2) {
        int numSprite = 7 - (int)(currentHealth / (double)(100/7));
        int numSpriteFuel = 8 - (int)(currentFuel / (double)(type.getFuel() / 8));
        switch (playerNum) {
            case 1:
                //bottom left
                g2.drawImage(healthStatus[numSprite], 5, Config.SCREEN_HEIGHT - 98,48, 48, null);
                g2.drawImage(fuelStatus[numSpriteFuel], 5, Config.SCREEN_HEIGHT - 48,48 , 48, null);
                g2.drawImage(slotSkill1, 52, Config.SCREEN_HEIGHT - 30, 32, 32, null);
                g2.drawImage(slotSkill2, 80, Config.SCREEN_HEIGHT - 30, 32, 32, null);
                break;
            case 2:
                //bottom right
                g2.drawImage(healthStatus[numSprite], Config.SCREEN_WIDTH - 48 - 1, Config.SCREEN_HEIGHT - 98,
                        48, 48, null);
                g2.drawImage(fuelStatus[numSpriteFuel], Config.SCREEN_WIDTH - 48 - 1, Config.SCREEN_HEIGHT - 48,
                        48, 48, null);
                g2.drawImage(slotSkill1, Config.SCREEN_WIDTH - 52 - 27 , Config.SCREEN_HEIGHT - 30, 32, 32, null);
                g2.drawImage(slotSkill2, Config.SCREEN_WIDTH - 80 - 27, Config.SCREEN_HEIGHT - 30, 32, 32, null);

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
                g2.drawImage(healthStatus[numSprite], Config.SCREEN_WIDTH - 48 - 1, 0,
                        48, 48, null);
                g2.drawImage(fuelStatus[numSpriteFuel], Config.SCREEN_WIDTH - 48 - 1, 50,
                        48, 48, null);
                g2.drawImage(slotSkill1, Config.SCREEN_WIDTH - 52 - 27 , 0, 32, 32, null);
                g2.drawImage(slotSkill2, Config.SCREEN_WIDTH - 80 - 27, 0, 32, 32, null);
                break;
        }
    }

    public boolean takeDamage(int damage, int impactX, int impactY) {
        if (!canBeDamaged()) {
            return false;
        }

        currentHealth = Math.max(0, currentHealth - damage);
        startHitEffect();

        if (currentHealth == 0) {
            startDeath();
        } else {
            startInvincibility();
        }
        return true;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public boolean canBeDamaged() {
        return state != TankState.DYING && state != TankState.DEAD && invincibilityTick < 0;
    }

    public boolean blockBulletIfPossible(Direction bulletDirection, int impactCenterX, int impactCenterY) {
        if (!hasShield) {
            return false;
        }

        Direction blockedDirection = getOppositeDirection(direction);
        if (bulletDirection != blockedDirection) {
            return false;
        }

        return true;
    }

    private Direction getOppositeDirection(Direction inputDirection) {
        return switch (inputDirection) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
            case NONE -> Direction.NONE;
        };
    }

    private Direction getLeftDirection(Direction inputDirection) {
        return switch (inputDirection) {
            case UP -> Direction.LEFT;
            case LEFT -> Direction.DOWN;
            case DOWN -> Direction.RIGHT;
            case RIGHT -> Direction.UP;
            case NONE -> Direction.NONE;
        };
    }

    private Direction getRightDirection(Direction inputDirection) {
        return switch (inputDirection) {
            case UP -> Direction.RIGHT;
            case RIGHT -> Direction.DOWN;
            case DOWN -> Direction.LEFT;
            case LEFT -> Direction.UP;
            case NONE -> Direction.NONE;
        };
    }

    public boolean isPendingRemoval() {
        return pendingRemoval;
    }

    public TankState getState() {
        return state;
    }

    private boolean shouldSkipBlinkFrame() {
        if (state != TankState.TAKING_DAMAGE || invincibilityTick < 0) {
            return false;
        }

        return (invincibilityTick / BLINK_TICKS_PER_STEP) % 2 == 1;
    }

    public int getShieldDuration() {
        return shieldDuration;
    }

    public boolean isHasShield() {
        return hasShield;
    }

    public void setShieldDuration(int shieldDuration) {
        this.shieldDuration = shieldDuration;
    }

    public void setHasShield(boolean hasShield) {
        this.hasShield = hasShield;
    }

    public void armNextBulletEffect(BulletEffectType effectType) {
        armedBulletEffect = effectType == null ? BulletEffectType.NONE : effectType;
    }

    public void activateSlowZone() {
        int centerX = solidArea.x + solidArea.width / 2;
        int centerY = solidArea.y + solidArea.height / 2;
        int radius = SLOW_RADIUS_TILES * gp.tileSize;

        for (Tank tank : gp.getTankList()) {
            if (tank == this) {
                continue;
            }
            if (tank.state == TankState.DYING || tank.state == TankState.DEAD) {
                continue;
            }
            if (isInsideRadius(tank.solidArea, centerX, centerY, radius)) {
                tank.applySlowDebuff(SLOW_DEBUFF_TICKS, SLOW_DEBUFF_MULTIPLIER);
            }
        }

        gp.addSlowZone(new SlowZone(gp, centerX, centerY, SLOW_VISUAL_TICKS, SLOW_DEBUFF_MULTIPLIER, radius));
    }

    private boolean isInsideRadius(Rectangle area, int centerX, int centerY, int radius) {
        int areaCenterX = area.x + area.width / 2;
        int areaCenterY = area.y + area.height / 2;
        int dx = areaCenterX - centerX;
        int dy = areaCenterY - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private void applySlowDebuff(int durationTicks, float multiplier) {
        if (slowDebuffTicksRemaining > 0) {
            return;
        }
        slowDebuffTicksRemaining = Math.max(slowDebuffTicksRemaining, Math.max(1, durationTicks));
        slowDebuffMultiplier = Math.min(slowDebuffMultiplier, Math.max(0.1f, multiplier));
    }

    private static class SlowTrailSegment {
        private final int x1;
        private final int y1;
        private final int x2;
        private final int y2;
        private final int maxLifeTicks;
        private int lifeTicks;

        private SlowTrailSegment(int x1, int y1, int x2, int y2, int lifeTicks) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.lifeTicks = lifeTicks;
            this.maxLifeTicks = Math.max(1, lifeTicks);
        }
    }

    public void applyPoison() {
        if (state == TankState.DYING || state == TankState.DEAD) {
            return;
        }

        poisonTicksRemaining = TOXIC_DURATION_TICKS;
        poisonTickCounter = 0;
        poisonEffectTick = 0;
    }

    public boolean isPoisoned() {
        return poisonTicksRemaining > 0;
    }
}
