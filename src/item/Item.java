package item;

import entity.SkillType;
import entity.Tank;
import main.Config;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class Item {
    public enum ItemType {
        SKILL_CHEST,
        HEALTH,
        ENERGY
    }

    private enum ItemState {
        CLOSED,
        OPENING,
        CONSUMED
    }

    private static final int CHEST_DRAW_SIZE = Config.TILE_SIZE;
    private static final int OPENING_TICKS = 14;
    private static final int CLOSED_DESPAWN_TICKS = Config.FPS * 12;
    private static final int HEALTH_RESTORE_AMOUNT = 30;
    private static final int ENERGY_RESTORE_AMOUNT = 30;

    private final int x;
    private final int y;
    private final Rectangle solidArea;
    private final ItemType itemType;
    private final BufferedImage[] chestFrames;
    private final BufferedImage healthIcon;
    private final BufferedImage energyIcon;
    private final Random random = new Random();
    private ItemState state = ItemState.CLOSED;
    private int openingTick = 0;
    private int closedTick = 0;

    public Item(int x, int y) {
        this.x = x;
        this.y = y;
        this.solidArea = new Rectangle(x + Config.TILE_SIZE / 4, y + Config.TILE_SIZE / 4,
                Config.TILE_SIZE / 2, Config.TILE_SIZE / 2);
        this.itemType = randomItemType();
        this.chestFrames = loadChestFrames();
        this.healthIcon = loadSprite("/icon/health.png");
        this.energyIcon = loadSprite("/icon/energy.png");
    }

    public void update() {
        if (state == ItemState.CLOSED) {
            closedTick++;
            if (closedTick >= CLOSED_DESPAWN_TICKS) {
                state = ItemState.CONSUMED;
            }
            return;
        }

        if (state != ItemState.OPENING) {
            return;
        }

        openingTick++;
        if (openingTick >= OPENING_TICKS) {
            state = ItemState.CONSUMED;
        }
    }

    public void draw(Graphics2D g2) {
        if (state == ItemState.CONSUMED) {
            return;
        }

        if (itemType == ItemType.HEALTH) {
            if (healthIcon != null) {
                g2.drawImage(healthIcon, x, y, CHEST_DRAW_SIZE, CHEST_DRAW_SIZE, null);
            }
            return;
        }

        if (itemType == ItemType.ENERGY) {
            if (energyIcon != null) {
                g2.drawImage(energyIcon, x, y, CHEST_DRAW_SIZE, CHEST_DRAW_SIZE, null);
            }
            return;
        }

        if (chestFrames.length == 0) {
            return;
        }

        int frameIndex = state == ItemState.OPENING ? 1 : 0;
        frameIndex = Math.min(frameIndex, chestFrames.length - 1);
        g2.drawImage(chestFrames[frameIndex], x, y, CHEST_DRAW_SIZE, CHEST_DRAW_SIZE, null);
    }

    public boolean tryOpen(Tank tank) {
        if (state != ItemState.CLOSED || tank == null) {
            return false;
        }
        if (!solidArea.intersects(tank.getSolidArea())) {
            return false;
        }

        if (itemType == ItemType.HEALTH) {
            int restored = tank.restoreHealth(HEALTH_RESTORE_AMOUNT);
            state = ItemState.CONSUMED;
            return true;
        }

        if (itemType == ItemType.ENERGY) {
            int restored = tank.restoreFuel(ENERGY_RESTORE_AMOUNT);
            state = ItemState.CONSUMED;
            return true;
        }

        SkillType reward = randomSkill();
        if (!tank.grantSkillToFirstEmptySlot(reward)) {
            return false;
        }

        state = ItemState.OPENING;
        openingTick = 0;
        return true;
    }

    public boolean isConsumed() {
        return state == ItemState.CONSUMED;
    }

    public Rectangle getSolidArea() {
        return solidArea;
    }

    public ItemType getItemType() {
        return itemType;
    }

    private SkillType randomSkill() {
        SkillType[] pool = {
                SkillType.SHIELD,
                SkillType.TOXIC,
                SkillType.PHASE_SHOT,
                SkillType.SLOW,
                SkillType.TRIPLE_SHOT,
                SkillType.BIG_PHASE_SHOT,
                SkillType.BOMB,
                SkillType.TRAP
        };
        return pool[random.nextInt(pool.length)];
    }

    private ItemType randomItemType() {
        int roll = random.nextInt(100);
        if (roll < 50) {
            return ItemType.SKILL_CHEST;
        }
        if (roll < 75) {
            return ItemType.HEALTH;
        }
        return ItemType.ENERGY;
    }

    private BufferedImage[] loadChestFrames() {
        BufferedImage sprite = loadSprite("/effects/treasure.png");
        if (sprite == null) {
            sprite = loadSprite("/icon/treasure.png");
        }
        if (sprite == null) {
            sprite = loadSprite("/ui/inventory.png");
        }
        if (sprite == null) {
            return new BufferedImage[0];
        }

        int frameW = Math.max(1, sprite.getWidth() / 2);
        int frameH = sprite.getHeight();
        if (sprite.getWidth() >= frameW * 2) {
            return new BufferedImage[] {
                    sprite.getSubimage(0, 0, frameW, frameH),
                    sprite.getSubimage(frameW, 0, frameW, frameH)
            };
        }

        return new BufferedImage[] {sprite, sprite};
    }

    private BufferedImage loadSprite(String path) {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            if (input == null) {
                return null;
            }
            return ImageIO.read(input);
        } catch (IOException e) {
            return null;
        }
    }
}