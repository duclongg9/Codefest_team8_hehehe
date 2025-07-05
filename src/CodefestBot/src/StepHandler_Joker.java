/**
 * ---------------------------------------------------------------
 *  JOKER Bot – CodeFest 2025
 *  Mục tiêu  : Tối ưu điểm số, hạn chế giao tranh, sống sót lâu.
 *  Hành vi   :
 *      1. Ưu tiên nhặt Item/Weapon nhiều điểm nhất.
 *      2. Luôn giữ vị trí trong Vùng Sáng; tránh Vùng Tối (mất HP/step).
 *      3. Né NPC gây sát thương & đạn bay, dùng SupportItem khi máu thấp.
 *      4. TẤN CÔNG người chơi khác (Player) CHỈ khi họ đã lọt vào tầm bắn.
 * ---------------------------------------------------------------
 *  Lưu ý     : Class này chỉ chứa logic; phần khởi tạo kết nối server
 *              giữ nguyên như hướng dẫn “Getting Started”.
 * ---------------------------------------------------------------
 */

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Bullet;
import jsclub.codefest.sdk.model.support_items.SupportItem;

import java.io.IOException;
import java.util.*;

public class StepHandler_Joker {

    /* -------------- CẤU HÌNH HÀNH VI -------------- */

    /** Khoảng cách tối đa cho phép BOT “đi farm đồ” xa khỏi tâm bo */
    private static final int SAFE_MARGIN = 3;

    /** Ngưỡng máu tự uống SupportItem */
    private static final float LOW_HP_THRESHOLD = 80f;

    /** Random phục vụ đi đường khi kẹt */
    private static final Random RAND = new Random();

    /* -------------- HÀM XỬ LÝ CHÍNH -------------- */

    public static void handleStep(Hero hero) throws IOException {

        /* Cập nhật dữ liệu map và trạng thái hiện tại */
        GameMap map          = hero.getGameMap();
        Inventory inv        = hero.getInventory();
        Player me            = map.getCurrentPlayer();
        if (me == null) return;                       // Chưa spawn → bỏ qua

        Node myPos           = new Node(me.getX(), me.getY());
        List<Node> avoid     = buildAvoidList(map, true);

        /* 1. DÙNG ITEM HỒI MÁU KHI CẦN */
        useSupportIfLowHP(hero, inv, me.getHealth());

        /* 2. TRÁNH ĐẠN ĐANG BAY TỚI */
        if (dodgeBulletIfTargeted(hero, map, myPos))  return;

        /* 3. TRÁNH ENEMY (NPC) ĐANG TIẾP CẬN */
        if (avoidEnemies(hero, map, myPos))           return;

        /* 4. LUÔN Ở TRONG VÙNG SÁNG */
        if (goBackToSafeZone(hero, map, myPos))       return;

        /* 5. NHẶT VŨ KHÍ NẾU CHƯA CÓ (để phòng thân + cộng điểm) */
        if (pickupGunIfNeeded(hero, map, myPos))      return;

        /* 6. NHẶT VẬT PHẨM CÓ GIÁ TRỊ/ĐẬP CHEST (điểm cao) */
        if (pickupValuableItem(hero, map, myPos))     return;
        if (breakChestIfNearby(hero, map, myPos))     return;

        /* 7. BẮN KHI ĐỊCH ĐÃ VÀO TẦM */
        if (shootNearby(hero, map, myPos, inv))       return;

        /* 8. KHÔNG CÓ VIỆC GÌ → ĐI DẠO RANDOM TRONG VÙNG AN TOÀN */
        moveRandomInSafe(hero, map, myPos);
    }

    /* ================== TIỆN ÍCH & HÀNH VI =================== */

