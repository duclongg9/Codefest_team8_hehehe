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

import java.io.IOException;
import java.util.*;

public class BaseBotLogic {

    private static final Random RAND = new Random();

    // --- Helper utilities ---
    public static List<Node> buildAvoidList(GameMap map, boolean avoidEnemies) {
        List<Node> avoid = new ArrayList<>(map.getListIndestructibles());
        avoid.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));
        if (avoidEnemies) {
            for (Enemy e : map.getListEnemies()) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        avoid.add(new Node(e.getX() + dx, e.getY() + dy));
                    }
                }
            }
        }
        avoid.addAll(map.getOtherPlayerInfo());
        return avoid;
    }

    public static <T extends Node> T getClosest(List<T> list, Node from) {
        return list.stream()
                .min(Comparator.comparingDouble(n -> PathUtils.distance(from, n)))
                .orElse(null);
    }

    public static String getDirection(Node from, Node to) {
        if (to.x < from.x) return "l";
        if (to.x > from.x) return "r";
        if (to.y < from.y) return "u";
        if (to.y > from.y) return "d";
        return "";
    }

    public static boolean goTo(Hero hero, GameMap map, Node from, Node to, List<Node> avoid) throws IOException {
        if (from.x == to.x && from.y == to.y) {
            hero.pickupItem();
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
        if (n.x < 0 || n.y < 0 || n.x >= map.getMapSize() || n.y >= map.getMapSize()) return true;
        for (Node b : avoid) {
            if (b.x == n.x && b.y == n.y) return true;
        }
        return false;
    }

    public static void moveRandom(Hero hero, GameMap map, Node current, List<Node> avoid) throws IOException {
        String[] dirs = {"l", "r", "u", "d"};
        Collections.shuffle(Arrays.asList(dirs));
        for (String d : dirs) {
            Node next = getNext(current, d.charAt(0));
            if (!isBlocked(next, map, avoid)) {
                hero.move(d);
                return;
            }
        }
    }

    // --- Basic behaviours ---
    public static boolean pickupGunIfNeeded(Hero hero, GameMap map, Node current) throws IOException {
        Inventory inv = hero.getInventory();
        if (inv.getGun() != null) return false;
        Weapon gun = getClosest(map.getAllGun(), current);
        if (gun != null) {
            return goTo(hero, map, current, gun, buildAvoidList(map, true));
        }
        return false;
    }

    public static boolean avoidEnemies(Hero hero, GameMap map, Node current) throws IOException {
        for (Enemy e : map.getListEnemies()) {
            if (PathUtils.distance(current, e) <= 1) {
                moveRandom(hero, map, current, buildAvoidList(map, true));
                return true;
            }
        }
        return false;
    }

    public static boolean shootNearby(Hero hero, GameMap map, Node current, Inventory inv) throws IOException {
        if (inv.getGun() == null) return false;
        Player target = map.getOtherPlayerInfo().stream()
                .filter(p -> p.getHealth() != null)
                .filter(p -> PathUtils.distance(current, p) <= inv.getGun().getRange())
                .min(Comparator.comparingDouble(p -> PathUtils.distance(current, p)))
                .orElse(null);
        if (target != null) {
            hero.shoot(getDirection(current, target));
            return true;
        }
        return false;
    }

    public static boolean breakChestIfNearby(Hero hero, GameMap map, Node current) throws IOException {
        Obstacle chest = getClosest(map.getListChests(), current);
        if (chest != null && PathUtils.distance(current, chest) <= 1) {
            return goTo(hero, map, current, chest, buildAvoidList(map, true));
        }
        return false;
    }

    public static boolean dodgeBulletIfTargeted(Hero hero, GameMap map, Node current) throws IOException {
        for (Bullet b : map.getListBullets()) {
            if (b.getDestinationX() == current.x && b.getDestinationY() == current.y) {
                moveRandom(hero, map, current, buildAvoidList(map, true));
                return true;
            }
        }
        return false;
    }
}