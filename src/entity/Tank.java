package entity;

import item.Item;
import main.Config;
import main.GamePanel;
import main.KeyHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

public class Tank extends GameObject {
    public enum BotDifficulty {
        EASY,
        MEDIUM,
        HARD
    }

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
    private final boolean isBot;
    private final BotDifficulty botDifficulty;
    private final Random random = new Random();
    // team id used for team-based modes (1 = players, 2 = bots)
    private int teamId = 0;

    private int shotCooldown = 0;
    private int shotCooldownMax = 1;
    private TankState state = TankState.IDLE;
    private int hitEffectTick = -1;
    private int invincibilityTick = -1;
    private int deathEffectTick = 0;

    public SkillType[] skillSlots = {SkillType.NONE, SkillType.NONE};

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
    private Trap currentTrap = null;
    private Direction botMoveDirection = Direction.UP;
    private int botMoveTicksRemaining = 0;
    private int botDashTicksRemaining = 0;
    private final List<Point> botPathTiles = new ArrayList<>();
    private int botPathIndex = 0;
    private int botRepathTicks = 0;
    private Point botTargetTile = null;
    private boolean botTargetIsItem = false;
    private final Map<Tank, Point> observedEnemyTiles = new HashMap<>();

    private static final int MEDIUM_BOT_REPATH_TICKS = 30;
    private static final int MEDIUM_BOT_SHIELD_RADIUS_TILES = 5;
    private static final float MEDIUM_BOT_SHIELD_HP_RATIO = 0.5f;
    private static final int MEDIUM_BOT_DASH_ITEM_RADIUS_TILES = 6;
    private static final float MEDIUM_BOT_CHASE_HP_RATIO = 0.5f;
    private static final int MEDIUM_BOT_CHASE_RADIUS_TILES = 6;
    private static final int MEDIUM_BOT_RETREAT_SCAN_RADIUS_TILES = 6;
    private static final int HARD_BOT_FUEL_RESERVE = 8;
    private static final int HARD_BOT_PREDICTION_TICKS = Config.FPS;
    private static final int HARD_BOT_DODGE_DEPTH = 3;
    private static final int HARD_BOT_JUST_FRAME_TILE_DISTANCE = 2;
    private static final int HARD_BOT_STANDOFF_DISTANCE = 3;
    private static final float HARD_BOT_RETREAT_HP_RATIO = 0.35f;
    private static final int HARD_BOT_ATTACK_STANDOFF_DISTANCE = 2;
    private static final int EASY_BOT_MIN_WALK_TICKS = 25;
    private static final int EASY_BOT_MAX_WALK_TICKS = 90;
    private static final int EASY_BOT_DANGER_RADIUS = 3 * Config.TILE_SIZE;
    private static final int EASY_BOT_LOW_HP_THRESHOLD = 35;
    private static final float EASY_BOT_RANDOM_SHOOT_CHANCE = 0.05f;

    public Tank(GamePanel gp, KeyHandler keyH, TankType type, int playerNum, KeySetting keySetting) {
        this(gp, keyH, type, playerNum, keySetting, false);
    }

    public Tank(GamePanel gp, KeyHandler keyH, TankType type, int playerNum, KeySetting keySetting, boolean isBot) {
        this(gp, keyH, type, playerNum, keySetting, isBot, BotDifficulty.MEDIUM);
    }

    public Tank(GamePanel gp, KeyHandler keyH, TankType type, int playerNum, KeySetting keySetting, boolean isBot, BotDifficulty botDifficulty) {
        this.gp = gp;
        this.keyH = keyH;
        this.type = type;
        this.isBot = isBot;
        this.botDifficulty = botDifficulty == null ? BotDifficulty.MEDIUM : botDifficulty;

        this.maxFuel = type.getFuel();
        this.currentFuel = type.getFuel();
        this.playerNum = playerNum;
        this.keySetting = keySetting;

        healthStatus =  new BufferedImage[9];
        fuelStatus =  new BufferedImage[9];

        setDefaultValues();
        getSprite();
        refreshSkillSlotIcons();
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
        shotCooldown = 0;
        shotCooldownMax = 1;
        currentTrap = null;
        botMoveDirection = Direction.UP;
        botMoveTicksRemaining = 0;
        botDashTicksRemaining = 0;
        botPathTiles.clear();
        botPathIndex = 0;
        botRepathTicks = 0;
        botTargetTile = null;
        botTargetIsItem = false;
        observedEnemyTiles.clear();
    }

    public boolean isBot() { return isBot; }

    public int getTeamId() { return teamId; }

    public void setTeamId(int id) { this.teamId = id; }

    private static class ControlIntent {
        private Direction moveDirection = Direction.NONE;
        private boolean shoot;
        private boolean dash;
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

        if (shotCooldown > 0) {
            shotCooldown--;
        }

        int baseSpeed = type.getSpeed();
        int desiredSpeed = baseSpeed;
        ControlIntent intent = isBot
                ? switch (botDifficulty) {
                    case EASY -> buildEasyBotIntent();
                    case HARD -> buildHardBotIntent();
                    case MEDIUM -> buildMediumBotIntent();
                }
                : buildHumanIntent();

        if (intent.dash && currentFuel > 0) {
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

        if (isBot) {
            if (botDifficulty == BotDifficulty.EASY) {
                tryActivateEasyPanicShield();
            } else if (botDifficulty == BotDifficulty.MEDIUM) {
                tryActivateBotUtilitySkills();
            }
        } else {
            handleSkillInput(keySetting.getKeySkill1(), skillSlots[0]);
            handleSkillInput(keySetting.getKeySkill2(), skillSlots[1]);
        }

        speed = applySlowDebuff(desiredSpeed);

        Direction inputDirection = intent.moveDirection;
        boolean movedThisTick = false;

        if (currentTrap != null && !currentTrap.isExpired()) {
            inputDirection = Direction.NONE;
        }

        if (inputDirection != Direction.NONE) {
            if (state != TankState.TAKING_DAMAGE) {
                state = TankState.MOVING;
            }
            direction = inputDirection;
            movedThisTick = movePixelByPixel(direction, speed);
            if (isBot && !movedThisTick) {
                botMoveTicksRemaining = 0;
            }
        } else if (state == TankState.MOVING) {
            state = TankState.IDLE;
        }

        updateSlowTrailState(movedThisTick);

        if (intent.shoot && shotCooldown == 0) {
            shoot();
        }
    }

    private ControlIntent buildHumanIntent() {
        ControlIntent intent = new ControlIntent();
        intent.moveDirection = readInputDirection();
        intent.shoot = keyH.isPressed(keySetting.getKeyShoot());
        intent.dash = keyH.isPressed(keySetting.getKeyDash());
        return intent;
    }

