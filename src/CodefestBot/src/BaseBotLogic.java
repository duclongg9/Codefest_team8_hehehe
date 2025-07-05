

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Bullet;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.Inventory;

import java.io.IOException;
import java.util.*;

/**
 * ==============================================================
 *  BaseBotLogic 2.1 – “human‑like core skills”
 *  ✦ PERCEPTION  : quét map, xây avoidList
 *  ✦ REASONING   : path‑finding, getDirection
 *  ✦ ACTION      : move / shoot / attack / pickup
 *  ✦ SURVIVAL    : dodge bullet, né NPC, heal
 *  ✦ ANTI‑STUCK  : tự phát hiện & gỡ kẹt
 *  ✦ CHEST‑FRIEND: obstacle “DESTRUCTIBLE” không còn cấm đường
 * ==============================================================
 *  Mọi hàm static → import dễ dàng cho các StepHandler.
 */
public final class BaseBotLogic {

    /* ---- cấu hình chung ---- */
    private static final Random RAND = new Random();
    private static final int SAFE_HP = 100;
    private static final int DODGE_RANGE = 1;
    private static final int STUCK_WINDOW = 6;

    private static final Deque<Node> POS_BUFFER = new ArrayDeque<>();

    /* ------------ PERCEPTION ------------ */

    public static List<Node> buildAvoidList(GameMap map, boolean avoidNPC) {
        List<Node> res = new ArrayList<>(map.getListIndestructibles());

        // Không né CHEST để có thể tiếp cận & phá
        res.removeAll(map.getObstaclesByTag("DESTRUCTIBLE"));

        // Không né vật cản đi xuyên được
        res.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));

        if (avoidNPC) {
            for (Enemy e : map.getListEnemies())
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        res.add(new Node(e.getX() + dx, e.getY() + dy));
        }
        res.addAll(map.getOtherPlayerInfo());
        return res;
    }

    public static <T extends Node> T getClosest(List<T> list, Node from) {
        return list == null || list.isEmpty() ? null :
                list.stream().min(Comparator.comparingDouble(n -> PathUtils.distance(from, n))).orElse(null);
    }

    /* ------------ REASONING ------------ */

    public static String getDirection(Node from, Node to) {
        int dx = to.x - from.x, dy = to.y - from.y;
        return Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "r" : "l")
                : (dy > 0 ? "d" : "u");
    }

    public static boolean goTo(Hero hero, GameMap map, Node cur, Node tgt,
                               List<Node> avoid) throws IOException {

        if (cur.x == tgt.x && cur.y == tgt.y) {
            hero.pickupItem();
            return true;
        }

        String path = PathUtils.getShortestPath(map, avoid, cur, tgt, false);
        if (path == null || path.isEmpty()) return false;

        char dir = path.charAt(0);

        /* nếu ô kế tiếp là CHEST → đập trước rồi nhặt */
        if (tryBreakObstacle(hero, map, cur, dir)) return true;

        Node nxt = getNext(cur, dir);
        if (isBlocked(nxt, map, avoid)) {     // tránh húc tường
            moveRandom(hero, map, cur, avoid);
            return true;
        }
        hero.move(String.valueOf(dir));
        return true;
    }

    /* ------------ ACTION ------------ */

    public static void moveRandom(Hero hero, GameMap map, Node cur,
                                  List<Node> avoid) throws IOException {
        String[] d = {"l","r","u","d"};
        Collections.shuffle(Arrays.asList(d), RAND);
        for (String s : d) {
            Node nxt = getNext(cur, s.charAt(0));
            if (!isBlocked(nxt, map, avoid)) { hero.move(s); break; }
        }
    }

    public static boolean shootNearby(Hero hero, GameMap map,
                                      Node cur, Inventory inv) throws IOException {
        Weapon gun = inv.getGun();
        if (gun == null) return false;

        int maxR = gun.getRange() != null ? gun.getRange()[0] : 3;
        Player tgt = map.getOtherPlayerInfo().stream()
                .filter(p -> p.getHealth() != null)
                .filter(p -> PathUtils.distance(cur, p) <= maxR)
                .min(Comparator.comparingDouble(p -> PathUtils.distance(cur, p)))
                .orElse(null);

        if (tgt != null) {
            hero.shoot(getDirection(cur, tgt));
            return true;
        }
        return false;
    }

    /* ------------ SURVIVAL ------------ */

    public static void useSupportIfLowHP(Hero hero, Inventory inv, Float hp) throws IOException {
        if (hp != null && hp < SAFE_HP &&
                inv.getListSupportItem() != null && !inv.getListSupportItem().isEmpty()) {
            hero.useItem(inv.getListSupportItem().get(0).getId());
        }
    }

    public static boolean dodgeBulletIfTargeted(Hero hero, GameMap map, Node cur) throws IOException {
        for (Bullet b : map.getListBullets())
            if (b.getDestinationX() == cur.x && b.getDestinationY() == cur.y) {
                moveRandom(hero, map, cur, buildAvoidList(map, true));
                return true;
            }
        return false;
    }

    public static boolean avoidEnemies(Hero hero, GameMap map, Node cur) throws IOException {
        for (Enemy e : map.getListEnemies())
            if (PathUtils.distance(cur, e) <= DODGE_RANGE) {
                moveRandom(hero, map, cur, buildAvoidList(map, true));
                return true;
            }
        return false;
    }

    /* ------------ BASIC BEHAVIOURS ------------ */

    public static boolean pickupGunIfNeeded(Hero hero, GameMap map, Node cur) throws IOException {
        if (hero.getInventory().getGun() != null) return false;
        Weapon gun = getClosest(map.getAllGun(), cur);
        return gun != null && goTo(hero, map, cur, gun, buildAvoidList(map, true));
    }

    public static boolean pickupValuableItem(Hero hero, GameMap map, Node cur) throws IOException {
        Node best = null; int point = -1;

        for (Weapon w : map.getListWeapons())
            if (w.getPickupPoints() > point) { point = w.getPickupPoints(); best = w; }

        for (SupportItem s : map.getListSupportItems())
            if (s.getPoint() > point) { point = s.getPoint(); best = s; }

        return best != null && goTo(hero, map, cur, best, buildAvoidList(map, true));
    }

    public static boolean breakChestIfNearby(Hero hero, GameMap map, Node cur) throws IOException {
        Obstacle chest = getClosest(map.getObstaclesByTag("DESTRUCTIBLE"), cur);
        return chest != null && PathUtils.distance(cur, chest) <= 1 &&
                goTo(hero, map, cur, chest, buildAvoidList(map, true));
    }

    /* ------------ ANTI‑STUCK ------------ */

    public static boolean isStuck(Node cur) {
        POS_BUFFER.addLast(new Node(cur.x, cur.y));
        if (POS_BUFFER.size() > STUCK_WINDOW) POS_BUFFER.removeFirst();
        return POS_BUFFER.stream().distinct().count() == 1;
    }

    public static void resolveStuck(Hero h, GameMap m, Node cur,
                                    Node tgt, List<Node> avoid) throws IOException {
        String p = tgt == null ? null :
                PathUtils.getShortestPath(m, avoid, cur, tgt, true);
        if (p != null && !p.isEmpty()) h.move(""+p.charAt(0));
        else moveRandom(h, m, cur, avoid);
    }

    /* ------------ PRIVATE SUPPORT ------------ */

    private static Node getNext(Node n, char d) {
        return switch (d) {
            case 'l' -> new Node(n.x - 1, n.y);
            case 'r' -> new Node(n.x + 1, n.y);
            case 'u' -> new Node(n.x,     n.y - 1);
            default  -> new Node(n.x,     n.y + 1);
        };
    }
    private static boolean isBlocked(Node n, GameMap m, List<Node> avoid) {
        int size = m.getMapSize();
        if (n.x < 0 || n.y < 0 || n.x >= size || n.y >= size) return true;
        return avoid.stream().anyMatch(b -> b.x == n.x && b.y == n.y);
    }
    private static boolean tryBreakObstacle(Hero hero, GameMap map, Node from, char dir) throws IOException {
        Node nxt = getNext(from, dir);
        Obstacle ob = map.getListObstacles().stream()
                .filter(o -> o.getX() == nxt.x && o.getY() == nxt.y)
                .findFirst().orElse(null);
        if (ob != null && ob.getTags().contains("DESTRUCTIBLE")) {
            hero.attack(String.valueOf(dir));   // đập rương
            return true;
        }
        return false;
    }
}
