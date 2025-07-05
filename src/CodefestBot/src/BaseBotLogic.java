

import jsclub.codefest.sdk.*;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.*;
import jsclub.codefest.sdk.model.weapon.*;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;

import java.io.IOException;
import java.util.*;

/** BaseBotLogic v3.0 – kỹ năng chung cho mọi bot */
public final class BaseBotLogic {

    /* -------- CONFIG -------- */
    private static final int HEAL_THRESHOLD = 70;
    private static final int STUCK_WINDOW   = 6;
    private static final int DANGER_RADIUS  = 3;
    private static final Random RAND = new Random();

    /* -------- STATE -------- */
    private static final Deque<Node> POS_BUF = new ArrayDeque<>();
    private static final Set<String> RECENT_GUNS = new LinkedHashSet<>();
    private static int gunCooldown = 0;

    /* ==============================================================
     *  UTILITIES  ---------------------------------------------------
     * ============================================================ */
    public static List<Node> buildAvoidList(GameMap map, boolean avoidNPC) {
        List<Node> list = new ArrayList<>(map.getListIndestructibles());

        // Giữ obstacle phá được, trừ CHEST
        for (Obstacle o : map.getObstaclesByTag("DESTRUCTIBLE"))
            if (!o.getTags().contains("PULLABLE_ROPE"))
                list.add(o);

        if (avoidNPC)
            for (Enemy e : map.getListEnemies())
                for (int dx = -DANGER_RADIUS; dx <= DANGER_RADIUS; dx++)
                    for (int dy = -DANGER_RADIUS; dy <= DANGER_RADIUS; dy++)
                        list.add(new Node(e.getX() + dx, e.getY() + dy));

        list.addAll(map.getOtherPlayerInfo());
        return list;
    }

    public static <T extends Node> T getClosest(List<T> list, Node from) {
        return (list == null || list.isEmpty()) ? null
                : list.stream().min(Comparator.comparingDouble(n -> PathUtils.distance(from, n))).orElse(null);
    }