    private ControlIntent buildEasyBotIntent() {
        ControlIntent intent = new ControlIntent();
        Direction incomingDirection = detectIncomingBulletDirection(EASY_BOT_DANGER_RADIUS);

        if (incomingDirection != Direction.NONE) {
            Direction opposite = getOppositeDirection(incomingDirection);
            if (!isDirectionBlocked(opposite)) {
                botMoveDirection = opposite;
            } else {
                botMoveDirection = randomDirectionExcluding(opposite);
            }
            botMoveTicksRemaining = 8 + random.nextInt(16);
        } else {
            if (botMoveTicksRemaining <= 0 || isDirectionBlocked(botMoveDirection)) {
                botMoveDirection = randomDirectionExcluding(Direction.NONE);
                botMoveTicksRemaining = EASY_BOT_MIN_WALK_TICKS
                        + random.nextInt(EASY_BOT_MAX_WALK_TICKS - EASY_BOT_MIN_WALK_TICKS + 1);
            }
            botMoveTicksRemaining--;
        }

        // Panic dash wastes fuel aggressively and can slam into walls.
        if (botDashTicksRemaining <= 0
                && currentFuel > 0
                && (currentHealth <= EASY_BOT_LOW_HP_THRESHOLD || incomingDirection != Direction.NONE)
                && random.nextFloat() < 0.35f) {
            botDashTicksRemaining = 40 + random.nextInt(81);
        }

        intent.moveDirection = botMoveDirection;
        intent.dash = botDashTicksRemaining > 0 && currentFuel > 0;
        intent.shoot = incomingDirection != Direction.NONE || random.nextFloat() < EASY_BOT_RANDOM_SHOOT_CHANCE;

        if (intent.dash) {
            botDashTicksRemaining--;
        }

        return intent;
    }

    private ControlIntent buildMediumBotIntent() {
        ControlIntent intent = new ControlIntent();
        Point selfTile = getCurrentTile();
        Tank nearestEnemy = findNearestEnemyTank(selfTile);
        Point nearestEnemyTile = nearestEnemy == null ? null : toTileCenter(nearestEnemy.getSolidArea());
        int enemyDistance = nearestEnemyTile == null ? -1 : manhattanDistance(selfTile, nearestEnemyTile);

        int chaseHpThreshold = Math.max(1, (int) Math.ceil(Config.MAX_HEALTH * MEDIUM_BOT_CHASE_HP_RATIO));
        boolean isLowHp = currentHealth <= chaseHpThreshold;
        boolean shouldRetreat = nearestEnemyTile != null && currentHealth <= chaseHpThreshold;
        boolean shouldChase = shouldChaseEnemy(enemyDistance, chaseHpThreshold);

        Point retreatTarget = shouldRetreat ? findRetreatTargetTile(selfTile, nearestEnemyTile) : null;
        Point itemTarget = isLowHp
                ? findBestHealthItemTile(selfTile, nearestEnemyTile)
                : ((!shouldRetreat && hasEmptySkillSlot()) ? findNearestItemTile(selfTile) : null);
        Point enemyTarget = shouldChase ? findStandoffTargetTile(selfTile, nearestEnemyTile, MEDIUM_BOT_CHASE_RADIUS_TILES) : null;
        if (shouldChase && enemyTarget == null) {
            enemyTarget = nearestEnemyTile;
        }

        Point desiredTarget = isLowHp
                ? (itemTarget != null ? itemTarget : retreatTarget)
                : (retreatTarget != null ? retreatTarget : (itemTarget != null ? itemTarget : enemyTarget));
        botTargetIsItem = desiredTarget != null && desiredTarget.equals(itemTarget);

        if (desiredTarget != null) {
            boolean targetChanged = botTargetTile == null || !botTargetTile.equals(desiredTarget);
            if (targetChanged || botRepathTicks <= 0 || botPathIndex >= botPathTiles.size()) {
                botTargetTile = desiredTarget;
                rebuildPath(selfTile, desiredTarget);
                botRepathTicks = MEDIUM_BOT_REPATH_TICKS;
            } else {
                botRepathTicks--;
            }
        } else {
            botTargetTile = null;
            botPathTiles.clear();
            botPathIndex = 0;
        }

        Direction moveDirection = nextDirectionFromPath(selfTile);
        if (moveDirection == Direction.NONE) {
            moveDirection = randomDirectionExcluding(Direction.NONE);
        }
        botMoveDirection = moveDirection;
        intent.moveDirection = botMoveDirection;

        Tank losEnemy = findLineOfSightEnemy(selfTile);
        if (losEnemy != null) {
            Point enemyTile = toTileCenter(losEnemy.getSolidArea());
            Direction attackDirection = directionToTargetTile(selfTile, enemyTile);
            if (attackDirection != Direction.NONE) {
                direction = attackDirection;
                intent.shoot = true;
            }
        }

        if (botTargetIsItem && botTargetTile != null && currentFuel > 0) {
            int myItemDistance = manhattanDistance(selfTile, botTargetTile);
            int nearestEnemyDistance = nearestEnemyDistanceToTile(botTargetTile);
            boolean raceCondition = nearestEnemyDistance >= 0 && nearestEnemyDistance <= myItemDistance + 1;
            boolean closeItem = myItemDistance <= MEDIUM_BOT_DASH_ITEM_RADIUS_TILES;
            if (raceCondition && closeItem) {
                botDashTicksRemaining = Math.max(botDashTicksRemaining, 12);
            }
        }

        if (shouldRetreat && currentFuel > 0 && enemyDistance >= 0 && enemyDistance <= MEDIUM_BOT_SHIELD_RADIUS_TILES) {
            botDashTicksRemaining = Math.max(botDashTicksRemaining, 10);
        }

        intent.dash = botDashTicksRemaining > 0 && currentFuel > 0;
        if (intent.dash && random.nextFloat() < 0.75f) {
            botDashTicksRemaining--;
        }

        return intent;
    }

