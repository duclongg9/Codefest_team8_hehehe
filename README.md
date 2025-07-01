# Codefest_team8_hehehe
# 🤖 CODEFEST 2025 – Team 8 Bot

## 🎯 Mục tiêu
Phát triển bot thông minh để chiến thắng CODEFEST 2025 thông qua chiến thuật, teamwork và tối ưu kỹ thuật.

## 📂 Cấu trúc thư mục
- `src/`: Chứa toàn bộ mã nguồn Java. Code bot, kết nối server và chiến lược xử lý đều ở đây.
- `sdk/`: Thư viện SDK của cuộc thi.
- `docs/`: Tài liệu nghiên cứu game, chiến thuật team tự xây dựng, ghi chú kỹ thuật.
- `scripts/`: Tập hợp script để chạy thử, test nhanh bot, tiện debug.
- `test/`: Nếu team muốn viết test case cho từng module bot (tuỳ năng lực team).
- `assets/`: Hình ảnh, biểu đồ chiến thuật, flow game (nếu có).
```bash
Codefest_team8/
├── src/                    # Mã nguồn Java chính
│   ├── bot/                # Logic điều khiển bot (chiến thuật, AI)
│   ├── game/               # Tương tác với map, server, trạng thái trận đấu
│   └── Main.java           # Điểm khởi đầu kết nối bot với game server
│
├── sdk/                    # Thư viện SDK của cuộc thi
│   └── CodeFestv2.5.jar
│
├── docs/                   # Tài liệu nội bộ: chiến thuật, ghi chú, phân tích map
│   ├── game-mechanics.md
│   ├── strategy-notes.md
│   └── npc-analysis.md
│
├── assets/                 # Hình ảnh, biểu đồ chiến thuật, flow game (nếu có)
│
├── scripts/                # Các script hỗ trợ training, test bot, debug
│   └── simulate.sh
│
├── test/                   # Unit tests hoặc mock test cases
│
├── .gitignore              # Bỏ qua file không cần track
├── README.md               # Hướng dẫn khởi động & quản lý dự án
└── LICENSE                 # Thông tin bản quyền (nếu có)

```
## ⚙️ Cách khởi động
1. Mở bằng IntelliJ (JDK 20+).
2. Import `CodeFestv2.5.jar` vào project.
3. Điền `GAME_ID`, `PLAYER_NAME`, `SECRET_KEY` vào `Main.java`.
4. Run `Main.java` → Bot sẽ kết nối với game server.

## ✅ Ghi chú
- Hạn chế hard-code; tối ưu dùng thuật toán thông minh (A*, DFS cho né trap...).
- Quản lý code sạch, dễ đọc để debug dễ và teamwork hiệu quả.
- Ghi chép rõ mọi chiến thuật để cả team hiểu cùng hướng phát triển.
