package tile;

import main.Config;
import main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TileManager {
    private final GamePanel gp;
    private final Tile[] tile;
    private final int[][] mapTileNum;

    public TileManager(GamePanel gp) {
        this.gp = gp;
        // Khởi tạo mảng chứa các loại Tile (ví dụ: 0 là đường trống, 1 là tường nâu)
        tile = new Tile[10];
        mapTileNum = new int[Config.MAX_SCREEN_COL][Config.MAX_SCREEN_ROW];

        getTileImage();
        loadMap();
    }

    private void getTileImage() {
        // Loại 0: Nền trống (Không cần khởi tạo hình ảnh vì ta sẽ vẽ đè lên nền đen)
        tile[0] = new Tile();

        // Loại 1: Tường bằng gạch (Vật cản)
        tile[1] = new Tile();
        tile[1].setCollision(true); // Xe tăng không thể đi xuyên qua

        // Tự động tạo một hình chữ nhật màu nâu thay thế cho ảnh asset thật
        tile[1].setImage(new BufferedImage(Config.TILE_SIZE, Config.TILE_SIZE, BufferedImage.TYPE_INT_ARGB));
        Graphics2D g2 = tile[1].getImage().createGraphics();
        g2.setColor(new Color(139, 69, 19)); // Mã màu nâu (SaddleBrown)
        g2.fillRect(0, 0, Config.TILE_SIZE, Config.TILE_SIZE);
        g2.dispose();
    }

    private void loadMap() {
        // Tạm thời tạo một bản đồ cứng (Hardcode) với tường bao quanh và vài chướng ngại vật
        // Sau này bạn có thể nâng cấp bằng cách đọc các con số này từ 1 file map.txt
        for (int col = 0; col < Config.MAX_SCREEN_COL; col++) {
            for (int row = 0; row < Config.MAX_SCREEN_ROW; row++) {
                // Xây tường bao quanh viền màn hình
                if (col == 0 || col == Config.MAX_SCREEN_COL - 1 || row == 0 || row == Config.MAX_SCREEN_ROW - 1) {
                    mapTileNum[col][row] = 1;
                } else {
                    mapTileNum[col][row] = 0; // Không gian trống
                }
            }
        }

        // Thêm một số bức tường ở giữa bản đồ
        mapTileNum[4][4] = 1;
        mapTileNum[4][5] = 1;
        mapTileNum[10][7] = 1;
        mapTileNum[10][8] = 1;
    }

    public boolean isCollisionAt(int col, int row) {
        if (col < 0 || row < 0 || col >= Config.MAX_SCREEN_COL || row >= Config.MAX_SCREEN_ROW) {
            return true;
        }

        int tileNum = mapTileNum[col][row];
        return tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null && tile[tileNum].isCollision();
    }

    public void draw(Graphics2D g2) {
        int col = 0;
        int row = 0;
        int x = 0;
        int y = 0;

        // Quét qua toàn bộ mảng 2 chiều và vẽ các Tile lên màn hình
        while (col < Config.MAX_SCREEN_COL && row < Config.MAX_SCREEN_ROW) {
            int tileNum = mapTileNum[col][row];

            // Chỉ vẽ những ô có hình ảnh (như tường), bỏ qua ô trống
            if (tileNum != 0 && tile[tileNum] != null) {
                g2.drawImage(tile[tileNum].getImage(), x, y, Config.TILE_SIZE, Config.TILE_SIZE, null);
            }

            col++;
            x += Config.TILE_SIZE;

            // Xuống dòng khi đã vẽ hết 1 hàng ngang
            if (col == Config.MAX_SCREEN_COL) {
                col = 0;
                x = 0;
                row++;
                y += Config.TILE_SIZE;
            }
        }
    }
}
