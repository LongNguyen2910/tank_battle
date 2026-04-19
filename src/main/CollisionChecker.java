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

        int entityLeftCol = (entityLeftX / gp.tileSize);
        int entityRightCol = (entityRightX / gp.tileSize);
        int entityTopRow = (entityTopY / gp.tileSize);
        int entityBottomRow = (entityBottomY / gp.tileSize);

        Direction direction = entity.getDirection();

        switch (direction) {
            case UP:
                entityTopRow = (entityTopY - entity.getSpeed()) / gp.tileSize;
                if (gp.getTileManager().isCollisionAt(entityLeftCol, entityTopRow)
                        || gp.getTileManager().isCollisionAt(entityRightCol, entityTopRow)) {
                    entity.setCollisionOn(true);
                }
                break;
            case DOWN:
                entityBottomRow = (entityBottomY + entity.getSpeed()) / gp.tileSize;
                if (gp.getTileManager().isCollisionAt(entityLeftCol, entityBottomRow)
                        || gp.getTileManager().isCollisionAt(entityRightCol, entityBottomRow)) {
                    entity.setCollisionOn(true);
                }
                break;
            case LEFT:
                entityLeftCol = (entityLeftX - entity.getSpeed()) / gp.tileSize;
                if (gp.getTileManager().isCollisionAt(entityLeftCol, entityTopRow)
                        || gp.getTileManager().isCollisionAt(entityLeftCol, entityBottomRow)) {
                    entity.setCollisionOn(true);
                }
                break;
            case RIGHT:
                entityRightCol = (entityRightX + entity.getSpeed()) / gp.tileSize;
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
                if (bullet.isAlive() && tank.getSolidArea().intersects(bullet.getSolidArea())) {
                    tank.takeDamage(bullet.getDamage());
                    bullet.setAlive(false);
                }
            }
        }
    }
}