// StepHandler_RogueCollector.java
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;

import java.io.IOException;
import java.util.*;

public class StepHandler {

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;
        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();

        List<Node> avoid = getAvoidNodes(gameMap);

        // 1. Ưu tiên nhặt súng nếu chưa có
        if (inv.getGun() == null) {
            Weapon gun = getClosest(gameMap.getAllGun(), me);
            if (gun != null && goTo(hero, gameMap, me, gun, avoid)) return;
        }

        // 2. Ưu tiên rương nếu có thể (collector mode)
        Obstacle chest = getClosest(gameMap.getListChests(), me);
        if (chest != null && goTo(hero, gameMap, me, chest, avoid)) return;

        // 3. Nhặt healing nếu máu < 60
        if (player.getHealth() < 60) {
            HealingItem heal = getClosest(gameMap.getListHealingItems(), me);
            if (heal != null && goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 4. Bắn chỉ khi bị chắn đường hoặc địch yếu
        if (inv.getGun() != null) {
            Player block = getWeakPlayerNearby(gameMap.getOtherPlayerInfo(), me, inv.getGun().getRange());
            if (block != null) {
                hero.shoot(getDirection(me, block));
                return;
            }
        }

        // 5. Luôn đảm bảo trong bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize()/2, gameMap.getMapSize()/2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 6. Nếu không có gì: di chuyển an toàn
        moveRandom(hero, gameMap, me, avoid);
    }

    private static List<Node> getAvoidNodes(GameMap map) {
        List<Node> avoid = new ArrayList<>(map.getListIndestructibles());
        avoid.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));
        for (Enemy e : map.getListEnemies()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    avoid.add(new Node(e.getX() + dx, e.getY() + dy));
                }
            }
        }
        avoid.addAll(map.getOtherPlayerInfo());
        return avoid;
    }

    private static <T extends Node> T getClosest(List<T> list, Node from) {
        return list.stream()
                .min(Comparator.comparingDouble(n -> PathUtils.distance(from, n)))
                .orElse(null);
    }

    private static Player getWeakPlayerNearby(List<Player> players, Node from, int range) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 40)
                .filter(p -> PathUtils.distance(from, p) <= range)
                .findFirst()
                .orElse(null);
    }

    private static boolean goTo(Hero hero, GameMap map, Node from, Node to, List<Node> avoid) throws IOException {
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

    private static void moveRandom(Hero hero, GameMap map, Node current, List<Node> avoid) throws IOException {
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

    private static String getDirection(Node from, Node to) {
        if (to.x < from.x) return "l";
        if (to.x > from.x) return "r";
        if (to.y < from.y) return "u";
        if (to.y > from.y) return "d";
        return "";
    }
}