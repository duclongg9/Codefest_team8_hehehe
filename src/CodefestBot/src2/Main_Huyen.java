
import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.armors.Armor;

import java.io.IOException;
import java.util.*;

// chưa có hồi máu

public class Main_Huyen {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "173540";
    private static final String PLAYER_NAME = "bot4";
    private static final String SECRET_KEY = "sk-CIWbSOsQSG2yaFmYIpJRTw:E4WFjJIV4drMuK_L3Dxm0B0pv9FYfY4LdTK9UwuV7bQeToK3TqLzyHaLl4QOeizl0lhO74GJ25tBVs2AMWiSRw";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener(hero));
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;
    private Node targetChestLocation = null;
    private boolean waitingForItemsToAppear = false;
    private int waitCounter = 0;
    private static final int MAX_WAIT_TURNS_FOR_PICKUP = 5;
    private final String[] directions = {"r", "d", "u", "l"};

    private boolean hasInitialWeapon = false;
    private boolean hasLootedFirstChest = false;
    private boolean combatMode = false;

    private static final float HEALTH_THRESHOLD = 0.7f;

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
            if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

            Inventory inv = hero.getInventory();
            List<Node> avoid = getNodesToAvoid(gameMap);
            Node playerNode = new Node(player.getX(), player.getY());

            System.out.println("=== DEBUG INFO ===");
            System.out.println("Player position: (" + player.getX() + ", " + player.getY() + ")");
            System.out.println("Player health: " + player.getHealth());
            System.out.println("Has initial weapon: " + hasInitialWeapon);
            System.out.println("Has looted first chest: " + hasLootedFirstChest);
            printInventoryStatus(inv);

            if (inv.getGun() != null && "RUBBER_GUN".equals(inv.getGun().getId())) {
                System.out.println("Removing RUBBER_GUN");
                hero.revokeItem("RUBBER_GUN");
                return;
            }

            if (needHealing(player, inv)) {
                System.out.println("Need healing, using item");
                if (useHealingItem(hero, inv)) {
                    return;
                }
            }

            if (!hasInitialWeapon) {
                if (hasGoodWeapon(inv)) {
                    System.out.println("Got initial weapon, moving to phase 2");
                    hasInitialWeapon = true;
                } else {
                    System.out.println("Looking for initial weapon");
                    if (findAndGetWeapon(gameMap, player, avoid)) {
                        return;
                    }
                    // If no weapon found, move randomly
                    System.out.println("No weapon found, moving randomly");
                    hero.move("r");
                    return;
                }
            }

            if (hasInitialWeapon && !hasLootedFirstChest) {
                System.out.println("Phase 2: Looking for chest to loot");
                if (targetChestLocation != null && waitingForItemsToAppear) {
                    System.out.println("Waiting for items to appear from chest");
                    handleChestLooting(gameMap, playerNode, avoid);
                    return;
                } else {
                    Node chest = getNearestChest(gameMap, player);
                    if (chest != null) {
                        System.out.println("Found chest at: (" + chest.getX() + ", " + chest.getY() + ")");
                        if (canBreakChest(inv)) {
                            handleChestBreaking(hero, gameMap, player, avoid, chest);
                            return;
                        } else {
                            System.out.println("Cannot break chest, need better weapon");
                            hasLootedFirstChest = true; // Skip chest phase if can't break
                        }
                    } else {
                        System.out.println("No chest found, moving to phase 3");
                        hasLootedFirstChest = true;
                    }
                }
            }

            if (hasInitialWeapon && hasLootedFirstChest) {
                System.out.println("Phase 3: Combat mode");
                if (shouldContinueCombat(player, inv)) {
                    Player enemy = getNearestPlayer(gameMap, player);
                    if (enemy != null) {
                        System.out.println("Attacking enemy at: (" + enemy.getX() + ", " + enemy.getY() + ")");
                        attackEnemyWithGunThenMelee(hero, gameMap, player, enemy, avoid);
                        return;
                    }
                }

                if (needBetterGun(inv)) {
                    System.out.println("Looking for better gun");
                    if (findAndGetWeapon(gameMap, player, avoid)) {
                        return;
                    }
                }

                Node chest = getNearestChest(gameMap, player);
                if (chest != null && canBreakChest(inv)) {
                    System.out.println("Found additional chest for resources");
                    if (targetChestLocation != null && waitingForItemsToAppear) {
                        handleChestLooting(gameMap, playerNode, avoid);
                        return;
                    } else {
                        handleChestBreaking(hero, gameMap, player, avoid, chest);
                        return;
                    }
                }

                Player enemy = getNearestPlayer(gameMap, player);
                if (enemy != null) {
                    System.out.println("Continuing combat");
                    attackEnemyWithGunThenMelee(hero, gameMap, player, enemy, avoid);
                    return;
                }
            }

            System.out.println("Fallback: Moving randomly");
            if (avoid.size() < gameMap.getMapSize() * gameMap.getMapSize()) {
                hero.move("r");
            }

        } catch (Exception e) {
            System.err.println("Error in main loop: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printInventoryStatus(Inventory inv) throws IOException {
        System.out.println("--- Inventory Status ---");
        System.out.println("Gun: " + (inv.getGun() != null ? inv.getGun().getId() + " (uses: " + inv.getGun().getUseCount() + ")" : "None"));
        System.out.println("Melee: " + (inv.getMelee() != null ? inv.getMelee().getId() : "None"));
        System.out.println("Special: " + (inv.getSpecial() != null ? inv.getSpecial().getId() + " (uses: " + inv.getSpecial().getUseCount() + ")" : "None"));
        System.out.println("Throwable: " + (inv.getThrowable() != null ? inv.getThrowable().getId() + " (uses: " + inv.getThrowable().getUseCount() + ")" : "None"));
        System.out.println("Support items: " + inv.getListSupportItem().size());
        System.out.println("Armor: " + (inv.getArmor() != null ? inv.getArmor().getId() : "None"));
        System.out.println("Helmet: " + (inv.getHelmet() != null ? inv.getHelmet().getId() : "None"));
    }

    private boolean hasGoodWeapon(Inventory inv) throws IOException {
        if (inv.getGun() != null && !"RUBBER_GUN".equals(inv.getGun().getId()) && inv.getGun().getUseCount() > 0) {
            return true;
        }

        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
            return true;
        }

        if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) {
            return true;
        }

        return false;
    }

    private boolean hasObstacleBetween(GameMap gameMap, Node from, Node to) {
        try {
            int dx = to.getX() - from.getX();
            int dy = to.getY() - from.getY();

            // Chỉ kiểm tra đường thẳng (horizontal, vertical)
            if (dx != 0 && dy != 0) {
                return true; // Không thể bắn theo đường chéo
            }

            int steps = Math.max(Math.abs(dx), Math.abs(dy));
            int stepX = dx == 0 ? 0 : (dx > 0 ? 1 : -1);
            int stepY = dy == 0 ? 0 : (dy > 0 ? 1 : -1);

            for (int i = 1; i < steps; i++) {
                int checkX = from.getX() + i * stepX;
                int checkY = from.getY() + i * stepY;

                Element e = gameMap.getElementByIndex(checkX, checkY);
                if (e != null && e.getType() != null) {
                    String type = e.getType().toString();
                    // Kiểm tra obstacle
                    if (type.contains("OBSTACLE")) {
                        if (e instanceof Obstacle) {
                            Obstacle obs = (Obstacle) e;
                            // Nếu không có tag CAN_SHOOT_THROUGH thì bị chặn
                            if (!obs.getTags().contains("CAN_SHOOT_THROUGH")) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                    // Player và Enemy luôn chặn
                    if (type.contains("PLAYER") || type.contains("ENEMY")) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean canBreakChest(Inventory inv) throws IOException {
        if (inv.getGun() != null && inv.getGun().getUseCount() > 0) {
            int[] range = inv.getGun().getRange();
            if (range[1] >= 1) { // Gun có thể bắn từ khoảng cách 1
                return true;
            }
        }

        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
            return true;
        }

        if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) {
            return true;
        }

        return false;
    }

    private boolean findAndGetWeapon(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        // Ưu tiên gun có range tốt
        Weapon gun = getBestWeapon(gameMap.getAllGun(), player);
        if (gun != null && !"RUBBER_GUN".equals(gun.getId())) {
            System.out.println("Found gun: " + gun.getId() + " at (" + gun.getX() + ", " + gun.getY() + ")");
            moveToWeaponOrPickup(hero, gun, gameMap, player, avoid);
            return true;
        }

        // Tìm melee weapon tốt
        Weapon melee = getBestWeapon(gameMap.getAllMelee(), player);
        if (melee != null && !"HAND".equals(melee.getId())) {
            System.out.println("Found melee: " + melee.getId() + " at (" + melee.getX() + ", " + melee.getY() + ")");
            moveToWeaponOrPickup(hero, melee, gameMap, player, avoid);
            return true;
        }

        // Tìm special weapon
        Weapon special = getBestWeapon(gameMap.getAllSpecial(), player);
        if (special != null) {
            System.out.println("Found special: " + special.getId() + " at (" + special.getX() + ", " + special.getY() + ")");
            moveToWeaponOrPickup(hero, special, gameMap, player, avoid);
            return true;
        }
        return false;
    }
    private Weapon getBestWeapon(List<Weapon> weapons, Player player) {
        Weapon best = null;
        int bestScore = -1;
        int minDistance = Integer.MAX_VALUE;

        Node playerNode = new Node(player.getX(), player.getY());

        for (Weapon weapon : weapons) {
            if ("RUBBER_GUN".equals(weapon.getId())) continue;

            int distance = PathUtils.distance(playerNode, new Node(weapon.getX(), weapon.getY()));
            int score = getWeaponScore(weapon);

            // Ưu tiên weapon có score cao hơn, nếu bằng nhau thì ưu tiên gần hơn
            if (score > bestScore || (score == bestScore && distance < minDistance)) {
                best = weapon;
                bestScore = score;
                minDistance = distance;
            }
        }

        return best;
    }

    private int getWeaponScore(Weapon weapon) {
        String id = weapon.getId();

        // Gun scoring
        if (weapon.getType().toString().contains("GUN")) {
            if ("SHOTGUN".equals(id)) return 100;
            if ("CROSSBOW".equals(id)) return 90;
            if ("PISTOL".equals(id)) return 80;
            return 50;
        }

        // Melee scoring
        if (weapon.getType().toString().contains("MELEE")) {
            if ("AXE".equals(id)) return 100;
            if ("SCEPTER".equals(id)) return 90;
            if ("BONE".equals(id)) return 80;
            if ("SAHUR_BAT".equals(id)) return 70;
            return 50;
        }

        // Special scoring
        if (weapon.getType().toString().contains("SPECIAL")) {
            if ("MACE".equals(id)) return 100;
            return 80;
        }

        return 50;
    }

    private boolean needHealing(Player player, Inventory inv) throws IOException {
        return player.getHealth() <= HEALTH_THRESHOLD && !inv.getListSupportItem().isEmpty();
    }

    private boolean useHealingItem(Hero hero, Inventory inv) throws IOException {
        List<SupportItem> items = inv.getListSupportItem();
        if (!items.isEmpty()) {
            SupportItem bestItem = items.get(0);
            for (SupportItem item : items) {
                if (item.getHealingHP() > bestItem.getHealingHP()) {
                    bestItem = item;
                }
            }
            System.out.println("Using healing item: " + bestItem.getId());
            hero.useItem(bestItem.getId());
            return true;
        }
        return false;
    }

    private boolean shouldContinueCombat(Player player, Inventory inv) throws IOException {
        return player.getHealth() > HEALTH_THRESHOLD &&
                hasUsableWeapon(inv) &&
                !inv.getListSupportItem().isEmpty();
    }

    private boolean needBetterGun(Inventory inv) throws IOException {
        return inv.getGun() == null ||
                "RUBBER_GUN".equals(inv.getGun().getId()) ||
                inv.getGun().getUseCount() <= 0;
    }

    private void handleChestLooting(GameMap gameMap, Node playerNode, List<Node> avoid) throws IOException {
        System.out.println("Handling chest looting phase");
        if (tryPickupAtCurrentPosition(gameMap, playerNode)) {
            System.out.println("Picked up item at current position");
            waitCounter = 0;
            return;
        }

        for (String dir : directions) {
            Node neighbor = moveInDirection(playerNode, dir);
            if (neighbor != null && hasPickupableItem(gameMap, neighbor)) {
                String path = PathUtils.getShortestPath(gameMap, avoid, playerNode, neighbor, false);
                if (path != null && !path.isEmpty()) {
                    System.out.println("Moving to pickup item in direction: " + dir);
                    hero.move(path);
                    return;
                }
            }
        }

        waitCounter++;
        System.out.println("Waiting for items, counter: " + waitCounter);
        if (waitCounter >= MAX_WAIT_TURNS_FOR_PICKUP) {
            System.out.println("Max wait reached, resetting chest target");
            targetChestLocation = null;
            waitingForItemsToAppear = false;
            waitCounter = 0;
            if (!hasLootedFirstChest) {
                hasLootedFirstChest = true;
            }
        }
    }

    private void handleChestBreaking(Hero hero, GameMap gameMap, Player player, List<Node> avoid, Node chest) throws IOException {
        Node me = new Node(player.getX(), player.getY());
        int distance = PathUtils.distance(me, chest);
        System.out.println("Handling chest breaking, distance: " + distance);
        if (distance > 1) {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, chest, false);
            if (path != null && !path.isEmpty()) {
                System.out.println("Moving towards chest: " + path);
                hero.move(path);
                return;
            }
        }

        String dir = getDirectionTo(player, chest);
        if (dir != null) {
            Inventory inv = hero.getInventory();
            boolean attackSuccessful = false;
            System.out.println("Attempting to break chest in direction: " + dir);

            if (inv.getGun() != null && inv.getGun().getUseCount() > 0) {
                System.out.println("Using gun to break chest: " + inv.getGun().getId());

                int[] gunRange = inv.getGun().getRange();
                if (distance >= gunRange[0] && distance <= gunRange[1]) {
                    if (!hasObstacleBetweenForShooting(gameMap, me, chest)) {
                        System.out.println("Shooting chest with gun");
                        hero.shoot(dir);
                        attackSuccessful = true;
                    } else {
                        System.out.println("Obstacle blocks gun shot to chest");
                    }
                } else {
                    System.out.println("Gun range: [" + gunRange[0] + ", " + gunRange[1] + "], distance: " + distance);
                    if (distance > gunRange[1]) {
                        String path = PathUtils.getShortestPath(gameMap, avoid, me, chest, false);
                        if (path != null && !path.isEmpty()) {
                            System.out.println("Moving closer for gun range");
                            hero.move(String.valueOf(path.charAt(0)));
                            return;
                        }
                    }
                }
            }

            if (!attackSuccessful && inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                if (distance == 1) {
                    System.out.println("Using melee to break chest: " + inv.getMelee().getId());
                    hero.attack(dir);
                    attackSuccessful = true;
                } else {
                    System.out.println("Too far for melee, moving closer");
                    String path = PathUtils.getShortestPath(gameMap, avoid, me, chest, false);
                    if (path != null && !path.isEmpty()) {
                        hero.move(String.valueOf(path.charAt(0)));
                        return;
                    }
                }
            }

            if (!attackSuccessful && inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) {
                int[] specialRange = inv.getSpecial().getRange();
                if (distance >= specialRange[0] && distance <= specialRange[1]) {
                    System.out.println("Using special to break chest: " + inv.getSpecial().getId());
                    hero.useSpecial(dir);
                    attackSuccessful = true;
                }
            }

            if (attackSuccessful && targetChestLocation == null) {
                System.out.println("Set chest target location");
                targetChestLocation = new Node(chest.getX(), chest.getY());
                waitingForItemsToAppear = true;
                waitCounter = 0;
            } else if (!attackSuccessful) {
                System.out.println("Cannot break chest with current weapons!");
                // Skip this chest and continue
                if (!hasLootedFirstChest) {
                    hasLootedFirstChest = true;
                }
            }
        } else {
            System.out.println("Cannot determine direction to chest");
        }
    }

    private void attackEnemyWithGunThenMelee(Hero hero, GameMap gameMap, Player player, Player enemy, List<Node> avoid) throws IOException {
        Node playerNode = new Node(player.getX(), player.getY());
        Node enemyNode = new Node(enemy.getX(), enemy.getY());
        int distance = PathUtils.distance(playerNode, enemyNode);

        System.out.println("Attacking enemy player at distance: " + distance);
        Inventory inv = hero.getInventory();

        // Ưu tiên dùng gun trước
        if (inv.getGun() != null && inv.getGun().getUseCount() > 0) {
            int[] gunRange = inv.getGun().getRange();
            if (distance >= gunRange[0] && distance <= gunRange[1]) {
                // Kiểm tra obstacle trước khi bắn
                if (!hasObstacleBetweenForShooting(gameMap, playerNode, enemyNode)) {
                    String dir = getDirectionTo(player, enemyNode);
                    if (dir != null) {
                        System.out.println("Shooting gun at player in direction: " + dir);
                        hero.shoot(dir);
                        return;
                    }
                } else {
                    System.out.println("Obstacle blocks shot, trying to move to better position");
                }
            }
            // Nếu quá xa, di chuyển lại gần
            if (distance > gunRange[1]) {
                String path = PathUtils.getShortestPath(gameMap, avoid, playerNode, enemyNode, false);
                if (path != null && !path.isEmpty()) {
                    System.out.println("Moving closer for gun range");
                    hero.move(String.valueOf(path.charAt(0)));
                    return;
                }
            }
        }

        // Dùng melee nếu ở gần
        if (distance == 1 && inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
            String dir = getDirectionTo(player, enemyNode);
            if (dir != null) {
                System.out.println("Attacking player with melee in direction: " + dir);
                hero.attack(dir);
                return;
            }
        }

        // Dùng special weapon
        if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) {
            int[] specialRange = inv.getSpecial().getRange();
            if (distance >= specialRange[0] && distance <= specialRange[1]) {
                String dir = getDirectionTo(player, enemyNode);
                if (dir != null) {
                    System.out.println("Using special weapon on player");
                    hero.useSpecial(dir);
                    return;
                }
            }
        }

        // Dùng throwable
        if (inv.getThrowable() != null && inv.getThrowable().getUseCount() > 0) {
            int[] throwRange = inv.getThrowable().getRange();
            if (distance >= throwRange[0] && distance <= throwRange[1]) {
                String dir = getDirectionTo(player, enemyNode);
                if (dir != null) {
                    System.out.println("Throwing item at player");
                    hero.throwItem(dir);
                    return;
                }
            }
        }

        // Di chuyển lại gần enemy
        String path = PathUtils.getShortestPath(gameMap, avoid, playerNode, enemyNode, false);
        if (path != null && !path.isEmpty()) {
            System.out.println("Moving closer to enemy player");
            hero.move(String.valueOf(path.charAt(0)));
        }
    }


    private boolean hasPickupableItem(GameMap gameMap, Node node) {
        try {
            Element e = gameMap.getElementByIndex(node.getX(), node.getY());
            if (e == null || e.getId() == null || e.getType() == null) return false;
            return shouldPickupItem(e);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean tryPickupAtCurrentPosition(GameMap gameMap, Node playerNode) {
        try {
            if (hasPickupableItem(gameMap, playerNode)) {
                System.out.println("Attempting to pickup item at current position");
                hero.pickupItem();
                return true;
            }
        } catch (Exception e) {
            System.out.println("Cannot pickup item at current position: " + e.getMessage());
        }
        return false;
    }

    private boolean hasUsableWeapon(Inventory inv) throws IOException {
        if (inv.getGun() != null && inv.getGun().getUseCount() > 0) return true;
        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) return true;
        if (inv.getThrowable() != null && inv.getThrowable().getUseCount() > 0) return true;
        if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) return true;
        return false;
    }

    private boolean shouldPickupItem(Element e) throws IOException {
        Inventory inv = hero.getInventory();
        String id = e.getId();
        String type = e.getType().toString();

        // Special weapons - ưu tiên cao
        List<String> highTierSpecials = List.of("MACE", "MAGIC_POTION", "ELIXIR_OF_LIFE", "GOD_LEAF", "SPIRIT_TEAR",
                "PHOENIX_FEATHERS", "UNICORN_BLOOD", "MERMAID_TAIL", "ELIXIR");
        if (type.contains("SPECIAL")) {
            if (inv.getSpecial() == null) return true;
            String currentId = inv.getSpecial().getId();
            // MACE luôn ưu tiên nhất
            if ("MACE".equals(id)) return true;
            if ("MACE".equals(currentId)) return false;
            // Các item tier cao
            if (highTierSpecials.contains(id) && !highTierSpecials.contains(currentId)) return true;
            return false;
        }

        // Melee weapons - ưu tiên theo thứ tự
        List<String> meleeRanking = List.of("AXE", "SCEPTER", "BONE", "SAHUR_BAT");
        if (type.contains("MELEE")) {
            if (inv.getMelee() == null || "HAND".equals(inv.getMelee().getId())) return true;
            String currentId = inv.getMelee().getId();
            int currentRank = meleeRanking.indexOf(currentId);
            int newRank = meleeRanking.indexOf(id);
            return newRank != -1 && (currentRank == -1 || newRank < currentRank);
        }

        // Gun weapons - ưu tiên theo thứ tự
        List<String> gunRanking = List.of("SHOTGUN", "CROSSBOW", "PISTOL");
        if (type.contains("GUN")) {
            if ("RUBBER_GUN".equals(id)) return false; // Không bao giờ lấy RUBBER_GUN
            if (inv.getGun() == null) return true;
            String currentId = inv.getGun().getId();
            if ("RUBBER_GUN".equals(currentId)) return true; // Thay thế RUBBER_GUN
            int currentRank = gunRanking.indexOf(currentId);
            int newRank = gunRanking.indexOf(id);
            return newRank != -1 && (currentRank == -1 || newRank < currentRank);
        }

        // Support items - giới hạn 3 items
        if (type.contains("HEALING") || type.contains("SUPPORT")) {
            return inv.getListSupportItem().size() < 3;
        }

        // Armor - lấy nếu chưa có
        if (type.contains("ARMOR")) {
            if (type.contains("HELMET")) {
                return inv.getHelmet() == null;
            } else {
                return inv.getArmor() == null;
            }
        }

        // Throwable items - ưu tiên theo thứ tự
        List<String> throwableRanking = List.of("BANANA", "SMOKE", "BELL");
        if (type.contains("THROWABLE")) {
            if (inv.getThrowable() == null) return true;
            String currentId = inv.getThrowable().getId();
            int currentRank = throwableRanking.indexOf(currentId);
            int newRank = throwableRanking.indexOf(id);
            if (newRank != -1 && (currentRank == -1 || newRank < currentRank)) {
                try {
                    hero.revokeItem(currentId);
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        }
        return false;
    }


    private Node moveInDirection(Node node, String dir) {
        int x = node.getX(), y = node.getY();
        return switch (dir) {
            case "r" -> new Node(x + 1, y);
            case "l" -> new Node(x - 1, y);
            case "u" -> new Node(x, y + 1);
            case "d" -> new Node(x, y - 1);
            default -> null;
        };
    }

    private List<Node> getNodesToAvoid(GameMap gameMap) {
        List<Node> list = new ArrayList<>(gameMap.getListIndestructibles());
        for (Obstacle o : gameMap.getObstaclesByTag("CAN_GO_THROUGH")) {
            list.remove(new Node(o.getX(), o.getY()));
        }
        list.addAll(gameMap.getObstaclesByTag("TRAP"));
        list.addAll(gameMap.getListEnemies());
        list.addAll(gameMap.getOtherPlayerInfo());

        return list;
    }

    private Node getNearestChest(GameMap gameMap, Player player) {
        Node res = null;
        int min = Integer.MAX_VALUE;
        Node me = new Node(player.getX(), player.getY());
        for (Obstacle o : gameMap.getObstaclesByTag("DESTRUCTIBLE")) {
            if (o.getId() != null && o.getId().contains("CHEST")) {
                Node n = new Node(o.getX(), o.getY());
                int d = PathUtils.distance(me, n);
                if (d < min) {
                    min = d;
                    res = n;
                }
            }
        }
        return res;
    }

    private Player getNearestPlayer(GameMap gameMap, Player player) {
        Player res = null;
        int min = Integer.MAX_VALUE;
        for (Player p : gameMap.getOtherPlayerInfo()) {
            if (p.getHealth() != null && p.getHealth() > 0) {
                int d = PathUtils.distance(player, p);
                if (d < min) {
                    min = d;
                    res = p;
                }
            }
        }
        return res;
    }

    private boolean hasObstacleBetweenForShooting(GameMap gameMap, Node from, Node to) {
        try {
            int dx = to.getX() - from.getX();
            int dy = to.getY() - from.getY();

            if (dx != 0 && dy != 0) {
                return true;
            }

            int steps = Math.max(Math.abs(dx), Math.abs(dy));
            int stepX = dx == 0 ? 0 : dx / Math.abs(dx);
            int stepY = dy == 0 ? 0 : dy / Math.abs(dy);

            for (int i = 1; i < steps; i++) {
                int checkX = from.getX() + i * stepX;
                int checkY = from.getY() + i * stepY;

                Element e = gameMap.getElementByIndex(checkX, checkY);
                if (e != null && e.getType() != null) {
                    String type = e.getType().toString();
                    if (type.contains("OBSTACLE")) {
                        if (e instanceof Obstacle) {
                            Obstacle obs = (Obstacle) e;
                            if (!obs.getTags().contains("CAN_SHOOT_THROUGH")) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                    if (type.contains("PLAYER") || type.contains("ENEMY")) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error checking obstacle: " + e.getMessage());
            return true;
        }
    }

    private void moveToWeaponOrPickup(Hero hero, Weapon w, GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Node me = new Node(player.getX(), player.getY());
        Node target = new Node(w.getX(), w.getY());
        if (PathUtils.distance(me, target) == 0) {
            try {
                System.out.println("Picking up weapon: " + w.getId());
                hero.pickupItem();
            } catch (Exception e) {
                System.out.println("Cannot pickup weapon: " + e.getMessage());
            }
        } else {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, target, false);
            if (path != null && !path.isEmpty()) {
                System.out.println("Moving to weapon: " + w.getId());
                hero.move(path);
            }
        }
    }

    private String getDirectionTo(Player player, Node target) {
        int dx = target.getX() - player.getX();
        int dy = target.getY() - player.getY();

        System.out.println("Direction calculation: dx=" + dx + ", dy=" + dy);
        System.out.println("From: (" + player.getX() + ", " + player.getY() + ") To: (" + target.getX() + ", " + target.getY() + ")");

        if (dx == -1 && dy == 0) return "l";
        if (dx == 1 && dy == 0) return "r";
        if (dx == 0 && dy == -1) return "d";
        if (dx == 0 && dy == 1) return "u";

        System.out.println("Cannot determine single direction - not adjacent");
        return null;
    }
}