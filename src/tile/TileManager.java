package tile;

import main.Config;
import main.GamePanel;

import javax.imageio.ImageIO;
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
        tile = new Tile[50];
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
        setup(0, "grass_01", false, false, false);
        setup(1, "grass_rock_01", false, false, false);
        setup(2, "grass_rock_02", false, false, false);
        setup(3, "grass_rock_03", false, false, false);
        setup(4, "rock_01", true, false, true);
        setup(5, "rock_02", true, false, true);
        setup(6, "rock_03", false, false, false);
        setup(7, "grass_rock_04", false, false, false);
        setup(8, "grass_rock_05", false, false, false);
        setup(9, "grass_rock_06", false, false, false);
        setup(10, "grass_rock_07", false, false, false);
        setup(11, "grass_rock_08", false, false, false);
        setup(12, "grass_rock_09", true, false, false);
    }

    private void setup(int index, String imageName, boolean isCollision, boolean isBreakable, boolean isBulletCollision) {
        try {
            tile[index] = new Tile();
            BufferedImage image = ImageIO.read(getClass().getResource("/tiles/" + imageName + ".png"));
            tile[index].setImage(image);
            tile[index].setCollision(isCollision);
            tile[index].setBulletCollision(isBulletCollision);
            tile[index].setBreakable(isBreakable);
        } catch (Exception e) {
            System.out.println("Error load tile image:: " + imageName);
            e.printStackTrace();
        }
    }

    public void setupAnimatedFromSheet(int index, String sheetName, int numFrames, int frameW, int frameH, boolean tankCol, boolean bulletCol) {
        try {
            tile[index] = new Tile();
            tile[index].setAnimated(true);
            BufferedImage[] frames = new BufferedImage[numFrames];

            // Step 1: Nạp file Sprite Sheet GỐC
            BufferedImage spriteSheet = ImageIO.read(getClass().getResourceAsStream("/tiles/" + sheetName + ".png"));

            for (int i = 0; i < numFrames; i++) {
                frames[i] = spriteSheet.getSubimage(i * frameW, 0, frameW, frameH);
            }

            tile[index].setFrames(frames);
            tile[index].setImage(frames[0]);

            tile[index].setCollision(tankCol);
            tile[index].setBulletCollision(bulletCol);

        } catch (Exception e) {
            System.out.println("Lỗi nạp Sprite Sheet: " + sheetName + " cho Tile ID: " + index);
            e.printStackTrace();
        }
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
        int col = 0, row = 0;
        int x = 0, y = 0;

        while (col < gp.maxScreenCol && row < gp.maxScreenRow) {
            int tileNum = mapTileNum[col][row];

            // VẼ VÀ ÉP KÍCH THƯỚC (SCALE) TRỰC TIẾP TẠI ĐÂY
            g2.drawImage(tile[tileNum].getImage(), x, y, gp.tileSize, gp.tileSize, null);

            col++; x += gp.tileSize;
            if (col == gp.maxScreenCol) {
                col = 0; x = 0;
                row++; y += gp.tileSize;
            }
        }
    }
}
