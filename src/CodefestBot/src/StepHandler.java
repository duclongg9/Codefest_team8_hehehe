import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.equipments.HealingItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.ElementType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StepHandler {

    private static final Random RANDOM = new Random();
    private static final int ENEMY_AVOID_RADIUS = 1;
    private static final int BIG_GUN_DAMAGE = 50;
    private static final int SAFE_ZONE_BUFFER = 2;

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;

        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

        Node current = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();

        boolean avoidEnemies = inv.getGun() == null || player.getHealth() < 40;
        boolean avoidPlayers = inv.getGun() == null;
        List<Node> avoid = getRestrictedNodes(gameMap, avoidEnemies, avoidPlayers);

        if (inv.getGun() == null) {
            Weapon nearestGun = getNearestWeapon(gameMap.getAllGun(), current);
            if (nearestGun != null && moveToTarget(hero, gameMap, current, nearestGun, avoid)) return;
        }

        if (inv.getGun() != null) {
            Enemy shootableEnemy = getShootableEnemy(gameMap, current, inv.getGun().getRange());
            if (shootableEnemy != null) {
                hero.shoot(getDirection(current, shootableEnemy));
                return;
            }

            Player shootablePlayer = getShootablePlayer(gameMap, current, inv.getGun().getRange());
            if (shootablePlayer != null) {
                hero.shoot(getDirection(current, shootablePlayer));
                return;
            }

            Weapon betterGun = getBetterGun(gameMap.getAllGun(), inv.getGun(), current);
            if (betterGun != null && moveToTarget(hero, gameMap, current, betterGun, avoid)) return;
        }

        Player nearestPlayer = getNearestPlayer(gameMap.getOtherPlayerInfo(), current);
        if (nearestPlayer != null) {
            int dist = PathUtils.distance(current, nearestPlayer);
            if (dist == 1 && player.getHealth() > 50) {
                hero.attack(getDirection(current, nearestPlayer));
                return;
            }
            if (moveToTarget(hero, gameMap, current, nearestPlayer, avoid)) return;
        }

        Enemy nearestEnemy = getNearestEnemy(gameMap.getListEnemies(), current);
        if (nearestEnemy != null) {
            int dist = PathUtils.distance(current, nearestEnemy);
            if (dist == 1 && player.getHealth() > 50) {
                hero.attack(getDirection(current, nearestEnemy));
                return;
            }
            if (inv.getGun() != null && player.getHealth() > 50 && moveToTarget(hero, gameMap, current, nearestEnemy, avoid)) return;
        }

        if (player.getHealth() < 50) {
            HealingItem nearestHeal = getNearestHealing(gameMap.getListHealingItems(), current);
            if (nearestHeal != null && moveToTarget(hero, gameMap, current, nearestHeal, avoid)) return;
        }

        Node center = new Node(gameMap.getMapSize() / 2, gameMap.getMapSize() / 2);
        int distToCenter = PathUtils.distance(current, center);
        if (distToCenter >= gameMap.getSafeZone() - SAFE_ZONE_BUFFER) {
            String path = PathUtils.getShortestPath(gameMap, avoid, current, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        if (!PathUtils.checkInsideSafeArea(current, gameMap.getSafeZone(), gameMap.getMapSize())) {
            String path = PathUtils.getShortestPath(gameMap, avoid, current, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        Obstacle chest = getNearestChest(gameMap.getListChests(), current);
        if (chest != null) {
            int dist = PathUtils.distance(current, chest);
            if (dist == 0) {
                hero.pickupItem();
            } else if (dist == 1) {
                hero.attack(getDirection(current, chest));
            } else {
                hero.move(getDirection(current, chest));
            }
            return;
        }

        moveRandom(hero, gameMap, current, avoid);
    }

    private static List<Node> getRestrictedNodes(GameMap map, boolean avoidEnemies, boolean avoidPlayers) {
        List<Node> nodes = new ArrayList<>(map.getListIndestructibles());
        nodes.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));
        if (avoidPlayers) nodes.addAll(map.getOtherPlayerInfo());
        if (avoidEnemies) {
            for (Enemy e : map.getListEnemies()) {
                for (int dx = -ENEMY_AVOID_RADIUS; dx <= ENEMY_AVOID_RADIUS; dx++) {
                    for (int dy = -ENEMY_AVOID_RADIUS; dy <= ENEMY_AVOID_RADIUS; dy++) {
                        int nx = e.getX() + dx, ny = e.getY() + dy;
                        if (nx >= 0 && ny >= 0 && nx < map.getMapSize() && ny < map.getMapSize()) {
                            nodes.add(new Node(nx, ny));
                        }
                    }
                }
            }
        }
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

    private static Weapon getBetterGun(List<Weapon> guns, Weapon currentGun, Node current) {
        Weapon best = null;
        double min = Double.MAX_VALUE;
        int curDamage = currentGun != null ? currentGun.getDamage() : 0;
        for (Weapon g : guns) {
            if (g.getDamage() > curDamage) {
                double d = PathUtils.distance(current, g);
                if (d < min) {
                    min = d;
                    best = g;
                }
            }
        }
        return best;
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

    private static Enemy getShootableEnemy(GameMap map, Node current, int range) {
        Enemy target = null;
        double min = Double.MAX_VALUE;
        for (Enemy e : map.getListEnemies()) {
            double dist = PathUtils.distance(current, e);
            if (dist > 0 && dist <= range && isAligned(current, e) && isClearPath(map, current, e)) {
                if (dist < min) {
                    min = dist;
                    target = e;
                }
            }
        }
        return target;
    }

    private static Player getNearestPlayer(List<Player> players, Node current) {
        Player nearest = null;
        double min = Double.MAX_VALUE;
        for (Player p : players) {
            double d = PathUtils.distance(current, p);
            if (d < min) {
                min = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private static Player getShootablePlayer(GameMap map, Node current, int range) {
        Player target = null;
        double min = Double.MAX_VALUE;
        for (Player p : map.getOtherPlayerInfo()) {
            if (p.getHealth() != null && p.getHealth() <= 0) continue;
            double dist = PathUtils.distance(current, p);
            if (dist > 0 && dist <= range && isAligned(current, p) && isClearPath(map, current, p)) {
                if (dist < min) {
                    min = dist;
                    target = p;
                }
            }
        }
        return target;
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

    private static boolean moveToTarget(Hero hero, GameMap map, Node current, Node target, List<Node> avoid) throws IOException {
        String path = PathUtils.getShortestPath(map, avoid, current, target, false);
        if (path == null || path.isEmpty()) return false;
        hero.move(String.valueOf(path.charAt(0)));
        return true;
    }

    private static void moveRandom(Hero hero, GameMap map, Node current, List<Node> avoid) throws IOException {
        String[] dirs = {"l", "r", "u", "d"};
        for (int i = 0; i < 4; i++) {
            String d = dirs[RANDOM.nextInt(dirs.length)];
            Node next = getNextNode(current, d.charAt(0));
            if (!isBlocked(map, next, avoid)) {
                hero.move(d);
                return;
            }
        }
    }

    private static Node getNextNode(Node current, char dir) {
        Node next = new Node(current.x, current.y);
        if (dir == 'l') next.x--;
        if (dir == 'r') next.x++;
        if (dir == 'u') next.y--;
        if (dir == 'd') next.y++;
        return next;
    }

    private static boolean isBlocked(GameMap map, Node node, List<Node> avoid) {
        List<Node> blocks = new ArrayList<>(map.getListIndestructibles());
        blocks.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));
        if (avoid != null) blocks.addAll(avoid);
        for (Node n : blocks) {
            if (n.x == node.x && n.y == node.y) return true;
        }
        return node.x < 0 || node.y < 0 || node.x >= map.getMapSize() || node.y >= map.getMapSize();
    }

    private static boolean isAligned(Node a, Node b) {
        return a.x == b.x || a.y == b.y;
    }

    private static boolean isClearPath(GameMap map, Node from, Node to) {
        if (!isAligned(from, to)) return false;
        int dx = Integer.compare(to.x, from.x);
        int dy = Integer.compare(to.y, from.y);
        Node check = new Node(from.x + dx, from.y + dy);
        while (check.x != to.x || check.y != to.y) {
            if (isBlocked(map, check, null)) return false;
            check.x += dx;
            check.y += dy;
        }
        return true;
    }

    private static String getDirection(Node from, Node to) {
        if (to.x < from.x) return "l";
        if (to.x > from.x) return "r";
        if (to.y < from.y) return "u";
        if (to.y > from.y) return "d";
        return "";
    }
}