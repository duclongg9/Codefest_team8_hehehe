import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.equipments.HealingItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.ElementType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StepHandler {
    private static final Random RANDOM = new Random();

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;

        var player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

        Node current = new Node(player.getX(), player.getY());
        List<Node> avoid = getRestrictedNodes(gameMap);
        Inventory inv = hero.getInventory();

        if (inv.getGun() == null) {
            Weapon nearestGun = getNearestWeapon(gameMap.getAllGun(), current);
            if (nearestGun != null) {
                moveToTarget(hero, gameMap, current, nearestGun, avoid);
                return;
            }
        }

        Enemy nearestEnemy = getNearestEnemy(gameMap.getListEnemies(), current);
        if (nearestEnemy != null && PathUtils.distance(current, nearestEnemy) == 1 && player.getHealth() > 50) {
            hero.attack(getDirection(current, nearestEnemy));
            return;
        }

        if (player.getHealth() < 50) {
            HealingItem heal = getNearestHealing(gameMap.getListHealingItems(), current);
            if (heal != null) {
                moveToTarget(hero, gameMap, current, heal, avoid);
                return;
            }
        }

        if (!PathUtils.checkInsideSafeArea(current, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize() / 2, gameMap.getMapSize() / 2);
            String path = PathUtils.getShortestPath(gameMap, avoid, current, center, false);
            if (path != null && !path.isEmpty()) {
                moveOneStep(hero, gameMap, current, path.charAt(0));
                return;
            }
        }

        Obstacle chest = getNearestChest(gameMap.getListChests(), current);
        if (chest != null && PathUtils.distance(current, chest) <= 1) {
            if (PathUtils.distance(current, chest) == 0) {
                hero.attack("u");
                hero.pickupItem();
            } else {
                hero.move(getDirection(current, chest));
            }
            return;
        }

        moveRandom(hero, gameMap, current);
    }

    private static List<Node> getRestrictedNodes(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        return nodes;
    }

    private static Weapon getNearestWeapon(List<Weapon> weapons, Node current) {
        Weapon nearest = null;
        double min = Double.MAX_VALUE;
        for (Weapon w : weapons) {
            double d = PathUtils.distance(current, w);
            if (d < min) {
                min = d;
                nearest = w;
            }
        }
        return nearest;
    }

    private static HealingItem getNearestHealing(List<HealingItem> items, Node current) {
        HealingItem nearest = null;
        double min = Double.MAX_VALUE;
        for (HealingItem h : items) {
            double d = PathUtils.distance(current, h);
            if (d < min) {
                min = d;
                nearest = h;
            }
        }
        return nearest;
    }

    private static Enemy getNearestEnemy(List<Enemy> enemies, Node current) {
        Enemy nearest = null;
        double min = Double.MAX_VALUE;
        for (Enemy e : enemies) {
            double d = PathUtils.distance(current, e);
            if (d < min) {
                min = d;
                nearest = e;
            }
        }
        return nearest;
    }

    private static Obstacle getNearestChest(List<Obstacle> chests, Node current) {
        Obstacle nearest = null;
        double min = Double.MAX_VALUE;
        for (Obstacle c : chests) {
            if (c.getType() == ElementType.CHEST) {
                double d = PathUtils.distance(current, c);
                if (d < min) {
                    min = d;
                    nearest = c;
                }
            }
        }
        return nearest;
    }

    private static void moveToTarget(Hero hero, GameMap map, Node current, Node target, List<Node> avoid) throws IOException {
        String path = PathUtils.getShortestPath(map, avoid, current, target, false);
        if (path == null) {
            moveRandom(hero, map, current);
            return;
        }
        if (path.isEmpty()) {
            hero.pickupItem();
            return;
        }
        moveOneStep(hero, map, current, path.charAt(0));
    }

    private static void moveOneStep(Hero hero, GameMap map, Node current, char dir) throws IOException {
        Node next = new Node(current.x, current.y);
        if (dir == 'l') next.x--;
        if (dir == 'r') next.x++;
        if (dir == 'u') next.y--;
        if (dir == 'd') next.y++;
        if (isBlocked(map, next)) {
            moveRandom(hero, map, current);
        } else {
            hero.move(String.valueOf(dir));
        }
    }

    private static boolean isBlocked(GameMap map, Node node) {
        List<Node> blocks = new ArrayList<>(map.getListIndestructibles());
        blocks.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));
        for (Node n : blocks) {
            if (n.x == node.x && n.y == node.y) return true;
        }
        return node.x < 0 || node.y < 0 || node.x >= map.getMapSize() || node.y >= map.getMapSize();
    }

    private static void moveRandom(Hero hero, GameMap map, Node current) throws IOException {
        String[] dirs = {"l", "r", "u", "d"};
        for (int i = 0; i < 4; i++) {
            String d = dirs[RANDOM.nextInt(dirs.length)];
            Node next = new Node(current.x, current.y);
            if (d.equals("l")) next.x--;
            if (d.equals("r")) next.x++;
            if (d.equals("u")) next.y--;
            if (d.equals("d")) next.y++;
            if (!isBlocked(map, next)) {
                hero.move(d);
                return;
            }
        }
    }

    private static String getDirection(Node from, Node to) {
        if (to.x < from.x) return "l";
        if (to.x > from.x) return "r";
        if (to.y < from.y) return "u";
        if (to.y > from.y) return "d";
        return "";
    }
}