    private ControlIntent buildHardBotIntent() {
        ControlIntent intent = new ControlIntent();
        Point selfTile = getCurrentTile();
        Tank nearestEnemy = findNearestEnemyTank(selfTile);
        Point enemyTile = nearestEnemy == null ? null : toTileCenter(nearestEnemy.getSolidArea());
        Point predictedEnemyTile = predictEnemyFutureTile(nearestEnemy, enemyTile);

        int hpThreshold = Math.max(1, (int) Math.ceil(Config.MAX_HEALTH * HARD_BOT_RETREAT_HP_RATIO));
        boolean lowHp = currentHealth <= hpThreshold;

        BulletThreat imminentThreat = detectImminentBulletThreat(selfTile, HARD_BOT_JUST_FRAME_TILE_DISTANCE + 1);
        if (imminentThreat != null && imminentThreat.distanceTiles <= HARD_BOT_JUST_FRAME_TILE_DISTANCE) {
            if (tryActivateBotSkill(SkillType.SHIELD, true)) {
                // just-frame shield
            }
            if (canSpendFuel(1)) {
                intent.dash = true;
            }
        }

        if (lowHp) {
            Point healthTile = findBestHealthItemTile(selfTile, enemyTile);
            Point retreatTile = enemyTile == null ? null : findRetreatTargetTile(selfTile, enemyTile);
            Point target = healthTile != null ? healthTile : retreatTile;
            botTargetIsItem = target != null && target.equals(healthTile);
            updateHardPathAndMove(target, selfTile, intent);
            if (enemyTile != null && canSpendFuel(1) && manhattanDistance(selfTile, enemyTile) <= MEDIUM_BOT_SHIELD_RADIUS_TILES) {
                intent.dash = true;
            }
        } else {
            Point standoffTarget = enemyTile == null ? null : findStandoffTargetTile(selfTile, enemyTile, HARD_BOT_ATTACK_STANDOFF_DISTANCE);
            Point skillItemTarget = hasEmptySkillSlot() ? findNearestItemTile(selfTile) : null;
            Point target = standoffTarget != null ? standoffTarget : skillItemTarget;
            botTargetIsItem = target != null && target.equals(skillItemTarget);
            updateHardPathAndMove(target, selfTile, intent);

            if (enemyTile != null && canSpendFuel(1)) {
                int engageDistance = manhattanDistance(selfTile, enemyTile);
                if (engageDistance > HARD_BOT_ATTACK_STANDOFF_DISTANCE + 1) {
                    intent.dash = true;
                }
            }
        }

        if (enemyTile != null) {
            Direction dodgeDirection = chooseDodgeDirectionAlphaBeta(selfTile, HARD_BOT_DODGE_DEPTH);
            if (dodgeDirection != Direction.NONE && imminentThreat != null && imminentThreat.distanceTiles <= HARD_BOT_JUST_FRAME_TILE_DISTANCE + 1) {
                intent.moveDirection = dodgeDirection;
            }
        }

        if (nearestEnemy != null) {
            tryHardComboSkills(nearestEnemy, enemyTile);
        }

        Point shootingTile = predictedEnemyTile != null ? predictedEnemyTile : enemyTile;
        if (shootingTile != null && hasLineOfSightToTile(selfTile, shootingTile)) {
            Direction attackDirection = directionToTargetTile(selfTile, shootingTile);
            if (attackDirection != Direction.NONE) {
                direction = attackDirection;
                intent.shoot = true;
            }
        } else {
            Tank losEnemy = findLineOfSightEnemy(selfTile);
            if (losEnemy != null) {
                Point losTile = toTileCenter(losEnemy.getSolidArea());
                Direction attackDirection = directionToTargetTile(selfTile, losTile);
                if (attackDirection != Direction.NONE) {
                    direction = attackDirection;
                    intent.shoot = true;
                }
            }
        }

        // Keep reserve fuel unless it is an imminent survival case.
        if (intent.dash && !canSpendFuel(1) && (imminentThreat == null || imminentThreat.distanceTiles > HARD_BOT_JUST_FRAME_TILE_DISTANCE)) {
            intent.dash = false;
        }

        return intent;
    }

    private void updateHardPathAndMove(Point target, Point selfTile, ControlIntent intent) {
        if (target != null) {
            boolean targetChanged = botTargetTile == null || !botTargetTile.equals(target);
            if (targetChanged || botRepathTicks <= 0 || botPathIndex >= botPathTiles.size()) {
                botTargetTile = target;
                rebuildPathAStar(selfTile, target);
                botRepathTicks = MEDIUM_BOT_REPATH_TICKS;
            } else {
                botRepathTicks--;
            }
        } else {
            botTargetTile = null;
            botPathTiles.clear();
            botPathIndex = 0;
        }

        Direction moveDirection = nextDirectionFromPath(selfTile);
        if (moveDirection == Direction.NONE) {
            moveDirection = randomDirectionExcluding(Direction.NONE);
        }
        botMoveDirection = moveDirection;
        intent.moveDirection = botMoveDirection;
    }

    private boolean shouldChaseEnemy(int enemyDistance, int chaseHpThreshold) {
        return currentHealth > chaseHpThreshold
                && enemyDistance >= 0;
    }

    private Point findStandoffTargetTile(Point selfTile, Point enemyTile, int desiredDistance) {
        if (selfTile == null || enemyTile == null) {
            return null;
        }

        Point best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int col = 0; col < Config.MAX_SCREEN_COL; col++) {
            for (int row = 0; row < Config.MAX_SCREEN_ROW; row++) {
                if (!isTileWalkable(col, row, null)) {
                    continue;
                }

                int distanceToEnemy = manhattanDistance(col, row, enemyTile.x, enemyTile.y);
                if (distanceToEnemy != desiredDistance) {
                    continue;
                }

                int distanceToSelf = manhattanDistance(col, row, selfTile.x, selfTile.y);
                if (distanceToSelf < bestScore) {
                    bestScore = distanceToSelf;
                    best = new Point(col, row);
                }
            }
        }

        return best;
    }

