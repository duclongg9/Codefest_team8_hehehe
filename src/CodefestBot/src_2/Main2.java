import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.equipments.HealingItem;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.obstacles.Obstacle;

import java.io.IOException;
import java.util.*;

public class Main2 {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "115290";
    private static final String PLAYER_NAME = "Huyen";
    private static final String SECRET_KEY = "sk-dkta4PFURHmV_RECouwuPA:w38nWATI0S0gtXWTi0ZvzlRsfg5ynYNQVfNiWnhK28dO2Dl3qgNVORAnZ_19B5RPpfIWUCn1xiIXczA8hNvvPA";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener(hero));
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }
    private void checkAndPickupBetterItem(GameMap map, Player player, List<Node> blocked) throws IOException {
        List<Weapon> groundWeapons = map.getListWeapons();
        List<Weapon> myWeapons = new ArrayList<>();
        Inventory inv = hero.getInventory();

        if (inv.getGun() != null) myWeapons.add(inv.getGun());
        if (inv.getMelee() != null) myWeapons.add(inv.getMelee());
        if (inv.getThrowable() != null) myWeapons.add(inv.getThrowable());
        if (inv.getSpecial() != null) myWeapons.add(inv.getSpecial());

        // Check Weapon trên map
        Weapon targetWeapon = null;
        double bestScoreDiff = 0;

        for (Weapon w : groundWeapons) {
            double newScore = w.getDamage() * 2 + w.getPickupPoints();
            double oldScore = getBestWeaponScore(myWeapons);
            double scoreDiff = newScore - oldScore;

            double dist = PathUtils.distance(player, w);
            if (scoreDiff > 0 && dist <= 2) {  // Ưu tiên item gần (<= 2 cells)
                targetWeapon = w;
                bestScoreDiff = scoreDiff;
            }
        }

        // Nếu có vũ khí tốt hơn gần → Bỏ đồ cũ
        if (targetWeapon != null) {
            for (Weapon w : myWeapons) {
                hero.revokeItem(w.getId());
            }
            moveToItem(targetWeapon, map, player, blocked);
            return;
        }

        // Check HealingItem trên map
        for (HealingItem item : map.getListHealingItems()) {
            int newHeal = item.getHealingHP();
            int oldBestHeal = getBestHeal(inv.getListHealingItem());
            double dist = PathUtils.distance(player, item);

            if (newHeal > oldBestHeal && dist <= 2) {
                for (HealingItem oldItem : inv.getListHealingItem()) {
                    hero.revokeItem(oldItem.getId());
                }
                moveToItem(item, map, player, blocked);
                return;
            }
        }
    }

    private double getBestWeaponScore(List<Weapon> weapons) {
        double best = 0;
        for (Weapon w : weapons) {
            double score = w.getDamage() * 2 + w.getPickupPoints();
            if (score > best) best = score;
        }
        return best;
    }

    private int getBestHeal(List<HealingItem> items) {
        int best = 0;
        for (HealingItem i : items) {
            if (i.getHealingHP() > best) best = i.getHealingHP();
        }
        return best;
    }

    private void moveToItem(Element target, GameMap map, Player player, List<Node> blocked) throws IOException {
        String path = PathUtils.getShortestPath(map, blocked, player, target, true);
        if (path != null && path.isEmpty()) {
            hero.pickupItem();
        } else if (path != null) {
            hero.move(path);
        }
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap map = hero.getGameMap();
            map.updateOnUpdateMap(args[0]);
            Player player = map.getCurrentPlayer();
            if (player == null || player.getHealth() <= 0) return;

            List<Node> blockedNodes = getBlockedNodes(map);

            Node myNode = new Node(player.getX(), player.getY());
            boolean inSafe = PathUtils.checkInsideSafeArea(myNode, map.getSafeZone(), map.getMapSize());
            if (!inSafe) {
                Node center = new Node(map.getMapSize() / 2, map.getMapSize() / 2);
                String path = PathUtils.getShortestPath(map, blockedNodes, myNode, center, true );
                if (path != null) hero.move(path);
                return;
            }

            Element current = map.getElementByIndex(player.getX(), player.getY());
            if (current.getType().equals(ElementType.CHEST)) {
                hero.pickupItem();
                return;
            }

            if (hero.getInventory().getGun() == null) {
                moveToNearestGunOrChest(map, player, blockedNodes);
                return;
            }
            checkAndPickupBetterItem(map, player, blockedNodes);


            attackNearestPlayer(map, player, blockedNodes);

            if (!isInsideSafeZone(player.getX(), player.getY(), map)) {
                Node center = new Node(map.getMapSize() / 2, map.getMapSize() / 2);
                String path = PathUtils.getShortestPath(map, blockedNodes, myNode, center, true);
                if (path != null) hero.move(path);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveToNearestGunOrChest(GameMap map, Player player, List<Node> blocked) throws IOException {
        Weapon targetGun = getNearestGun(map, player);
        if (targetGun != null) {
            String path = PathUtils.getShortestPath(map, blocked, player, targetGun, true);
            if (path != null && path.isEmpty()) {
                hero.pickupItem();
            } else if (path != null) {
                hero.move(path);
            }
            return;
        }

        Node chest = getNearestChest(map, player);
        if (chest != null) {
            String path = PathUtils.getShortestPath(map, blocked, player, chest, true);
            if (path != null) hero.move(path);
        }
    }

    private Weapon getNearestGun(GameMap map, Player player) {
        List<Weapon> guns = map.getAllGun();
        Weapon nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Weapon gun : guns) {
            double dist = PathUtils.distance(player, gun);
            if (dist < minDist) {
                minDist = dist;
                nearest = gun;
            }
        }
        return nearest;
    }

    private Node getNearestChest(GameMap map, Player player) {
        List<Obstacle> chests = map.getListChests();
        Node nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Obstacle chest : chests) {
            double dist = PathUtils.distance(player, new Node(chest.getX(), chest.getY()));
            if (dist < minDist) {
                minDist = dist;
                nearest = new Node(chest.getX(), chest.getY());
            }
        }
        return nearest;
    }

    private void attackNearestPlayer(GameMap map, Player player, List<Node> blockedNodes) throws IOException {
        Player target = getNearestPlayer(map, player);
        if (target == null) return;

        double distance = PathUtils.distance(player, target);
        Weapon bestWeapon = getBestWeapon(hero.getInventory());
        if (bestWeapon == null) return;

        String dir = getDirectionTo(player, target);

        if (distance <= bestWeapon.getRange()) {
            switch (bestWeapon.getType()) {
                case GUN -> hero.shoot(dir);
                case THROWABLE -> hero.throwItem(dir, (int) Math.min(distance, bestWeapon.getRange()));
                case MELEE -> hero.attack(dir);
                case SPECIAL -> hero.useSpecial(dir);
            }
        } else if (bestWeapon.getType().equals(ElementType.MELEE) && distance == 1) {
            hero.attack(dir);
        } else {

            if (target.getHealth() > 0) {
                bfsMove(hero, map, target.getX(), target.getY(), blockedNodes);
            }
        }
    }


    private Player getNearestPlayer(GameMap map, Player player) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Player p : map.getOtherPlayerInfo()) {
            if (p.getHealth() <= 0) continue;  // Bỏ qua Player chết

            double dist = PathUtils.distance(player, p);
            if (dist > 0 && dist < minDist) {  // Bắt buộc target phải cách mình ít nhất 1 ô
                minDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }


    private Weapon getBestWeapon(Inventory inv) {
        List<Weapon> weapons = new ArrayList<>();
        if (inv.getGun() != null) weapons.add(inv.getGun());
        if (inv.getThrowable() != null) weapons.add(inv.getThrowable());
        if (inv.getMelee() != null) weapons.add(inv.getMelee());
        if (inv.getSpecial() != null) weapons.add(inv.getSpecial());

        Weapon best = null;
        double bestScore = -1;
        for (Weapon w : weapons) {
            double score = w.getDamage() * 2 + w.getPickupPoints();
            if (score > bestScore) {
                bestScore = score;
                best = w;
            }
        }
        return best;
    }

    private List<Node> getBlockedNodes(GameMap map) {
        List<Node> blocked = new ArrayList<>();

        // Né Indestructibles
        for (Obstacle obs : map.getListIndestructibles()) {
            blocked.add(new Node(obs.getX(), obs.getY()));
        }

        // Né Traps
        for (Obstacle trap : map.getListTraps()) {
            blocked.add(new Node(trap.getX(), trap.getY()));
        }

        // Né Enemy NPC
        for (Enemy enemy : map.getListEnemies()) {
            blocked.add(new Node(enemy.getX(), enemy.getY()));
        }


        return blocked;
    }


    private String getDirectionTo(Player from, Player to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "d" : "u";
        }
    }

    private boolean isInsideSafeZone(int x, int y, GameMap map) {
        Node point = new Node(x, y);
        return PathUtils.checkInsideSafeArea(point, map.getSafeZone(), map.getMapSize());
    }


    private void bfsMove(Hero hero, GameMap map, int tx, int ty, List<Node> blocked) throws IOException {
        int sx = map.getCurrentPlayer().getX(), sy = map.getCurrentPlayer().getY();
        Queue<int[]> q = new LinkedList<>();
        Map<String, String> move = new HashMap<>();
        Set<String> visited = new HashSet<>();
        q.add(new int[]{sx, sy});
        visited.add(sx + "," + sy);
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        String[] dirStr = {"u", "d", "l", "r"};

        while (!q.isEmpty()) {
            int[] pos = q.poll();
            if (pos[0] == tx && pos[1] == ty) break;
            for (int i = 0; i < 4; i++) {
                int nx = pos[0] + dirs[i][0], ny = pos[1] + dirs[i][1];
                String key = nx + "," + ny;
                if (nx >= 0 && ny >= 0 && nx < map.getMapSize() && ny < map.getMapSize()
                        && !visited.contains(key)
                        && isWalkable(nx, ny, blocked)
                        && isInsideSafeZone(nx, ny, map)) {      // fChỉ đi node trong SafeZone
                    q.add(new int[]{nx, ny});
                    visited.add(key);
                    move.put(key, dirStr[i]);
                }
            }
        }
        String dir = move.get(tx + "," + ty);
        if (dir != null) hero.move(dir);
    }


    private boolean isWalkable(int x, int y, List<Node> blocked) {
        for (Node node : blocked) {
            if (node.getX() == x && node.getY() == y) return false;
        }
        return true;
    }
}
