package entity;

public enum SkillType {
    NONE(0, "") {
        @Override
        public void activate(Tank tank) {
            // Do nothing
        }
    },
    SHIELD(30, "Shield") {
        @Override
        public void activate(Tank tank) {
            tank.setHasShield(true);
            tank.setShieldDuration(300);
        }
    },
    TOXIC(35, "Toxic") {
        @Override
        public void activate(Tank tank) {
            tank.armNextBulletEffect(BulletEffectType.TOXIC);
        }
    },
    PHASE_SHOT(25, "Phase Shot") {
        @Override
        public void activate(Tank tank) {
            tank.armNextBulletEffect(BulletEffectType.PIERCING);
        }
    },
    SLOW(25, "Slow") {
        @Override
        public void activate(Tank tank) {
            tank.activateSlowZone();
        }
    },
    TRIPLE_SHOT(30, "Triple Shot") {
        @Override
        public void activate(Tank tank) {
            tank.activateTripleShot();
        }
    },
    BIG_PHASE_SHOT(40, "Big Phase Shot") {
        @Override
        public void activate(Tank tank) {
            tank.activateBigPhaseShot();
        }
    },
    BOMB(35, "Bomb") {
        @Override
        public void activate(Tank tank) {
            tank.activateBomb();
        }
    },
    TRAP(30, "Trap") {
        @Override
        public void activate(Tank tank) {
            tank.activateTrap();
        }
    };


    private final int fuelCost;
    private final String displayName;

    SkillType(int fuelCost, String displayName) {
        this.fuelCost = fuelCost;
        this.displayName = displayName;
    }

    public int getFuelCost() { return fuelCost; }
    public String getDisplayName() { return displayName; }

    public abstract void activate(Tank tank);
}
