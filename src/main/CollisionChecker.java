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
        Rectangle solidArea = entity.getSolidArea();
        int entityLeftX = solidArea.x;
        int entityRightX = solidArea.x + solidArea.width - 1;
        int entityTopY = solidArea.y;
        int entityBottomY = solidArea.y + solidArea.height - 1;

        int entityLeftCol = (entityLeftX / Config.TILE_SIZE);
        int entityRightCol = (entityRightX / Config.TILE_SIZE);
        int entityTopRow = (entityTopY / Config.TILE_SIZE);
        int entityBottomRow = (entityBottomY / Config.TILE_SIZE);

        Direction direction = entity.getDirection();

        switch (direction) {
            case UP:
                entityTopRow = (entityTopY - entity.getSpeed()) / Config.TILE_SIZE;
                if (gp.getTileManager().isCollisionAt(entityLeftCol, entityTopRow)
                        || gp.getTileManager().isCollisionAt(entityRightCol, entityTopRow)) {
                    entity.setCollisionOn(true);
                }
                break;
            case DOWN:
                entityBottomRow = (entityBottomY + entity.getSpeed()) / Config.TILE_SIZE;
                if (gp.getTileManager().isCollisionAt(entityLeftCol, entityBottomRow)
                        || gp.getTileManager().isCollisionAt(entityRightCol, entityBottomRow)) {
                    entity.setCollisionOn(true);
                }
                break;
            case LEFT:
                entityLeftCol = (entityLeftX - entity.getSpeed()) / Config.TILE_SIZE;
                if (gp.getTileManager().isCollisionAt(entityLeftCol, entityTopRow)
                        || gp.getTileManager().isCollisionAt(entityLeftCol, entityBottomRow)) {
                    entity.setCollisionOn(true);
                }
                break;
            case RIGHT:
                entityRightCol = (entityRightX + entity.getSpeed()) / Config.TILE_SIZE;
                if (gp.getTileManager().isCollisionAt(entityRightCol, entityTopRow)
                        || gp.getTileManager().isCollisionAt(entityRightCol, entityBottomRow)) {
                    entity.setCollisionOn(true);
                }
                break;
            case NONE:
                break;
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