package tile;

import main.Config;
import main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TileManager {
    private final GamePanel gp;
    private final Tile[] tile;
    private final int[][] mapTileNum;

    public TileManager(GamePanel gp) {
        this.gp = gp;
        tile = new Tile[10];
        mapTileNum = new int[Config.MAX_SCREEN_COL][Config.MAX_SCREEN_ROW];

        getTileImage();
        // Load default map if none specified
        loadMap("/maps/map01.txt");
    }

    private void getTileImage() {
        tile[0] = new Tile();

        // Loại 1: Tường bằng gạch (Vật cản không thể phá)
        tile[1] = new Tile();
        tile[1].setCollision(true);
        tile[1].setBulletCollision(true);
        tile[1].setImage(new BufferedImage(Config.TILE_SIZE, Config.TILE_SIZE, BufferedImage.TYPE_INT_ARGB));
        Graphics2D g2 = tile[1].getImage().createGraphics();
        g2.setColor(new Color(100, 100, 100)); // Màu xám cho tường cứng
        g2.fillRect(0, 0, Config.TILE_SIZE, Config.TILE_SIZE);
        g2.setColor(Color.BLACK);
        g2.drawRect(0, 0, Config.TILE_SIZE - 1, Config.TILE_SIZE - 1);
        g2.dispose();

        // Loại 2: Tường có thể phá (Breakable)
        tile[2] = new Tile();
        tile[2].setCollision(true);
        tile[2].setBreakable(true);
        tile[2].setBulletCollision(true);
        tile[2].setImage(new BufferedImage(Config.TILE_SIZE, Config.TILE_SIZE, BufferedImage.TYPE_INT_ARGB));
        g2 = tile[2].getImage().createGraphics();
        g2.setColor(new Color(139, 69, 19)); // Màu nâu gạch
        g2.fillRect(0, 0, Config.TILE_SIZE, Config.TILE_SIZE);
        g2.setColor(new Color(200, 100, 50));
        g2.drawRect(2, 2, Config.TILE_SIZE - 5, Config.TILE_SIZE - 5); // Hiệu ứng gạch
        g2.dispose();

        // Loại 3: Nước (Tank ko qua đc, đạn qua đc)
        tile[3] = new Tile();
        tile[3].setCollision(true);
        tile[3].setBulletCollision(false);
        tile[3].setImage(new BufferedImage(Config.TILE_SIZE, Config.TILE_SIZE, BufferedImage.TYPE_INT_ARGB));
        g2 = tile[3].getImage().createGraphics();
        g2.setColor(new Color(30, 144, 255)); // Màu xanh nước biển
        g2.fillRect(0, 0, Config.TILE_SIZE, Config.TILE_SIZE);
        // Thêm vài gợn sóng
        g2.setColor(new Color(100, 200, 255));
        g2.drawLine(5, 10, 20, 10);
        g2.drawLine(25, 20, 40, 20);
        g2.dispose();

        //new

    }

    public void setTileAt(int col, int row, int tileId) {
        if (col >= 0 && col < Config.MAX_SCREEN_COL && row >= 0 && row < Config.MAX_SCREEN_ROW) {
            mapTileNum[col][row] = tileId;
        }
    }

    public Tile getTile(int id) {
        if (id >= 0 && id < tile.length) {
            return tile[id];
        }
        return null;
    }

    public int getTileIdAt(int col, int row) {
        if (col < 0 || row < 0 || col >= Config.MAX_SCREEN_COL || row >= Config.MAX_SCREEN_ROW) {
            return 1; // Coi như tường cứng nếu ra ngoài biên
        }
        return mapTileNum[col][row];
    }

    public void loadMap(String filePath) {
        try {
            InputStream is = getClass().getResourceAsStream(filePath);
            if (is == null) {
                System.err.println("Map file not found: " + filePath);
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            int col = 0;
            int row = 0;

            while (row < Config.MAX_SCREEN_ROW) {
                String line = br.readLine();
                if (line == null) break;

                String[] numbers = line.split("\\s+");
                for (col = 0; col < Config.MAX_SCREEN_COL && col < numbers.length; col++) {
                    int num = Integer.parseInt(numbers[col]);
                    mapTileNum[col][row] = num;
                }
                row++;
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCollisionAt(int col, int row) {
        if (col < 0 || row < 0 || col >= Config.MAX_SCREEN_COL || row >= Config.MAX_SCREEN_ROW) {
            return true;
        }

        int tileNum = mapTileNum[col][row];
        return tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null && tile[tileNum].isCollision();
    }

    public void draw(Graphics2D g2) {
        for (int row = 0; row < Config.MAX_SCREEN_ROW; row++) {
            for (int col = 0; col < Config.MAX_SCREEN_COL; col++) {
                int tileNum = mapTileNum[col][row];
                if (tileNum != 0 && tile[tileNum] != null) {
                    g2.drawImage(tile[tileNum].getImage(), col * Config.TILE_SIZE, row * Config.TILE_SIZE, Config.TILE_SIZE, Config.TILE_SIZE, null);
                }
            }
        }
    }
}
