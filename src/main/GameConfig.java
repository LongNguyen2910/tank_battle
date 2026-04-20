package main;

import entity.TankType;

public class GameConfig {
    public int playerCount = 1;
    public int computerCount = 0;
    public String mapPath = "/maps/map03.txt";
    public String gameMode = "Deathmatch";
    
    // Thêm lựa chọn xe tăng
    public TankType p1Tank = TankType.NORMAL;
    public TankType p2Tank = TankType.NORMAL;
}
