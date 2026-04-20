package main;

import entity.Bullet;
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
                if (bullet.canDamage() && tank.getSolidArea().intersects(bullet.getSolidArea())) {
                    Rectangle bulletArea = bullet.getSolidArea();
                    int impactCenterX = bulletArea.x + bulletArea.width / 2;
                    int impactCenterY = bulletArea.y + bulletArea.height / 2;

                    if (tank.blockBulletIfPossible(bullet.getDirection(), impactCenterX, impactCenterY)) {
                        bullet.startImpact();
                        continue;
                    }

                    if (tank.canBeDamaged()) {
                        boolean damageApplied = tank.takeDamage(bullet.getDamage(), bullet.getSolidArea().x, bullet.getSolidArea().y);
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
}