    /** l/r/u/d theo khoảng cách lớn hơn */
    public static String dir(Node a, Node b) {
        int dx = b.x - a.x, dy = b.y - a.y;
        return Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "r" : "l") : (dy > 0 ? "u" : "d");
    }

    /* -------- coordinate helper -------- */
    private static Node next(Node n, char d) {
        return switch (d) {
            case 'l' -> new Node(n.x - 1, n.y);
            case 'r' -> new Node(n.x + 1, n.y);
            case 'u' -> new Node(n.x, n.y + 1);  // trục Y ngược, giữ như bản gốc
            default  -> new Node(n.x, n.y - 1);  // 'd'
        };
    }
    private static boolean isBlocked(Node n, GameMap m, List<Node> avoid) {
        int s = m.getMapSize();
        if (n.x < 0 || n.y < 0 || n.x >= s || n.y >= s) return true;
        return avoid.stream().anyMatch(b -> b.x == n.x && b.y == n.y);
    }

    private static void autoPickupAround(Hero h) throws IOException {
        for (int i = 0; i < 4; i++) h.pickupItem();
    }

    /* ==============================================================
     *  CORE MOVE / STUCK
     * ============================================================ */
    public static boolean goTo(Hero h, GameMap m, Node cur, Node tgt, List<Node> avoid)
            throws IOException {
        if (cur.equals(tgt)) { h.pickupItem(); autoPickupAround(h); return true; }

        String path = PathUtils.getShortestPath(m, avoid, cur, tgt, false);
        if (path == null || path.isEmpty()) return false;
        char d = path.charAt(0);

        if (tryBreakChestAhead(h, m, cur, d)) return true;

        Node nxt = next(cur, d);
        if (isBlocked(nxt, m, avoid)) {
            String alt = PathUtils.getShortestPath(m, avoid, cur, tgt, true);
            if (alt != null && !alt.isEmpty() && alt.charAt(0) != d) {
                h.move("" + alt.charAt(0));
                return true;
            }
            moveRandom(h, m, cur, avoid); return true;
        }
        h.move("" + d);
        return true;
    }

    public static void moveRandom(Hero h, GameMap m, Node cur, List<Node> avoid)
            throws IOException {
        List<String> dirs = new ArrayList<>(List.of("l", "r", "u", "d"));
        Collections.shuffle(dirs, RAND);
        for (String s : dirs) {
            Node n = next(cur, s.charAt(0));
            if (!isBlocked(n, m, avoid)) { h.move(s); return; }
        }
        resolveStuck(h, m, cur, getClosest(m.getObstaclesByTag("PULLABLE_ROPE"), cur), avoid);
    }

    public static boolean isStuck(Node cur) {
        POS_BUF.addLast(new Node(cur.x, cur.y));
        if (POS_BUF.size() > STUCK_WINDOW) POS_BUF.removeFirst();
        return POS_BUF.stream().distinct().count() == 1;
    }
    public static void resolveStuck(Hero h, GameMap m, Node cur, Node tgt, List<Node> avoid)
            throws IOException {
        if (tgt != null && goTo(h, m, cur, tgt, avoid)) return;
        moveRandom(h, m, cur, avoid);
    }

    /* ==============================================================
     *         S U R V I V A L
     * ============================================================ */
    public static void healIfNeeded(Hero h, Inventory inv, float hp) throws IOException {
        if (hp > HEAL_THRESHOLD) return;
        List<SupportItem> items = inv.getListSupportItem();
        if (items == null || items.isEmpty()) return;
        SupportItem best = items.stream()
                .max(Comparator.comparingInt(SupportItem::getHealingHP)).orElse(null);
        h.useItem(best.getId());
    }

    public static boolean keepInsideSafeZone(Hero h, GameMap m, Node cur, List<Node> avoid) throws IOException {
        if (PathUtils.checkInsideSafeArea(cur, m.getSafeZone(), m.getMapSize())) return false;
        Node center = new Node(m.getMapSize() / 2, m.getMapSize() / 2);
        goTo(h, m, cur, center, avoid);
        return true;
    }

    public static boolean avoidEnemies(Hero h, GameMap m, Node cur) throws IOException {
        for (Enemy e : m.getListEnemies()) {
            if (PathUtils.distance(cur, e) <= DANGER_RADIUS) {
                if (!PathUtils.checkInsideSafeArea(cur, m.getSafeZone(), m.getMapSize())) continue;
                String d = dir(e, cur);
                Node nxt = next(cur, d.charAt(0));
                if (!isBlocked(nxt, m, buildAvoidList(m, false))) {
                    h.move(d); return true;
                }
            }
        }
        return false;
    }

    public static boolean dodgeBulletIfTargeted(Hero h, GameMap m, Node cur) throws IOException {
        List<Node> avoid = buildAvoidList(m, true);
        for (Bullet b : m.getListBullets()) {
            int sx=b.getX(), sy=b.getY(), dx=b.getDestinationX(), dy=b.getDestinationY();
            int ddx = dx - sx, ddy = dy - sy;
            String d = Math.abs(ddx) > Math.abs(ddy) ? (ddx > 0 ? "r" : "l") : (ddy > 0 ? "u" : "d");

            boolean danger =
                    (d.equals("r") && sy == cur.y && sx < cur.x && cur.x <= dx) ||
                            (d.equals("l") && sy == cur.y && sx > cur.x && cur.x >= dx) ||
                            (d.equals("u") && sx == cur.x && sy < cur.y && cur.y <= dy) ||
                            (d.equals("d") && sx == cur.x && sy > cur.y && cur.y >= dy) ||
                            (cur.x == dx && cur.y == dy);

            if (!danger) continue;

            String[] pref = d.matches("[lr]") ? new String[]{"u", "d"} : new String[]{"l", "r"};
            for (String p : pref) {
                Node nxt = next(cur, p.charAt(0));
                if (!isBlocked(nxt, m, avoid)) { h.move(p); return true; }
            }
            moveRandom(h, m, cur, avoid); return true;
        }
        return false;
    }

    /* ==============================================================
     *         C H E S T
     * ============================================================ */
    public static boolean breakChestIfNearby(Hero h, GameMap m, Node cur) throws IOException {
        Obstacle chest = getClosest(m.getObstaclesByTag("PULLABLE_ROPE"), cur);
        if (chest == null) return false;
        double d = PathUtils.distance(cur, chest);
        if (d > 1) return false;

        if (d == 0) {                   // đứng trên chest → lùi một ô
            for (char c : new char[]{'l','r','u','d'}) {
                Node n = next(cur, c);
                if (!isBlocked(n, m, buildAvoidList(m,false))) { h.move(""+c); cur = n; break; }
            }
        }
        String dr = dir(cur, new Node(chest.getX(), chest.getY()));
        h.attack(dr);
        h.move(dr);
        autoPickupAround(h);
        return true;
    }
    private static boolean tryBreakChestAhead(Hero h, GameMap m, Node cur, char d) throws IOException {
        Node nxt = next(cur, d);
        Obstacle ob = m.getListObstacles().stream()
                .filter(o -> o.getX() == nxt.x && o.getY() == nxt.y)
                .findFirst().orElse(null);
        if (ob != null && ob.getTags().contains("PULLABLE_ROPE")) {
            h.attack("" + d); return true;
        }
        return false;
    }

    /* ==============================================================
     *         E C O N O M Y
     * ============================================================ */
    public static boolean pickupGunIfBetter(Hero h, GameMap m, Node cur) throws IOException {
        if (gunCooldown > 0) gunCooldown--;
        Weapon best = getClosest(m.getAllGun(), cur);
        if (best == null || "RUBBER_GUN".equals(best.getId())) return false;

        Weapon curG = h.getInventory().getGun();
        if (curG != null && best.getPickupPoints() <= curG.getPickupPoints()) return false;
        if (RECENT_GUNS.contains(best.getId()) && gunCooldown > 0) return false;

        boolean moved = goTo(h, m, cur, best, buildAvoidList(m, false));
        if (moved) { RECENT_GUNS.add(best.getId()); gunCooldown = 6; }
        return moved;
    }

    public static void autoUpgradeGun(Hero h, GameMap m, Node cur) throws IOException {
        Weapon curG = h.getInventory().getGun();
        if (curG == null) return;
        Weapon better = m.getAllGun().stream()
                .filter(g -> g.getPickupPoints() > curG.getPickupPoints())
                .filter(g -> !g.getId().equals(curG.getId()))
                .min(Comparator.comparingInt(Weapon::getPickupPoints))
                .orElse(null);
        if (better != null) goTo(h, m, cur, better, buildAvoidList(m,false));
    }

    public static boolean pickupValuableItem(Hero h, GameMap m, Node cur) throws IOException {
        for (Node n : m.getListWeapons())
            if (PathUtils.distance(cur, n) <= 1)
                return goTo(h,m,cur,n,buildAvoidList(m,false));
        for (Node n : m.getListSupportItems())
            if (PathUtils.distance(cur, n) <= 1)
                return goTo(h,m,cur,n,buildAvoidList(m,false));

        Node best=null; int score=-1;
        for (Weapon w : m.getListWeapons())
            if (w.getPickupPoints()>score){ score=w.getPickupPoints(); best=w; }
        for (SupportItem s : m.getListSupportItems())
            if (s.getPoint()>score){ score=s.getPoint(); best=s; }

        return best!=null && goTo(h, m, cur, best, buildAvoidList(m,false));
    }

    /* ==============================================================
     *         C O M B A T
     * ============================================================ */
    public static boolean shootNearby(Hero h, GameMap m, Node cur, Inventory inv) throws IOException {
        Weapon gun = inv.getGun();
        if (gun == null || gun.getRange() == null || gun.getRange().length == 0) return false;
        int R = gun.getRange()[0];
        Player tgt = m.getOtherPlayerInfo().stream()
                .filter(p -> p.getHealth() != null && p.getHealth() > 0)
                .filter(p -> PathUtils.distance(cur, p) <= R)
                .min(Comparator.comparingDouble(p -> PathUtils.distance(cur, p)))
                .orElse(null);
        if (tgt != null) {
            h.shoot(dir(cur, new Node(tgt.getX(), tgt.getY())));
            return true;
        }
        return false;
    }

    public static boolean attackNearby(Hero h, GameMap m, Node cur) throws IOException {
        Player tgt = m.getOtherPlayerInfo().stream()
                .filter(p -> p.getHealth() != null && p.getHealth() > 0)
                .filter(p -> PathUtils.distance(cur, p) == 1)
                .findFirst()
                .orElse(null);
        if (tgt == null) return false;

        String d = dir(cur, new Node(tgt.getX(), tgt.getY()));
        Inventory inv = h.getInventory();

        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) { h.attack(d); return true; }
        if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) { h.useSpecial(d); return true; }
        h.attack(d); return true;
    }
}
