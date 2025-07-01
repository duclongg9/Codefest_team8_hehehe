// StepHandler_ZoneDominator.java
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

    private static final int SAFE_HP = 60;
    private static final int CONTROL_RADIUS = 6;

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();
        List<Node> avoid = getAvoidNodes(gameMap, player.getHealth() < SAFE_HP);

        // 1. Nếu chưa có súng → loot
        if (inv.getGun() == null) {
            Weapon gun = getClosest(gameMap.getAllGun(), me);
            if (gun != null && goTo(hero, gameMap, me, gun, avoid)) return;
        }

        // 2. Nếu máu thấp → tìm hồi máu
        if (player.getHealth() < SAFE_HP) {
            HealingItem heal = getClosest(gameMap.getListHealingItems(), me);
            if (heal != null && goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 3. Nếu có thính gần rơi, đoán vị trí và đến chiếm
        // (giả định bot biết rồng bay theo đường từ top->bottom, cố thủ vùng trung tâm)
        Node center = new Node(gameMap.getMapSize() / 2, gameMap.getMapSize() / 2);
        if (PathUtils.distance(me, center) > CONTROL_RADIUS) {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 4. Nếu có player lảng vảng khu trung tâm → bắn
        if (inv.getGun() != null) {
            Player intruder = getTargetInCenter(gameMap.getOtherPlayerInfo(), center, inv.getGun().getRange());
            if (intruder != null) {
                hero.shoot(getDirection(me, intruder));
                return;
            }
        }

        // 5. Luôn trong bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 6. Nếu đang ở trung tâm rồi → đứng canh
        // Không cần di chuyển thêm
    }

    private static List<Node> getAvoidNodes(GameMap map, boolean avoidEnemies) {
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

    private static <T extends Node> T getClosest(List<T> list, Node from) {
        return list.stream()
                .min(Comparator.comparingDouble(n -> PathUtils.distance(from, n)))
                .orElse(null);
    }

    private static Player getTargetInCenter(List<Player> players, Node center, int range) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 70)
                .filter(p -> PathUtils.distance(center, p) <= CONTROL_RADIUS)
                .filter(p -> PathUtils.distance(center, p) <= range)
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

    private static String getDirection(Node from, Node to) {
        if (to.x < from.x) return "l";
        if (to.x > from.x) return "r";
        if (to.y < from.y) return "u";
        if (to.y > from.y) return "d";
        return "";
    }
}