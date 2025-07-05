//// StepHandler_b.java
//
//import jsclub.codefest.sdk.Hero;
//import jsclub.codefest.sdk.algorithm.PathUtils;
//import jsclub.codefest.sdk.base.Node;
//import jsclub.codefest.sdk.model.*;
//import jsclub.codefest.sdk.model.players.Player;
//import jsclub.codefest.sdk.model.npcs.Enemy;
//import jsclub.codefest.sdk.model.npcs.Ally;
//import jsclub.codefest.sdk.model.support_items.SupportItem;
//import jsclub.codefest.sdk.model.weapon.Weapon;
//import jsclub.codefest.sdk.model.obstacles.Obstacle;
//
//import java.io.IOException;
//import java.util.*;
//
//import static BaseBotLogic.*;
//
//public class StepHandler_B {
//
//    public static void handleStep(GameMap map, Hero hero) throws IOException {
//        if (map == null || hero == null) return;
//        Player player = map.getCurrentPlayer();
//        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;
//
//        Node me = new Node(player.getX(), player.getY());
//        Inventory inv = hero.getInventory();
//        boolean acted;
//
//        // Hành vi cơ bản - chỉ thực hiện nếu trả về true (có hành động)
//        if ((acted = pickupGunIfNeeded(hero, map, me)) ||
//                (acted = avoidEnemies(hero, map, me)) ||
//                (acted = dodgeBulletIfTargeted(hero, map, me)) ||
//                (acted = shootNearby(hero, map, me, inv)) ||
//                (acted = breakChestIfNearby(hero, map, me))) return;
//
//        // Nếu HP thấp, tìm item hồi máu gần nhất
//        if (player.getHealth() < 40) {
//            SupportItem heal = getClosest(map.getListSupportItems(), me);
//            if (heal != null && goTo(hero, map, me, heal, false)) return;
//        }
//
//        // Nếu gần đồng minh NPC thì tiến đến
//        Ally ally = getClosest(map.getListAllies(), me);
//        if (ally != null && PathUtils.distance(me, ally) <= 4 && goTo(hero, map, me, ally, false)) return;
//
//        // Nếu có vũ khí mạnh thì chủ động săn kẻ yếu
//        Weapon gun = inv.getGun();
//        if (gun != null) {
//            Player target = getWeakPlayer(map.getOtherPlayerInfo(), me, gun.getRange());
//            if (target != null) {
//                hero.shoot(getDirection(me, target));
//                return;
//            }
//        }
//
//        // Di chuyển về gần trung tâm bo nếu chưa ở trong
//        if (!PathUtils.checkInsideSafeArea(me, map.getSafeZone(), map.getMapSize())) {
//            Node center = new Node(map.getMapSize() / 2, map.getMapSize() / 2);
//            String path = PathUtils.getShortestPath(map, getAvoidNodes(map, true), me, center, false);
//            if (path != null && !path.isEmpty()) {
//                hero.move(path.substring(0, 1));
//                return;
//            }
//        }
//
//        // Nếu gần bo thì kiểm soát trung tâm
//        Node center = new Node(map.getMapSize() / 2, map.getMapSize() / 2);
//        if (PathUtils.distance(me, center) > 4) {
//            String path = PathUtils.getShortestPath(map, getAvoidNodes(map, true), me, center, false);
//            if (path != null && !path.isEmpty()) {
//                hero.move(path.substring(0, 1));
//                return;
//            }
//        }
//
//        // Di chuyển ngẫu nhiên cuối cùng nếu không có gì khác
//        moveRandom(hero, map, me, getAvoidNodes(map, true));
//    }
//
//    private static <T extends Node> T getClosest(List<T> list, Node from) {
//        return list.stream()
//                .filter(Objects::nonNull)
//                .min(Comparator.comparingInt(n -> PathUtils.distance(from, n)))
//                .orElse(null);
//    }
//
//    private static Player getWeakPlayer(List<Player> players, Node from, int[] range) {
//        return players.stream()
//                .filter(p -> p.getHealth() != null && p.getHealth() < 40)
//                .filter(p -> PathUtils.distance(from, p) <= range[0])
//                .min(Comparator.comparingInt(p -> PathUtils.distance(from, p)))
//                .orElse(null);
//    }
//
//    private static boolean goTo(Hero hero, GameMap map, Node from, Node to, boolean skipDark) throws IOException {
//        if (from.equals(to)) {
//            hero.pickupItem();
//            return true;
//        }
//        String path = PathUtils.getShortestPath(map, getAvoidNodes(map, false), from, to, skipDark);
//        if (path != null && !path.isEmpty()) {
//            hero.move(path.substring(0, 1));
//            return true;
//        }
//        return false;
//    }
//
//    private static List<Node> getAvoidNodes(GameMap map, boolean avoidEnemies) {
//        List<Node> avoid = new ArrayList<>(map.getListIndestructibles());
//        avoid.removeAll(map.getObstaclesByTag("CAN_GO_THROUGH"));
//        if (avoidEnemies) {
//            for (Enemy e : map.getListEnemies()) {
//                for (int dx = -1; dx <= 1; dx++)
//                    for (int dy = -1; dy <= 1; dy++)
//                        avoid.add(new Node(e.getX() + dx, e.getY() + dy));
//            }
//        }
//        for (Player p : map.getOtherPlayerInfo()) {
//            avoid.add(new Node(p.getX(), p.getY()));
//        }
//        return avoid;
//    }
//
//    private static String getDirection(Node from, Node to) {
//        if (to.x < from.x) return "l";
//        if (to.x > from.x) return "r";
//        if (to.y < from.y) return "u";
//        if (to.y > from.y) return "d";
//        return "";
//    }
//
//    private static void moveRandom(Hero hero, GameMap map, Node current, List<Node> avoid) throws IOException {
//        String[] dirs = new String[]{"l", "r", "u", "d"};
//        Collections.shuffle(Arrays.asList(dirs));
//        for (String d : dirs) {
//            Node next = getNext(current, d.charAt(0));
//            if (!isBlocked(next, map, avoid)) {
//                hero.move(d);
//                return;
//            }
//        }
//    }
//
//    private static Node getNext(Node cur, char d) {
//        int x = cur.x, y = cur.y;
//        switch (d) {
//            case 'l' -> x--;
//            case 'r' -> x++;
//            case 'u' -> y--;
//            case 'd' -> y++;
//        }
//        return new Node(x, y);
//    }
//
//    private static boolean isBlocked(Node n, GameMap map, List<Node> avoid) {
//        if (n.x < 0 || n.y < 0 || n.x >= map.getMapSize() || n.y >= map.getMapSize()) return true;
//        for (Node b : avoid) {
//            if (b.x == n.x && b.y == n.y) return true;
//        }
//        return false;
//    }
//}