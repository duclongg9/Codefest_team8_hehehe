import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.obstacles.Obstacle;

import java.io.IOException;
import java.util.*;

/**
 * A more complete strategy inspired from the example MapUpdateListener.
 * Combines looting, healing, combat and movement logic.
 */
public class StepHandler_Superman {

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

        Node me = new Node(player.getX(), player.getY());
        List<Node> avoid = getNodesToAvoid(gameMap);

        if (player.getHealth() < 50) {
            moveToNearestSupportItem(gameMap, hero, me, avoid);
            return;
        }

        if (hero.getInventory().getGun() == null) {
            moveToNearestGun(gameMap, hero, me, avoid);
            return;
        }

        Player enemy = findClosestEnemy(gameMap, me);
        if (enemy != null && isInRange(me, enemy)) {
            shootAt(hero, me, enemy);
            return;
        }

        if (openChestIfNearby(gameMap, hero, me, avoid)) return;

        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            moveToSafeZone(gameMap, hero, me, avoid);
            return;
        }

        if (shouldMoveToAirDrop(gameMap)) {
            moveToAirDrop(gameMap, hero, me, avoid);
            return;
        }

        moveRandomly(gameMap, hero, me, avoid);
    }

    // ------------ Các hành động cụ thể ------------

    private static void moveToNearestGun(GameMap map, Hero hero, Node player, List<Node> avoid) throws IOException {
        Weapon gun = getNearestWeapon(map.getAllGun(), player);
        if (gun == null) return;
        moveOrPickup(hero, map, player, gun, avoid);
    }

    private static void moveToNearestSupportItem(GameMap map, Hero hero, Node player, List<Node> avoid) throws IOException {
        SupportItem item = getNearestItem(map.getListSupportItems(), player);
        if (item == null) return;
        moveOrPickup(hero, map, player, item, avoid);
    }

    private static void moveToSafeZone(GameMap map, Hero hero, Node player, List<Node> avoid) throws IOException {
        Node center = new Node(map.getMapSize() / 2, map.getMapSize() / 2);
        String path = PathUtils.getShortestPath(map, avoid, player, center, false);
        if (path != null && !path.isEmpty()) hero.move(path);
    }

    private static void moveToAirDrop(GameMap map, Hero hero, Node player, List<Node> avoid) throws IOException {
        Node drop = predictAirDropPosition(map);
        if (drop == null) return;
        String path = PathUtils.getShortestPath(map, avoid, player, drop, false);
        if (path != null && !path.isEmpty()) hero.move(path);
    }

    private static void moveRandomly(GameMap map, Hero hero, Node player, List<Node> avoid) throws IOException {
        String[] dirs = {"l", "r", "u", "d"};
        for (String d : dirs) {
            Node next = nextNode(player, d);
            if (!contains(avoid, next)) {
                hero.move(d);
                return;
            }
        }
    }

    private static void shootAt(Hero hero, Node self, Player enemy) throws IOException {
        int dx = enemy.getX() - self.getX();
        int dy = enemy.getY() - self.getY();
        String dir = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "r" : "l") : (dy > 0 ? "d" : "u");
        hero.shoot(dir);
    }

    // ------------ Tiện ích hỗ trợ ------------

    private static Weapon getNearestWeapon(List<Weapon> list, Node player) {
        return list.stream()
                .min(Comparator.comparingDouble(w -> PathUtils.distance(player, w)))
                .orElse(null);
    }

    private static SupportItem getNearestItem(List<SupportItem> list, Node player) {
        return list.stream()
                .min(Comparator.comparingDouble(i -> PathUtils.distance(player, i)))
                .orElse(null);
    }

    private static Player findClosestEnemy(GameMap map, Node player) {
        return map.getOtherPlayerInfo().stream()
                .filter(p -> p.getHealth() != null && p.getHealth() > 0)
                .min(Comparator.comparingDouble(p -> PathUtils.distance(player, p)))
                .orElse(null);
    }

    private static boolean isInRange(Node self, Player target) {
        return PathUtils.distance(self, target) <= 6;
    }


    private static Node predictAirDropPosition(GameMap map) {
        return new Node(map.getMapSize() / 2, map.getMapSize() / 2);
    }

    private static void moveOrPickup(Hero hero, GameMap map, Node player, Node target, List<Node> avoid) throws IOException {
        String path = PathUtils.getShortestPath(map, avoid, player, target, false);
        if (path != null) {
            if (path.isEmpty()) hero.pickupItem();
            else hero.move(path);
        }
    }

    private static List<Node> getNodesToAvoid(GameMap map) {
        List<Node> avoid = new ArrayList<>(map.getListIndestructibles());
        avoid.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));
        avoid.addAll(map.getOtherPlayerInfo());
        return avoid;
    }

    private static boolean openChestIfNearby(GameMap map, Hero hero, Node me, List<Node> avoid) throws IOException {
        for (Obstacle o : map.getListObstacles()) {
            if (o.getType() == ElementType.CHEST && PathUtils.distance(me, o) <= 1) {
                return BaseBotLogic.goTo(hero, map, me, o, avoid);
            }
        }
        return false;
    }

    private static boolean shouldMoveToAirDrop(GameMap map) {
        // Simple placeholder logic. In absence of real air drop data just return false
        return false;
    }

    private static Node nextNode(Node cur, String dir) {
        int x = cur.getX();
        int y = cur.getY();
        switch (dir) {
            case "l": x--; break;
            case "r": x++; break;
            case "u": y--; break;
            case "d": y++; break;
        }
        return new Node(x, y);
    }

    private static boolean contains(List<Node> list, Node n) {
        for (Node other : list) {
            if (other.getX() == n.getX() && other.getY() == n.getY()) return true;
        }
        return false;
    }
}