    private Point findRetreatTargetTile(Point selfTile, Point enemyTile) {
        if (selfTile == null || enemyTile == null) {
            return null;
        }

        Point best = null;
        int bestScore = Integer.MIN_VALUE;
        int currentEnemyDistance = manhattanDistance(selfTile, enemyTile);

        for (int col = 0; col < Config.MAX_SCREEN_COL; col++) {
            for (int row = 0; row < Config.MAX_SCREEN_ROW; row++) {
                Point candidate = new Point(col, row);
                int selfDistance = manhattanDistance(selfTile, candidate);
                if (selfDistance <= 0 || selfDistance > MEDIUM_BOT_RETREAT_SCAN_RADIUS_TILES) {
                    continue;
                }
                if (!isTileWalkable(col, row, null)) {
                    continue;
                }

                int enemyDistance = manhattanDistance(enemyTile, candidate);
                if (enemyDistance <= currentEnemyDistance) {
                    continue;
                }

                int score = enemyDistance * 10 - selfDistance;
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }

        return best;
    }

    private boolean hasEmptySkillSlot() {
        for (SkillType skill : skillSlots) {
            if (skill == SkillType.NONE) {
                return true;
            }
        }
        return false;
    }

    private void tryActivateEasyPanicShield() {
        if (hasShield || currentHealth > EASY_BOT_LOW_HP_THRESHOLD) {
            return;
        }

        for (int i = 0; i < skillSlots.length; i++) {
            if (skillSlots[i] != SkillType.SHIELD) {
                continue;
            }
            if (currentFuel < SkillType.SHIELD.getFuelCost()) {
                return;
            }
            if (!canActivateSkill(SkillType.SHIELD)) {
                return;
            }

            currentFuel -= SkillType.SHIELD.getFuelCost();
            SkillType.SHIELD.activate(this);
            skillSlots[i] = SkillType.NONE;
            refreshSkillSlotIcons();
            return;
        }
    }

    private void tryActivateBotUtilitySkills() {
        if (hasShield) {
            return;
        }

        int shieldHealthThreshold = Math.max(1, (int) Math.ceil(Config.MAX_HEALTH * MEDIUM_BOT_SHIELD_HP_RATIO));
        if (currentHealth > shieldHealthThreshold) {
            return;
        }

        Point selfTile = getCurrentTile();
        Tank nearestEnemy = findNearestEnemyTank(selfTile);
        if (nearestEnemy == null) {
            return;
        }

        int enemyDistance = manhattanDistance(selfTile, toTileCenter(nearestEnemy.getSolidArea()));
        if (enemyDistance > MEDIUM_BOT_SHIELD_RADIUS_TILES) {
            return;
        }

        for (int i = 0; i < skillSlots.length; i++) {
            if (skillSlots[i] != SkillType.SHIELD) {
                continue;
            }
            if (currentFuel < SkillType.SHIELD.getFuelCost()) {
                return;
            }
            if (!canActivateSkill(SkillType.SHIELD)) {
                return;
            }

            currentFuel -= SkillType.SHIELD.getFuelCost();
            SkillType.SHIELD.activate(this);
            skillSlots[i] = SkillType.NONE;
            refreshSkillSlotIcons();
            return;
        }
    }

    private void rebuildPath(Point startTile, Point targetTile) {
        botPathTiles.clear();
        botPathTiles.addAll(bestFirstPath(startTile, targetTile));
        botPathIndex = 0;
    }

    private void rebuildPathAStar(Point startTile, Point targetTile) {
        botPathTiles.clear();
        botPathTiles.addAll(aStarPath(startTile, targetTile));
        botPathIndex = 0;
    }

    private List<Point> aStarPath(Point startTile, Point targetTile) {
        List<Point> empty = new ArrayList<>();
        if (startTile == null || targetTile == null || startTile.equals(targetTile)) {
            return empty;
        }

        int cols = Config.MAX_SCREEN_COL;
        int rows = Config.MAX_SCREEN_ROW;
        int[][] gScore = new int[cols][rows];
        boolean[][] visited = new boolean[cols][rows];
        Point[][] parent = new Point[cols][rows];
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                gScore[x][y] = Integer.MAX_VALUE;
            }
        }

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.fScore));
        gScore[startTile.x][startTile.y] = 0;
        openSet.add(new AStarNode(startTile.x, startTile.y, manhattanDistance(startTile, targetTile)));

        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            if (!isValidTile(current.x, current.y) || visited[current.x][current.y]) {
                continue;
            }
            visited[current.x][current.y] = true;

            if (current.x == targetTile.x && current.y == targetTile.y) {
                return reconstructPath(startTile, targetTile, parent);
            }

            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];
                if (!isTileWalkable(nx, ny, targetTile)) {
                    continue;
                }
                int tentativeG = gScore[current.x][current.y] + 1;
                if (tentativeG >= gScore[nx][ny]) {
                    continue;
                }

                gScore[nx][ny] = tentativeG;
                parent[nx][ny] = new Point(current.x, current.y);
                int fScore = tentativeG + manhattanDistance(nx, ny, targetTile.x, targetTile.y);
                openSet.add(new AStarNode(nx, ny, fScore));
            }
        }

        return empty;
    }

    private List<Point> bestFirstPath(Point startTile, Point targetTile) {
        List<Point> empty = new ArrayList<>();
        if (startTile == null || targetTile == null) {
            return empty;
        }
        if (startTile.equals(targetTile)) {
            return empty;
        }

        boolean[][] visited = new boolean[Config.MAX_SCREEN_COL][Config.MAX_SCREEN_ROW];
        Point[][] parent = new Point[Config.MAX_SCREEN_COL][Config.MAX_SCREEN_ROW];
        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingInt(node -> node.heuristic));
        openSet.add(new PathNode(startTile.x, startTile.y, manhattanDistance(startTile, targetTile)));

        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();
            if (!isValidTile(current.x, current.y) || visited[current.x][current.y]) {
                continue;
            }

            visited[current.x][current.y] = true;
            if (current.x == targetTile.x && current.y == targetTile.y) {
                return reconstructPath(startTile, targetTile, parent);
            }

            for (int i = 0; i < 4; i++) {
                int nextX = current.x + dx[i];
                int nextY = current.y + dy[i];
                if (!isTileWalkable(nextX, nextY, targetTile)) {
                    continue;
                }
                if (!isValidTile(nextX, nextY) || visited[nextX][nextY]) {
                    continue;
                }

                if (parent[nextX][nextY] == null) {
                    parent[nextX][nextY] = new Point(current.x, current.y);
                }
                openSet.add(new PathNode(nextX, nextY, manhattanDistance(nextX, nextY, targetTile.x, targetTile.y)));
            }
        }

        return empty;
    }

    private List<Point> reconstructPath(Point startTile, Point targetTile, Point[][] parent) {
        List<Point> reversed = new ArrayList<>();
        Point current = new Point(targetTile);

        while (!current.equals(startTile)) {
            reversed.add(new Point(current));
            Point prev = parent[current.x][current.y];
            if (prev == null) {
                return new ArrayList<>();
            }
            current = prev;
        }

        Collections.reverse(reversed);
        return reversed;
    }

    private Direction nextDirectionFromPath(Point selfTile) {
        if (botPathTiles.isEmpty()) {
            return Direction.NONE;
        }

        while (botPathIndex < botPathTiles.size()) {
            Point nextTile = botPathTiles.get(botPathIndex);
            if (nextTile.equals(selfTile)) {
                botPathIndex++;
                continue;
            }

            Direction nextDirection = directionToTargetTile(selfTile, nextTile);
            if (nextDirection == Direction.NONE) {
                botPathIndex++;
                continue;
            }

            if (isDirectionBlocked(nextDirection)) {
                botPathTiles.clear();
                botPathIndex = 0;
                return Direction.NONE;
            }

            return nextDirection;
        }

        return Direction.NONE;
    }

    private Point findNearestItemTile(Point fromTile) {
        Point best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Item item : gp.itemList) {
            Point itemTile = toTileCenter(item.getSolidArea());
            int distance = manhattanDistance(fromTile, itemTile);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = itemTile;
            }
        }

        return best;
    }

    private Point findBestHealthItemTile(Point selfTile, Point enemyTile) {
        Point best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Item item : gp.itemList) {
            if (item.getItemType() != Item.ItemType.HEALTH) {
                continue;
            }

            Point itemTile = toTileCenter(item.getSolidArea());
            int selfDistance = manhattanDistance(selfTile, itemTile);
            int enemyDistance = enemyTile == null ? MEDIUM_BOT_RETREAT_SCAN_RADIUS_TILES : manhattanDistance(enemyTile, itemTile);

            // Prioritize safety first (farther from enemy), then approach cost.
            int score = enemyDistance * 100 - selfDistance;
            if (score > bestScore) {
                bestScore = score;
                best = itemTile;
            }
        }

        return best;
    }

    private Point predictEnemyFutureTile(Tank enemy, Point enemyTile) {
        if (enemy == null || enemyTile == null) {
            return null;
        }

        Point previous = observedEnemyTiles.get(enemy);
        observedEnemyTiles.put(enemy, new Point(enemyTile));
        if (previous == null) {
            return enemyTile;
        }

        int vx = enemyTile.x - previous.x;
        int vy = enemyTile.y - previous.y;
        int steps = Math.max(1, HARD_BOT_PREDICTION_TICKS / Math.max(1, Config.FPS / 2));
        int predictedX = enemyTile.x + vx * steps;
        int predictedY = enemyTile.y + vy * steps;
        predictedX = Math.max(0, Math.min(Config.MAX_SCREEN_COL - 1, predictedX));
        predictedY = Math.max(0, Math.min(Config.MAX_SCREEN_ROW - 1, predictedY));
        return new Point(predictedX, predictedY);
    }

    private boolean hasLineOfSightToTile(Point fromTile, Point targetTile) {
        if (fromTile == null || targetTile == null) {
            return false;
        }
        if (!isSameRowOrColumn(fromTile, targetTile)) {
            return false;
        }
        return !isSteelWallBlockingLine(fromTile, targetTile);
    }

    private void tryHardComboSkills(Tank enemy, Point enemyTile) {
        if (enemy == null || enemyTile == null) {
            return;
        }

        int enemyDistance = manhattanDistance(getCurrentTile(), enemyTile);
        boolean enemyInCorner = isNearCorner(enemyTile) || isNarrowAround(enemy.getSolidArea()) || enemyDistance <= 4;
        if (!enemyInCorner) {
            return;
        }

        tryActivateBotSkill(SkillType.SLOW, false);
        if (hasLineOfSightToTile(getCurrentTile(), enemyTile)) {
            tryActivateBotSkill(SkillType.TOXIC, false);
        }
    }

    private boolean isNearCorner(Point tile) {
        return tile.x <= 1 || tile.y <= 1 || tile.x >= Config.MAX_SCREEN_COL - 2 || tile.y >= Config.MAX_SCREEN_ROW - 2;
    }

    private boolean isNarrowAround(Rectangle area) {
        Point center = toTileCenter(area);
        int blocked = 0;
        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};
        for (int i = 0; i < 4; i++) {
            int nx = center.x + dx[i];
            int ny = center.y + dy[i];
            if (!isTileWalkable(nx, ny, null)) {
                blocked++;
            }
        }
        return blocked >= 2;
    }

    private boolean tryActivateBotSkill(SkillType desiredSkill, boolean critical) {
        if (desiredSkill == null || desiredSkill == SkillType.NONE) {
            return false;
        }

        for (int i = 0; i < skillSlots.length; i++) {
            if (skillSlots[i] != desiredSkill) {
                continue;
            }

            int cost = desiredSkill.getFuelCost();
            if (!critical && !canSpendFuel(cost)) {
                return false;
            }
            if (currentFuel < cost || !canActivateSkill(desiredSkill)) {
                return false;
            }

            currentFuel -= cost;
            desiredSkill.activate(this);
            skillSlots[i] = SkillType.NONE;
            refreshSkillSlotIcons();
            return true;
        }

        return false;
    }

    private boolean canSpendFuel(int amount) {
        return currentFuel - Math.max(0, amount) >= HARD_BOT_FUEL_RESERVE;
    }

    private BulletThreat detectImminentBulletThreat(Point selfTile, int maxDistanceTiles) {
        if (selfTile == null) {
            return null;
        }

        BulletThreat best = null;
        for (Bullet bullet : gp.getBulletList()) {
            if (!bullet.isAlive() || bullet.getOwner() == this) {
                continue;
            }

            Point bulletTile = toTileCenter(bullet.getSolidArea());
            Direction dir = bullet.getDirection();
            int distance = incomingDistanceTiles(selfTile, bulletTile, dir);
            if (distance < 0 || distance > maxDistanceTiles) {
                continue;
            }

            if (best == null || distance < best.distanceTiles) {
                best = new BulletThreat(dir, distance, bulletTile);
            }
        }

        return best;
    }

    private Direction detectIncomingBulletDirection(int detectionRadiusPixels) {
        int centerX = solidArea.x + solidArea.width / 2;
        int centerY = solidArea.y + solidArea.height / 2;
        int radiusSq = detectionRadiusPixels * detectionRadiusPixels;
        int axisTolerance = Math.max(8, gp.tileSize / 3);

        for (Bullet bullet : gp.getBulletList()) {
            if (!bullet.isAlive() || bullet.getOwner() == this) {
                continue;
            }

            Rectangle bulletArea = bullet.getSolidArea();
            int bx = bulletArea.x + bulletArea.width / 2;
            int by = bulletArea.y + bulletArea.height / 2;
            int dx = centerX - bx;
            int dy = centerY - by;
            if (dx * dx + dy * dy > radiusSq) {
                continue;
            }

            Direction bd = bullet.getDirection();
            switch (bd) {
                case UP:
                    if (by > centerY && Math.abs(bx - centerX) <= axisTolerance) {
                        return bd;
                    }
                    break;
                case DOWN:
                    if (by < centerY && Math.abs(bx - centerX) <= axisTolerance) {
                        return bd;
                    }
                    break;
                case LEFT:
                    if (bx > centerX && Math.abs(by - centerY) <= axisTolerance) {
                        return bd;
                    }
                    break;
                case RIGHT:
                    if (bx < centerX && Math.abs(by - centerY) <= axisTolerance) {
                        return bd;
                    }
                    break;
                case NONE:
                    break;
            }
        }

        return Direction.NONE;
    }

    private int incomingDistanceTiles(Point selfTile, Point bulletTile, Direction bulletDirection) {
        if (selfTile == null || bulletTile == null || bulletDirection == Direction.NONE) {
            return -1;
        }

        switch (bulletDirection) {
            case UP:
                if (bulletTile.x == selfTile.x && bulletTile.y > selfTile.y) {
                    return bulletTile.y - selfTile.y;
                }
                break;
            case DOWN:
                if (bulletTile.x == selfTile.x && bulletTile.y < selfTile.y) {
                    return selfTile.y - bulletTile.y;
                }
                break;
            case LEFT:
                if (bulletTile.y == selfTile.y && bulletTile.x > selfTile.x) {
                    return bulletTile.x - selfTile.x;
                }
                break;
            case RIGHT:
                if (bulletTile.y == selfTile.y && bulletTile.x < selfTile.x) {
                    return selfTile.x - bulletTile.x;
                }
                break;
            case NONE:
                break;
        }

        return -1;
    }

    private Direction chooseDodgeDirectionAlphaBeta(Point selfTile, int depth) {
        List<Direction> actions = possibleActions(selfTile);
        double bestScore = Double.NEGATIVE_INFINITY;
        Direction bestDirection = Direction.NONE;

        for (Direction action : actions) {
            Point next = simulateStep(selfTile, action);
            double score = alphaBeta(next, depth - 1, false, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            if (score > bestScore) {
                bestScore = score;
                bestDirection = action;
            }
        }

        return bestDirection;
    }

    private double alphaBeta(Point stateTile, int depth, boolean maximizing, double alpha, double beta) {
        if (depth <= 0) {
            return evaluateTileSafety(stateTile);
        }

        if (maximizing) {
            double value = Double.NEGATIVE_INFINITY;
            for (Direction action : possibleActions(stateTile)) {
                value = Math.max(value, alphaBeta(simulateStep(stateTile, action), depth - 1, false, alpha, beta));
                alpha = Math.max(alpha, value);
                if (alpha >= beta) {
                    break;
                }
            }
            return value;
        }

        // Adversarial step: assume bullets advance to minimize safety.
        double value = Double.POSITIVE_INFINITY;
        for (Direction action : possibleActions(stateTile)) {
            value = Math.min(value, alphaBeta(simulateStep(stateTile, action), depth - 1, true, alpha, beta) - projectedBulletRisk(stateTile));
            beta = Math.min(beta, value);
            if (alpha >= beta) {
                break;
            }
        }
        return value;
    }

    private List<Direction> possibleActions(Point tile) {
        List<Direction> result = new ArrayList<>();
        if (tile == null) {
            return result;
        }

        Direction[] dirs = {Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT, Direction.NONE};
        for (Direction dir : dirs) {
            Point next = simulateStep(tile, dir);
            if (next != null) {
                result.add(dir);
            }
        }
        return result;
    }

    private Point simulateStep(Point tile, Direction directionStep) {
        if (tile == null) {
            return null;
        }
        int nx = tile.x;
        int ny = tile.y;
        switch (directionStep) {
            case UP -> ny--;
            case DOWN -> ny++;
            case LEFT -> nx--;
            case RIGHT -> nx++;
            case NONE -> {
            }
        }

        if (directionStep == Direction.NONE) {
            return tile;
        }

        return isTileWalkable(nx, ny, null) ? new Point(nx, ny) : null;
    }

    private double evaluateTileSafety(Point tile) {
        if (tile == null) {
            return -1000;
        }

        double score = 0;
        for (Bullet bullet : gp.getBulletList()) {
            if (!bullet.isAlive() || bullet.getOwner() == this) {
                continue;
            }
            Point bulletTile = toTileCenter(bullet.getSolidArea());
            int distance = manhattanDistance(tile, bulletTile);
            score -= 10.0 / Math.max(1, distance);
        }

        Tank enemy = findNearestEnemyTank(tile);
        if (enemy != null) {
            Point enemyTile = toTileCenter(enemy.getSolidArea());
            int dist = manhattanDistance(tile, enemyTile);
            score -= Math.abs(dist - HARD_BOT_STANDOFF_DISTANCE) * 0.4;
        }

        return score;
    }

    private double projectedBulletRisk(Point tile) {
        if (tile == null) {
            return 0;
        }
        double risk = 0;
        for (Bullet bullet : gp.getBulletList()) {
            if (!bullet.isAlive() || bullet.getOwner() == this) {
                continue;
            }
            Point b = toTileCenter(bullet.getSolidArea());
            int distance = incomingDistanceTiles(tile, b, bullet.getDirection());
            if (distance >= 0 && distance <= 3) {
                risk += (4 - distance);
            }
        }
        return risk;
    }

    private Point findNearestEnemyTile(Point fromTile) {
        Tank enemy = findNearestEnemyTank(fromTile);
        if (enemy == null) {
            return null;
        }
        return toTileCenter(enemy.getSolidArea());
    }

    private Tank findNearestEnemyTank(Point fromTile) {
        Tank bestTank = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Tank tank : gp.getTankList()) {
            if (tank == this || tank.isPendingRemoval()) {
                continue;
            }
            if (tank.getState() == TankState.DYING || tank.getState() == TankState.DEAD) {
                continue;
            }

            int distance = manhattanDistance(fromTile, toTileCenter(tank.getSolidArea()));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTank = tank;
            }
        }

        return bestTank;
    }

    private Tank findLineOfSightEnemy(Point selfTile) {
        Tank bestEnemy = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Tank tank : gp.getTankList()) {
            if (tank == this || tank.isPendingRemoval()) {
                continue;
            }
            if (tank.getState() == TankState.DYING || tank.getState() == TankState.DEAD) {
                continue;
            }

            Point enemyTile = toTileCenter(tank.getSolidArea());
            if (!isSameRowOrColumn(selfTile, enemyTile)) {
                continue;
            }
            if (isSteelWallBlockingLine(selfTile, enemyTile)) {
                continue;
            }

            int distance = manhattanDistance(selfTile, enemyTile);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestEnemy = tank;
            }
        }

        return bestEnemy;
    }

    private int nearestEnemyDistanceToTile(Point tile) {
        int bestDistance = Integer.MAX_VALUE;

        for (Tank tank : gp.getTankList()) {
            if (tank == this || tank.isPendingRemoval()) {
                continue;
            }
            if (tank.getState() == TankState.DYING || tank.getState() == TankState.DEAD) {
                continue;
            }

            int distance = manhattanDistance(toTileCenter(tank.getSolidArea()), tile);
            bestDistance = Math.min(bestDistance, distance);
        }

        return bestDistance == Integer.MAX_VALUE ? -1 : bestDistance;
    }

    private Point getCurrentTile() {
        return toTileCenter(solidArea);
    }

    private Point toTileCenter(Rectangle area) {
        int centerX = area.x + area.width / 2;
        int centerY = area.y + area.height / 2;
        return new Point(Math.floorDiv(centerX, gp.tileSize), Math.floorDiv(centerY, gp.tileSize));
    }

    private int manhattanDistance(Point a, Point b) {
        return manhattanDistance(a.x, a.y, b.x, b.y);
    }

    private int manhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private Direction directionToTargetTile(Point fromTile, Point toTile) {
        int dx = toTile.x - fromTile.x;
        int dy = toTile.y - fromTile.y;

        if (Math.abs(dx) >= Math.abs(dy) && dx != 0) {
            return dx > 0 ? Direction.RIGHT : Direction.LEFT;
        }
        if (dy != 0) {
            return dy > 0 ? Direction.DOWN : Direction.UP;
        }
        return Direction.NONE;
    }

    private Direction randomDirectionExcluding(Direction excludedDirection) {
        Direction[] allDirections = {Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT};
        Direction fallback = Direction.UP;
        boolean fallbackSet = false;

        for (int attempt = 0; attempt < allDirections.length * 2; attempt++) {
            Direction candidate = allDirections[random.nextInt(allDirections.length)];
            if (candidate == excludedDirection) {
                continue;
            }
            if (!isDirectionBlocked(candidate)) {
                return candidate;
            }
            if (!fallbackSet) {
                fallback = candidate;
                fallbackSet = true;
            }
        }

        return fallbackSet ? fallback : Direction.UP;
    }

    private boolean isValidTile(int col, int row) {
        return col >= 0 && row >= 0 && col < Config.MAX_SCREEN_COL && row < Config.MAX_SCREEN_ROW;
    }

    private boolean isTileWalkable(int col, int row, Point targetTile) {
        if (!isValidTile(col, row)) {
            return false;
        }

        if (targetTile != null && targetTile.x == col && targetTile.y == row) {
            return true;
        }

        int tileId = gp.getTileManager().getTileIdAt(col, row);
        tile.Tile tile = gp.getTileManager().getTile(tileId);
        if (tile != null && tile.isCollision()) {
            return false;
        }

        for (Tank other : gp.getTankList()) {
            if (other == this || other.isPendingRemoval()) {
                continue;
            }
            if (other.getState() == TankState.DYING || other.getState() == TankState.DEAD) {
                continue;
            }

            Point occupied = toTileCenter(other.getSolidArea());
            if (occupied.x == col && occupied.y == row) {
                return false;
            }
        }

        return true;
    }

    private boolean isSameRowOrColumn(Point a, Point b) {
        return a.x == b.x || a.y == b.y;
    }

    private boolean isSteelWallBlockingLine(Point fromTile, Point toTile) {
        if (fromTile.x == toTile.x) {
            int col = fromTile.x;
            int minRow = Math.min(fromTile.y, toTile.y) + 1;
            int maxRow = Math.max(fromTile.y, toTile.y);
            for (int row = minRow; row < maxRow; row++) {
                if (isSteelTile(col, row)) {
                    return true;
                }
            }
            return false;
        }

        if (fromTile.y == toTile.y) {
            int row = fromTile.y;
            int minCol = Math.min(fromTile.x, toTile.x) + 1;
            int maxCol = Math.max(fromTile.x, toTile.x);
            for (int col = minCol; col < maxCol; col++) {
                if (isSteelTile(col, row)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isSteelTile(int col, int row) {
        int tileId = gp.getTileManager().getTileIdAt(col, row);
        tile.Tile tile = gp.getTileManager().getTile(tileId);
        return tile != null && tile.isBulletCollision() && !tile.isBreakable();
    }

    private boolean isDirectionBlocked(Direction candidateDirection) {
        if (candidateDirection == Direction.NONE) {
            return true;
        }

        Direction oldDirection = direction;
        boolean oldCollision = collisionOn;
        direction = candidateDirection;
        collisionOn = false;
        gp.getCollisionChecker().checkTile(this, 1);
        boolean blocked = collisionOn || gp.getCollisionChecker().willTankCollide(this, candidateDirection, 1);
        direction = oldDirection;
        collisionOn = oldCollision;
        return blocked;
    }

    private static class PathNode {
        private final int x;
        private final int y;
        private final int heuristic;

        private PathNode(int x, int y, int heuristic) {
            this.x = x;
            this.y = y;
            this.heuristic = heuristic;
        }
    }

    private static class AStarNode {
        private final int x;
        private final int y;
        private final int fScore;

        private AStarNode(int x, int y, int fScore) {
            this.x = x;
            this.y = y;
            this.fScore = fScore;
        }
    }

    private static class BulletThreat {
        private final Direction bulletDirection;
        private final int distanceTiles;
        private final Point bulletTile;

        private BulletThreat(Direction bulletDirection, int distanceTiles, Point bulletTile) {
            this.bulletDirection = bulletDirection;
            this.distanceTiles = distanceTiles;
            this.bulletTile = bulletTile;
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

    private boolean movePixelByPixel(Direction moveDirection, int totalDistance) {
        if (moveDirection == Direction.NONE || totalDistance <= 0) {
            return false;
        }

        boolean moved = false;
        int originalSpeed = speed;
        speed = 1;

        for (int i = 0; i < totalDistance; i++) {
            collisionOn = false;
            gp.getCollisionChecker().checkTile(this, 1);
            if (collisionOn || gp.getCollisionChecker().willTankCollide(this, moveDirection, 1)) {
                break;
            }
            move(moveDirection);
            moved = true;
        }

        speed = originalSpeed;
        return moved;
    }

    private void shoot() {
        BulletType shotBulletType = type.getBulletType();
        BulletEffectType shotEffect = armedBulletEffect;
        armedBulletEffect = BulletEffectType.NONE;
        spawnBullet(direction, shotBulletType, shotEffect);
        startShotCooldown(shotBulletType.getCooldown());
    }

    private void startShotCooldown(int cooldownTicks) {
        shotCooldown = Math.max(0, cooldownTicks);
        shotCooldownMax = Math.max(1, cooldownTicks);
    }

    private void spawnBullet(Direction bulletDirection, BulletType bulletType, BulletEffectType effectType) {
        if (bulletDirection == Direction.NONE) {
            return;
        }

        Point spawnPoint = calculateBulletSpawnPoint(bulletDirection, bulletType);
        gp.addBullet(new Bullet(gp, spawnPoint.x, spawnPoint.y, bulletDirection, bulletType, effectType, this));
    }

    private Point calculateBulletSpawnPoint(Direction bulletDirection, BulletType bulletType) {
        int bulletX = solidArea.x;
        int bulletY = solidArea.y;

        switch (bulletDirection) {
            case UP:
                bulletX = solidArea.x + (solidArea.width / 2 - bulletType.getWidth() + 2);
                bulletY = solidArea.y - solidArea.height / 2 - 5;
                break;
            case RIGHT:
                bulletX = solidArea.x + solidArea.width + 1;
                bulletY = solidArea.y + (solidArea.height / 2 - bulletType.getHeight() - 3);
                break;
            case DOWN:
                bulletX = solidArea.x + (solidArea.width / 2 - bulletType.getWidth() + 2);
                bulletY = solidArea.y + solidArea.height;
                break;
            case LEFT:
                bulletX = solidArea.x - solidArea.width / 2 - 6;
                bulletY = solidArea.y + (solidArea.height / 2 - bulletType.getWidth() + 2);
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
        BulletType shotBulletType = type.getBulletType();

        spawnBullet(direction, shotBulletType, BulletEffectType.NONE);
        spawnBullet(leftDirection, shotBulletType, BulletEffectType.NONE);
        spawnBullet(rightDirection, shotBulletType, BulletEffectType.NONE);

        startShotCooldown(shotBulletType.getCooldown());
    }

    public void activateBigPhaseShot() {
        if (shotCooldown > 0 || direction == Direction.NONE) {
            return;
        }

        spawnBullet(direction, BulletType.BIG_BULLET, BulletEffectType.PIERCING);
        startShotCooldown(BulletType.BIG_BULLET.getCooldown());
    }

    public void activateBomb() {
        int centerX = solidArea.x + solidArea.width / 2;
        int centerY = solidArea.y + solidArea.height / 2;
        gp.addBomb(new Bomb(centerX, centerY, this));
    }

    public void activateTrap() {
        int centerX = solidArea.x + solidArea.width / 2;
        int centerY = solidArea.y + solidArea.height / 2;
        gp.addTrap(new Trap(centerX, centerY, this));
    }

    public void trapTank(Trap trap) {
        currentTrap = trap;
    }

    public void releaseTrap() {
        currentTrap = null;
    }

    public boolean grantSkillToFirstEmptySlot(SkillType skill) {
        if (skill == null || skill == SkillType.NONE) {
            return false;
        }

        for (int i = 0; i < skillSlots.length; i++) {
            if (skillSlots[i] == SkillType.NONE) {
                skillSlots[i] = skill;
                refreshSkillSlotIcons();
                return true;
            }
        }
        return false;
    }

    private void refreshSkillSlotIcons() {
        slotSkill1 = resolveSkillIcon(skillSlots[0]);
        slotSkill2 = resolveSkillIcon(skillSlots[1]);
    }

    private BufferedImage resolveSkillIcon(SkillType skill) {
        String iconPath = switch (skill) {
            case SHIELD -> "/icon/shied_icon.png";
            case TOXIC -> "/icon/poison.png";
            case PHASE_SHOT -> "/icon/piercing.png";
            case BIG_PHASE_SHOT -> "/icon/big_bullet.png";
            case SLOW -> "/icon/slow.png";
            case TRIPLE_SHOT -> "/icon/3shot.png";
            case TRAP -> "/icon/trap.png";
            case BOMB -> "/icon/landmine.png";
            default -> "/ui/inventory.png";
        };

        try {
            return loadImage(iconPath);
        } catch (IOException e) {
            try {
                return loadImage("/ui/inventory.png");
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    private void handleSkillInput(int skillKey, SkillType skill) {
        boolean isPressed = keyH.isPressed(skillKey);
        boolean wasPressed = skillKey == keySetting.getKeySkill1() ? skill1WasPressed : skill2WasPressed;

        if (isPressed && !wasPressed) {
            boolean activated = tryActivateSkill(skillKey, skill);
            if (activated) {
                consumeSkillFromSlot(skillKey);
            }
        }

        if (skillKey == keySetting.getKeySkill1()) {
            skill1WasPressed = isPressed;
        } else if (skillKey == keySetting.getKeySkill2()) {
            skill2WasPressed = isPressed;
        }
    }

    private boolean tryActivateSkill(int skillKey, SkillType skill) {
        if (!keyH.isPressed(skillKey) || skill == SkillType.NONE || currentFuel < skill.getFuelCost()) {
            return false;
        }

        if (!canActivateSkill(skill)) {
            return false;
        }

        BulletEffectType requestedEffect = getSkillBulletEffect(skill);
        if (requestedEffect != BulletEffectType.NONE && armedBulletEffect == requestedEffect) {
            return false;
        }

        currentFuel -= skill.getFuelCost();
        skill.activate(this);
        return true;
    }

    private void consumeSkillFromSlot(int skillKey) {
        if (skillKey == keySetting.getKeySkill1()) {
            skillSlots[0] = SkillType.NONE;
        } else if (skillKey == keySetting.getKeySkill2()) {
            skillSlots[1] = SkillType.NONE;
        }
        refreshSkillSlotIcons();
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
            case BIG_PHASE_SHOT -> shotCooldown == 0 && direction != Direction.NONE;
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
        shotCooldownMax = 1;
        slowTrailSegments.clear();
    }

    public void draw(Graphics2D g2) {
        if (state == TankState.DEAD) {
            return;
        }

        if (state != TankState.DYING) {
            drawSlowTrail(g2);
            drawTank(g2);
            drawPoisonEffect(g2);
            drawShieldEffect(g2);
            drawHitEffect(g2);
            drawUI(g2);
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
        double cooldownBarHeight = (double) shotCooldown / Math.max(1, shotCooldownMax) * cooldownBar;
        cooldownBarHeight = Math.min(cooldownBar, Math.max(0, cooldownBarHeight));
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

    public int restoreHealth(int amount) {
        if (amount <= 0 || state == TankState.DYING || state == TankState.DEAD) {
            return 0;
        }

        int before = currentHealth;
        currentHealth = Math.min(Config.MAX_HEALTH, currentHealth + amount);
        return currentHealth - before;
    }

    public int restoreFuel(int amount) {
        if (amount <= 0 || state == TankState.DYING || state == TankState.DEAD) {
            return 0;
        }

        int before = currentFuel;
        currentFuel = Math.min(maxFuel, currentFuel + amount);
        return currentFuel - before;
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
