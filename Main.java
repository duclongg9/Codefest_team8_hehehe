import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "157092";
    private static final String PLAYER_NAME = "duong";
    private static final String SECRET_KEY = "sk-dkta4PFURHmV_RECouwuPA:w38nWATI0S0gtXWTi0ZvzlRsfg5ynYNQVfNiWnhK28dO2Dl3qgNVORAnZ_19B5RPpfIWUCn1xiIXczA8hNvvPA";
    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener onMapUpdate = new MapUpdateListener(hero);
        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;
    private Obstacle lastTargetedChest = null;
    private int pickupAttempts = 0; // Đếm số lần thử pickup
    private int repositionAttempts = 0; // Đếm số lần thử reposition
    private static final int MAX_PICKUP_ATTEMPTS = 3;
    private static final int MAX_REPOSITION_ATTEMPTS = 5;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }
    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();
            if (player == null || player.getHealth() == 0) return;

            List<Node> nodesToAvoid = getNodesToAvoid(gameMap);
            Weapon gun = hero.getInventory().getGun();

            if (gun == null) {
                searchForGun(gameMap, player, nodesToAvoid);
            } else {
                actWithGun(gameMap, player, nodesToAvoid);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void searchForGun(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Node pNode = new Node(player.getX(), player.getY());
        // Chỉ thử pickup khi ở vị trí có item và chưa thử quá nhiều lần
        if (pickupAttempts < MAX_PICKUP_ATTEMPTS) {
            hero.pickupItem();
            pickupAttempts++;
        }
        Weapon gun = getNearestGun(gameMap, pNode);
        if (gun == null) {
            moveToSafeZone(gameMap, player, avoid);
            return;
        }
        Node gNode = new Node(gun.getX(), gun.getY());
        String path = PathUtils.getShortestPath(gameMap, avoid, pNode, gNode, false);
        if (path != null && !path.isEmpty()) {
            hero.move(path.substring(0, 1));
            // Reset pickup attempts khi di chuyển
            pickupAttempts = 0;
        }
    }
    private void actWithGun(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Node pNode = new Node(player.getX(), player.getY());
        // Tìm cả NPC và rương gần nhất
        Player nearestEnemy = findNearestEnemy(gameMap, player);
        Optional<Obstacle> nearestChest = findNearestChest(gameMap, pNode);
        // Xác định mục tiêu ưu tiên (cái nào gần hơn)
        if (nearestEnemy != null && nearestChest.isPresent()) {
            double enemyDist = PathUtils.distance(pNode, new Node(nearestEnemy.getX(), nearestEnemy.getY()));
            double chestDist = PathUtils.distance(pNode, new Node(nearestChest.get().getX(), nearestChest.get().getY()));
            if (enemyDist <= chestDist) {
                handleEnemyTarget(gameMap, player, nearestEnemy, pNode, avoid);
            } else {
                handleChestTarget(gameMap, player, nearestChest.get(), pNode, avoid);
            }
        } else if (nearestEnemy != null) {
            handleEnemyTarget(gameMap, player, nearestEnemy, pNode, avoid);
        } else if (nearestChest.isPresent()) {
            handleChestTarget(gameMap, player, nearestChest.get(), pNode, avoid);
        } else {
            moveToSafeZone(gameMap, player, avoid);
        }
    }
    private void handleChestTarget(GameMap gameMap, Player player, Obstacle chest, Node pNode, List<Node> avoid) throws IOException {
        Node cNode = new Node(chest.getX(), chest.getY());
        double dist = PathUtils.distance(pNode, cNode);
        Weapon gun = hero.getInventory().getGun();
        // Reset counter khi đổi mục tiêu
        if (lastTargetedChest == null || !lastTargetedChest.equals(chest)) {
            lastTargetedChest = chest;
            pickupAttempts = 0;
            repositionAttempts = 0;
        }
        System.out.println("Distance to chest: " + dist);
        if (gun != null) {
            System.out.println("Gun range: " + gun.getRange());
            System.out.println("Can shoot straight: " + canShootStraight(player, chest));
        }
        // Nếu ở sát rương (dist <= 1.5), thử pickup
        if (dist <= 1.5 && pickupAttempts < MAX_PICKUP_ATTEMPTS) {
            hero.pickupItem();
            pickupAttempts++;
            return;
        }
        // Nếu có thể bắn thẳng và trong tầm
        if (gun != null && dist > 1.5 && dist <= gun.getRange() && canShootStraight(player, chest)) {
            String dir = getDirectionToTarget(player.getX(), player.getY(), chest.getX(), chest.getY());
            if (dir != null) {
                System.out.println("Shooting at chest, direction: " + dir);
                hero.shoot(dir);
                return;
            }
        }
        // Nếu có súng, trong tầm nhưng không thể bắn thẳng
        if (gun != null && dist <= gun.getRange() && !canShootStraight(player, chest)) {
            if (repositionAttempts < MAX_REPOSITION_ATTEMPTS) {
                repositionForShooting(gameMap, player, chest, avoid);
                repositionAttempts++;
                return;
            } else {
                // Quá nhiều lần reposition không thành công, bỏ qua rương này
                System.out.println("Too many reposition attempts, skipping this chest");
                lastTargetedChest = null; // Reset để tìm mục tiêu khác
                moveToSafeZone(gameMap, player, avoid);
                return;
            }
        }
        // Di chuyển đến rương nếu ngoài tầm bắn
        if (dist > gun.getRange()) {
            String path = PathUtils.getShortestPath(gameMap, avoid, pNode, cNode, false);
            if (path != null && !path.isEmpty()) {
                hero.move(path.substring(0, 1));
                // Reset counters khi di chuyển
                if (repositionAttempts > 0) repositionAttempts = 0;
            } else {
                // Không thể đi đến rương, bỏ qua
                lastTargetedChest = null;
                moveToSafeZone(gameMap, player, avoid);
            }
        }
    }
    private void repositionForShooting(GameMap gameMap, Player player, Obstacle chest, List<Node> avoid) throws IOException {
        Node pNode = new Node(player.getX(), player.getY());
        Weapon gun = hero.getInventory().getGun();
        if (gun == null) return;
        // Tìm vị trí đơn giản hơn - chỉ thử 4 hướng cơ bản
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        for (int i = 0; i < dx.length; i++) {
            int targetX = player.getX() + dx[i];
            int targetY = player.getY() + dy[i];
            // Kiểm tra vị trí hợp lệ
            if (targetX >= 0 && targetX < gameMap.getMapSize() &&
                    targetY >= 0 && targetY < gameMap.getMapSize()) {
                // Kiểm tra xem từ vị trí mới có thể bắn thẳng không
                if (targetX == chest.getX() || targetY == chest.getY()) {
                    Node targetNode = new Node(targetX, targetY);
                    String path = PathUtils.getShortestPath(gameMap, avoid, pNode, targetNode, false);
                    if (path != null && !path.isEmpty()) {
                        System.out.println("Repositioning to get better shooting angle");
                        hero.move(path.substring(0, 1));
                        return;
                    }
                }
            }
        }
        // Nếu không tìm được vị trí tốt, di chuyển ngẫu nhiên 1 bước
        String[] directions = {"u", "d", "l", "r"};
        for (String dir : directions) {
            try {
                hero.move(dir);
                System.out.println("Random reposition move: " + dir);
                return;
            } catch (Exception e) {
                // Thử hướng khác
            }
        }
    }
    // Các method khác giữ nguyên...
    private Player findNearestEnemy(GameMap gm, Player p) {
        double min = Double.MAX_VALUE;
        Player res = null;
        Node pn = new Node(p.getX(), p.getY());
        for (Player o : gm.getOtherPlayerInfo()) {
            if (o.getHealth() <= 0) continue;
            Node on = new Node(o.getX(), o.getY());
            double d = PathUtils.distance(pn, on);
            if (d < min) {
                min = d;
                res = o;
            }
        }
        return res;
    }
    private void handleEnemyTarget(GameMap gameMap, Player player, Player enemy, Node pNode, List<Node> avoid) throws IOException {
        Node eNode = new Node(enemy.getX(), enemy.getY());
        if (canShootStraight(player, enemy)) {
            Weapon gun = hero.getInventory().getGun();
            double dist = PathUtils.distance(pNode, eNode);
            if (gun != null && dist <= gun.getRange()) {
                String dir = getDirectionToTarget(player.getX(), player.getY(), enemy.getX(), enemy.getY());
                hero.shoot(dir);
                return;
            }
        }
        String path = PathUtils.getShortestPath(gameMap, avoid, pNode, eNode, false);
        if (path != null && !path.isEmpty()) {
            hero.move(path.substring(0, Math.min(path.length(), 2)));
        }
    }
    private boolean canShootStraight(Player p, Player e) {
        return p.getX() == e.getX() || p.getY() == e.getY();
    }
    private boolean canShootStraight(Player p, Obstacle o) {
        return p.getX() == o.getX() || p.getY() == o.getY();
    }
    private String getDirectionToTarget(int fx, int fy, int tx, int ty) {
        if (fx < tx) return "r";
        if (fx > tx) return "l";
        if (fy < ty) return "u";
        if (fy > ty) return "d";
        return null;
    }
    private void moveToSafeZone(GameMap gm, Player p, List<Node> avoid) throws IOException {
        Node pn = new Node(p.getX(), p.getY());
        if (!PathUtils.checkInsideSafeArea(pn, gm.getSafeZone(), gm.getMapSize())) {
            Node center = new Node(gm.getMapSize()/2, gm.getMapSize()/2);
            String path = PathUtils.getShortestPath(gm, avoid, pn, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(path.substring(0, Math.min(path.length(), 3)));
            }
        }
    }
    private Weapon getNearestGun(GameMap gm, Node pn) {
        if (gm.getAllGun() == null) return null;
        double min = Double.MAX_VALUE;
        Weapon res = null;
        for (Weapon w : gm.getAllGun()) {
            if ("Gun".equalsIgnoreCase(w.getType().toString())) {
                Node wn = new Node(w.getX(), w.getY());
                double d = PathUtils.distance(pn, wn);
                if (d < min) {
                    min = d;
                    res = w;
                }
            }
        }
        return res;
    }
    private Optional<Obstacle> findNearestChest(GameMap gm, Node pn) {
        if (gm.getListObstacles() == null) return Optional.empty();
        double min = Double.MAX_VALUE;
        Obstacle res = null;
        for (Obstacle o : gm.getListObstacles()) {
            if ("CHEST".equalsIgnoreCase(o.getId()) && "Chest".equalsIgnoreCase(o.getType().toString())) {
                Node on = new Node(o.getX(), o.getY());
                double d = PathUtils.distance(pn, on);
                if (d < min) {
                    min = d;
                    res = o;
                }
            }
        }
        return Optional.ofNullable(res);
    }
    private List<Node> getNodesToAvoid(GameMap gm) {
        List<Node> nodes = new ArrayList<>();
        if (gm.getListIndestructibles() != null) {
            nodes.addAll(gm.getListIndestructibles());
        }
        for (Player p : gm.getOtherPlayerInfo()) {
            if (p.getHealth() > 0) {
                nodes.add(new Node(p.getX(), p.getY()));
            }
        }
        if (gm.getObstaclesByTag("CAN_GO_THROUGH") != null) {
            nodes.removeAll(gm.getObstaclesByTag("CAN_GO_THROUGH"));
        }
        if (gm.getObstaclesByTag("CAN_SHOOT_THROUGH") != null) {
            nodes.removeAll(gm.getObstaclesByTag("CAN_SHOOT_THROUGH"));
        }
        return nodes;
    }
}
