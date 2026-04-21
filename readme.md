# Tank Battle Project

Dự án Tank Battle là một trò chơi hành động xe tăng được xây dựng bằng Java Swing. Người chơi có thể tham gia vào các trận chiến kịch tính với nhiều loại xe tăng, vật phẩm hỗ trợ và bản đồ khác nhau.

## 📁 Cấu trúc dự án

Dưới đây là cấu trúc các thư mục chính của dự án:

```text
C:\Users\LOQ\IdeaProjects\tank_battle\
├───res\                # Tài nguyên game (hình ảnh, bản đồ, icon)
│   ├───bullets\        # Hình ảnh các loại đạn
│   ├───effects\        # Hiệu ứng nổ, trạng thái (poison, slow,...)
│   ├───icon\           # Các biểu tượng kỹ năng và vật phẩm
│   ├───maps\           # Tệp tin cấu hình bản đồ (.txt)
│   ├───tanks\          # Hình ảnh các loại xe tăng
│   ├───tiles\          # Hình ảnh các ô gạch trong game
│   └───ui\             # Hình ảnh giao diện người dùng (máu, nhiên liệu,...)
└───src\                # Mã nguồn Java
    ├───entity\         # Các đối tượng trò chơi (Tank, Bullet, Bomb,...)
    ├───item\           # Hệ thống vật phẩm hỗ trợ
    ├───main\           # Logic chính (Vòng lặp game, va chạm, cấu hình)
    ├───tile\           # Quản lý bản đồ và các ô gạch
    └───ui\             # Các màn hình (Lobby, Pause, Win, Settings,...)
```

## 📋 Yêu cầu hệ thống (Requirements)

Để chạy được dự án này, máy tính của bạn cần:

- **Hệ điều hành:** Windows, macOS hoặc Linux.
- **Java Development Kit (JDK):** Phiên bản **17** trở lên.
- **IDE khuyên dùng:** IntelliJ IDEA (hoặc Eclipse/VS Code với Java Extension).

## 🚀 Hướng dẫn Set up và Chạy dự án

### 1. Cài đặt môi trường
- Đảm bảo bạn đã cài đặt JDK 17+. Bạn có thể kiểm tra bằng lệnh `java -version` trong terminal.

### 2. Mở dự án trong IntelliJ IDEA
- Mở IntelliJ IDEA.
- Chọn **File > Open** và dẫn đến thư mục `tank_battle`.
- IntelliJ sẽ tự động nhận diện cấu trúc dự án.

### 3. Cấu hình Tài nguyên (Resources)
- Đảm bảo thư mục `res` được cấu hình là thư mục chứa tài nguyên.
- Trong IntelliJ: Chuột phải vào thư mục `res` > **Mark Directory as > Resources Root**.

### 4. Chạy trò chơi
- Tìm đến tệp tin `src/ui/StartingScreen.java`.
- Chuột phải vào tệp và chọn **Run**.
- Trò chơi sẽ khởi động với màn hình Starting Screen.

## 🎮 Cách điều khiển (Mặc định)

- **Người chơi 1:**
  - Di chuyển: `W`, `A`, `S`, `D`
  - Bắn: `J`
  - Lướt (Dash): `K`
  - Kỹ năng 1: `U` | Kỹ năng 2: `I`

- **Người chơi 2:**
  - Di chuyển: Các phím mũi tên
  - Bắn: `Num 1`
  - Lướt (Dash): `Num 2`
  - Kỹ năng 1: `Num 4` | Kỹ năng 2: `Num 5`

- **Hệ thống:**
  - Tạm dừng/Menu: `P` hoặc `ESC`

## 📜 Giấy phép tài nguyên (LICENSE)

Dự án sử dụng tài nguyên từ các nguồn sau:
- **UI Health bar, Inventory:** [ElvGames](https://elvgames.itch.io/free-inventory-asset-pack)
- **Bullet:** [BDragon1727](https://bdragon1727.itch.io/fire-pixel-bullet-16x16)
- **Bullet-hit, Explosion:** [pimen](https://pimen.itch.io/fire-spell)
- **Font:** [Daniel Linssen](https://managore.itch.io/m6x11)
- **Tileset:** [Pixel Frog](https://pixelfrog-assets.itch.io/tiny-swords)
- **Icon:** [Clockwork Raven](https://clockworkraven.itch.io/raven-fantasy-icons)