    /* Danh sách ô cần né: vật cản cứng, NPC, player khác… */
    private static List<Node> buildAvoidList(GameMap map, boolean avoidEnemies) {
        List<Node> avoid = new ArrayList<>(map.getListIndestructibles());
        avoid.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));

        if (avoidEnemies) {
            // Né Enemy lẫn 8 ô xung quanh Enemy  → tránh va chạm sát thương
            for (Enemy e : map.getListEnemies()) {
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        avoid.add(new Node(e.getX() + dx, e.getY() + dy));
            }
        }
        // Né vị trí các người chơi khác (ta chỉ giao tranh khi họ TỚI GẦN)
        avoid.addAll(map.getOtherPlayerInfo());
        return avoid;
    }

    /* Dùng SupportItem khi máu thấp */
    private static void useSupportIfLowHP(Hero hero,
                                          Inventory inv,
                                          Float hp) throws IOException {
        if (hp != null && hp < LOW_HP_THRESHOLD
                && inv.getListSupportItem() != null
                && !inv.getListSupportItem().isEmpty()) {
            hero.useItem(inv.getListSupportItem().get(0).getId());
        }
    }

    /* Né đạn đang lao thẳng vào mình */
    private static boolean dodgeBulletIfTargeted(Hero hero,
                                                 GameMap map,
                                                 Node cur) throws IOException {
        for (Bullet b : map.getListBullets()) {
            if (b.getDestinationX() == cur.x && b.getDestinationY() == cur.y) {
                moveRandom(hero, map, cur, buildAvoidList(map, true));
                return true;
            }
        }
        return false;
    }

    /* Né Enemy trong phạm vi 1 ô */
    private static boolean avoidEnemies(Hero hero,
                                        GameMap map,
                                        Node cur) throws IOException {
        for (Enemy e : map.getListEnemies()) {
            if (PathUtils.distance(cur, e) <= 1) {
                moveRandom(hero, map, cur, buildAvoidList(map, true));
                return true;
            }
        }
        return false;
    }

    /* Giữ BOT trong Vùng Sáng – tránh mất 5 → xx HP/step ở Vùng Tối :contentReference[oaicite:0]{index=0} */
    private static boolean goBackToSafeZone(Hero hero,
                                            GameMap map,
                                            Node cur) throws IOException {

        int safeRadius = map.getSafeZone();              // bán kính
        int mapSize    = map.getMapSize();
        if (PathUtils.checkInsideSafeArea(cur, safeRadius, mapSize))
            return false;                                // đang an toàn

        // Đi về tâm BO (mapSize/2, mapSize/2) nhưng chừa SAFE_MARGIN ô
        Node center = new Node(mapSize / 2, mapSize / 2);
        List<Node> avoid = buildAvoidList(map, true);

        String path = PathUtils.getShortestPath(
                map, avoid, cur, center, true);

        if (path != null && !path.isEmpty()) {
            hero.move(String.valueOf(path.charAt(0)));
            return true;
        }
        // Nếu không tìm được đường → chạy random (nhưng vẫn giữ trong bo)
        moveRandom(hero, map, cur, avoid);
        return true;
    }

    /* Nhặt súng nếu chưa có để +điểm & phòng thủ */
    private static boolean pickupGunIfNeeded(Hero hero,
                                             GameMap map,
                                             Node cur) throws IOException {

        Inventory inv = hero.getInventory();
        if (inv.getGun() != null) return false;          // đã có súng

        Weapon gun = getClosest(map.getAllGun(), cur);
        if (gun == null) return false;

        return goTo(hero, map, cur, gun, buildAvoidList(map, true));
    }

    /* Chọn Item/Weapon nhiều điểm nhất (PickupPoints / SupportItem.getPoint) */
    private static boolean pickupValuableItem(Hero hero,
                                              GameMap map,
                                              Node cur) throws IOException {

        Node best   = null;
        int bestPt  = -1;

        // Xét vũ khí rải rác trên map
        for (Weapon w : map.getListWeapons()) {
            if (w.getPickupPoints() > bestPt) {
                best   = w;
                bestPt = w.getPickupPoints();
            }
        }

        // Xét SupportItem (vừa cho điểm, vừa hồi máu khi dùng) :contentReference[oaicite:1]{index=1}
        for (SupportItem s : map.getListSupportItems()) {
            if (s.getPoint() > bestPt) {
                best   = s;
                bestPt = s.getPoint();
            }
        }

        if (best == null) return false;
        return goTo(hero, map, cur, best, buildAvoidList(map, true));
    }

    /* Đập chest gần nhất (1 ô) để lấy 4 đồ random + điểm :contentReference[oaicite:2]{index=2} */
    private static boolean breakChestIfNearby(Hero hero,
                                              GameMap map,
                                              Node cur) throws IOException {

        List<Obstacle> chests = map.getObstaclesByTag("DESTRUCTIBLE");
        Obstacle chest        = getClosest(chests, cur);
        if (chest != null && PathUtils.distance(cur, chest) <= 1) {
            return goTo(hero, map, cur, chest, buildAvoidList(map, true));
        }
        return false;
    }

    /* Bắn người chơi KHÁC khi đã lọt vào phạm vi tối đa của súng */
    private static boolean shootNearby(Hero hero,
                                       GameMap map,
                                       Node cur,
                                       Inventory inv) throws IOException {

        Weapon gun = inv.getGun();
        if (gun == null) return false;

        int[] range = gun.getRange();
        int maxR    = (range != null && range.length > 0) ? range[0] : 3;

        Player target = map.getOtherPlayerInfo().stream()
                .filter(p -> p.getHealth() != null && p.getHealth() > 0)
                .filter(p -> PathUtils.distance(cur, p) <= maxR)
                .min(Comparator.comparingDouble(p -> PathUtils.distance(cur, p)))
                .orElse(null);

        if (target == null) return false;    // Chưa ai vào tầm → không bắn

        String dir = getDirection(cur, target);
        hero.shoot(dir);                     // Bắn 1 viên rồi dừng
        return true;
    }

    /* Đi random trong BO khi “rảnh” */
    private static void moveRandomInSafe(Hero hero,
                                         GameMap map,
                                         Node cur) throws IOException {

        List<Node> avoid = buildAvoidList(map, true);
        moveRandom(hero, map, cur, avoid);
    }

    /* --------------------------------------------------------
       Các HÀM TIỆN ÍCH kế thừa/đơn giản hoá từ BaseBotLogic
       -------------------------------------------------------- */

    private static <T extends Node> T getClosest(List<T> list, Node from) {
        return list.stream()
                .min(Comparator.comparingDouble(n -> PathUtils.distance(from, n)))
                .orElse(null);
    }

    private static String getDirection(Node from, Node to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? "r" : "l";
        if (dy != 0)                      return dy > 0 ? "d" : "u";
        return "";
    }

    private static boolean goTo(Hero hero,
                                GameMap map,
                                Node from,
                                Node to,
                                List<Node> avoid) throws IOException {

        if (from.x == to.x && from.y == to.y) {
            hero.pickupItem();                         // Đứng đúng ô đồ → nhặt
            return true;
        }
        String path = PathUtils.getShortestPath(map, avoid, from, to, false);
        if (path != null && !path.isEmpty()) {
            hero.move(String.valueOf(path.charAt(0)));
            return true;
        }
        return false;
    }

    private static Node getNext(Node cur, char d) {
        int x = cur.x, y = cur.y;
        if (d == 'l') x--;
        if (d == 'r') x++;
        if (d == 'u') y--;
        if (d == 'd') y++;
        return new Node(x, y);
    }

    private static boolean isBlocked(Node n, GameMap map, List<Node> avoid) {
        int size = map.getMapSize();
        if (n.x < 0 || n.y < 0 || n.x >= size || n.y >= size) return true;
        return avoid.stream().anyMatch(b -> b.x == n.x && b.y == n.y);
    }

    private static void moveRandom(Hero hero,
                                   GameMap map,
                                   Node cur,
                                   List<Node> avoid) throws IOException {

        String[] dir = {"l", "r", "u", "d"};
        Collections.shuffle(Arrays.asList(dir), RAND);
        for (String d : dir) {
            Node next = getNext(cur, d.charAt(0));
            if (!isBlocked(next, map, avoid)) {
                hero.move(d);
                return;
            }
        }
    }
}