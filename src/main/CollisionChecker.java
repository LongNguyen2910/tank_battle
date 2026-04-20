package main;

import entity.Bullet;
import entity.BulletType;
import entity.Bomb;
import entity.Trap;
import entity.Direction;
import entity.GameObject;
import entity.Tank;

import java.awt.*;

public class CollisionChecker {
    GamePanel gp;

    public CollisionChecker(GamePanel gp) {
        this.gp = gp;
    }

    public void checkTile(GameObject entity) {
        checkTile(entity, entity.getSpeed());
    }

    public void checkTile(GameObject entity, int probeDistance) {
        Rectangle solidArea = entity.getSolidArea();
        int entityLeftX = solidArea.x;
        int entityRightX = solidArea.x + solidArea.width - 1;
        int entityTopY = solidArea.y;
        int entityBottomY = solidArea.y + solidArea.height - 1;

        int entityLeftCol = Math.floorDiv(entityLeftX, Config.TILE_SIZE);
        int entityRightCol = Math.floorDiv(entityRightX, Config.TILE_SIZE);
        int entityTopRow = Math.floorDiv(entityTopY, Config.TILE_SIZE);
        int entityBottomRow = Math.floorDiv(entityBottomY, Config.TILE_SIZE);

        Direction direction = entity.getDirection();
        int step = Math.max(1, probeDistance);

        switch (direction) {
            case UP:
                entityTopRow = Math.floorDiv(entityTopY - step, Config.TILE_SIZE);
                checkCollision(entity, entityLeftCol, entityTopRow, entityRightCol, entityTopRow);
                break;
            case DOWN:
                entityBottomRow = Math.floorDiv(entityBottomY + step, Config.TILE_SIZE);
                checkCollision(entity, entityLeftCol, entityBottomRow, entityRightCol, entityBottomRow);
                break;
            case LEFT:
                entityLeftCol = Math.floorDiv(entityLeftX - step, Config.TILE_SIZE);
                checkCollision(entity, entityLeftCol, entityTopRow, entityLeftCol, entityBottomRow);
                break;
            case RIGHT:
                entityRightCol = Math.floorDiv(entityRightX + step, Config.TILE_SIZE);
                checkCollision(entity, entityRightCol, entityTopRow, entityRightCol, entityBottomRow);
                break;
            case NONE:
                break;
        }
    }

    public boolean willTankCollide(Tank movingTank, Direction direction, int probeDistance) {
        if (movingTank == null || direction == Direction.NONE) {
            return false;
        }

        int step = Math.max(1, probeDistance);
        Rectangle nextArea = new Rectangle(movingTank.getSolidArea());
        switch (direction) {
            case UP -> nextArea.y -= step;
            case DOWN -> nextArea.y += step;
            case LEFT -> nextArea.x -= step;
            case RIGHT -> nextArea.x += step;
            case NONE -> {
            }
        }

        if (nextArea.x < 0 || nextArea.y < 0
                || nextArea.x + nextArea.width > Config.SCREEN_WIDTH
                || nextArea.y + nextArea.height > Config.SCREEN_HEIGHT) {
            return true;
        }

        for (Tank other : gp.getTankList()) {
            if (other == movingTank || other.isPendingRemoval()) {
                continue;
            }
            if (other.getState() == Tank.TankState.DYING || other.getState() == Tank.TankState.DEAD) {
                continue;
            }
            if (nextArea.intersects(other.getSolidArea())) {
                return true;
            }
        }

        return false;
    }

    private void checkCollision(GameObject entity, int col1, int row1, int col2, int row2) {
        int tileId1 = gp.getTileManager().getTileIdAt(col1, row1);
        int tileId2 = gp.getTileManager().getTileIdAt(col2, row2);
        
        tile.Tile t1 = gp.getTileManager().getTile(tileId1);
        tile.Tile t2 = gp.getTileManager().getTile(tileId2);

        if (entity instanceof Bullet) {
            if (t1 != null && t1.isBulletCollision()) {
                if (t1.isBreakable()) {
                    gp.getTileManager().setTileAt(col1, row1, 0);
                }
                entity.setCollisionOn(true);
            }
            if (t2 != null && t2.isBulletCollision()) {
                if (t2.isBreakable()) {
                    gp.getTileManager().setTileAt(col2, row2, 0);
                }
                entity.setCollisionOn(true);
            }
        } else {
            if ((t1 != null && t1.isCollision()) || (t2 != null && t2.isCollision())) {
                entity.setCollisionOn(true);
            }
        }
    }

    public void checkHit() {
        for (Tank tank : gp.getTankList()) {
            for (Bullet bullet : gp.getBulletList()) {
                if (bullet.getOwner() == tank) {
                    continue;
                }
                if (bullet.canDamage() && tank.getSolidArea().intersects(bullet.getSolidArea())) {
                    Rectangle bulletArea = bullet.getSolidArea();
                    int impactCenterX = bulletArea.x + bulletArea.width / 2;
                    int impactCenterY = bulletArea.y + bulletArea.height / 2;
                    boolean blockedByShield = tank.blockBulletIfPossible(bullet.getDirection(), impactCenterX, impactCenterY);
                    boolean shieldReducedHit = blockedByShield && bullet.getBulletType() == BulletType.BIG_BULLET;

                    if (blockedByShield && !shieldReducedHit) {
                        bullet.startImpact();
                        continue;
                    }

                    if (tank.canBeDamaged()) {
                        int incomingDamage = bullet.getDamage();
                        if (shieldReducedHit) {
                            incomingDamage = Math.max(1, (int) Math.ceil(incomingDamage * 0.5));
                        }

                        boolean damageApplied = tank.takeDamage(incomingDamage, bullet.getSolidArea().x, bullet.getSolidArea().y);
                        if (damageApplied) {
                            switch (bullet.getEffectType()) {
                                case TOXIC -> tank.applyPoison();
                                case PIERCING, NONE -> {
                                    // no extra on-hit effect
                                }
                            }
                        }
                        bullet.destroyImmediately();
                    }
                    bullet.setAlive(false);
                }
            }
        }
    }

    public void checkBombHit() {
        for (Bomb bomb : gp.getBombList()) {
            if (!bomb.canTrigger()) {
                continue;
            }

            for (Tank tank : gp.getTankList()) {
                if (tank.getState() == Tank.TankState.DYING || tank.getState() == Tank.TankState.DEAD) {
                    continue;
                }

                if (bomb.canBeTriggeredBy(tank) && bomb.intersects(tank.getSolidArea())) {
                    bomb.trigger(tank);
                    break;
                }
            }
        }
    }

    public void checkTrapHit() {
        for (Trap trap : gp.getTrapList()) {
            if (!trap.canTrigger()) {
                continue;
            }

            for (Tank tank : gp.getTankList()) {
                if (tank.getState() == Tank.TankState.DYING || tank.getState() == Tank.TankState.DEAD) {
                    continue;
                }

                if (trap.canBeTriggeredBy(tank) && trap.intersects(tank.getSolidArea())) {
                    trap.trigger(tank);
                    break;
                }
            }
        }
    }
}