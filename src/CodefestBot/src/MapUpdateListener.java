import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.*;

public class MapUpdateListener implements Emitter.Listener {

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

    /* ===================================================== */
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


            if (inv.getGun() != null && "RUBBER_GUN".equals(inv.getGun().getId())) {
                hero.revokeItem("RUBBER_GUN");                          // fallback
                return;
            }

            if (needHealing(player, inv)) {
                if (useHealingItem(hero, inv)) {
                    return;
                }
            }

            if (!hasInitialWeapon) {
                if (hasGoodWeapon(inv)) {
                    hasInitialWeapon = true;
                } else {
                    if (findAndGetWeapon(gameMap, player, avoid)) {
                        return;
                    }
                    hero.move("r");
                    return;
                }
                         // không có / không phá được
            }

            if (hasInitialWeapon && !hasLootedFirstChest) {
                if (targetChestLocation != null && waitingForItemsToAppear) {
                    handleChestLooting(gameMap, playerNode, avoid);
                    return;
                } else {
                    Node chest = getNearestChest(gameMap, player);
                    if (chest != null) {
                        if (canBreakChest(inv)) {
                            handleChestBreaking(hero, gameMap, player, avoid, chest);
                            return;
                        } else {
                            hasLootedFirstChest = true;
                        }
                    } else {
                        hasLootedFirstChest = true;
                    }
                }
            }


            if (hasInitialWeapon && hasLootedFirstChest) {
                if (shouldContinueCombat(player, inv)) {
                    Player enemy = getNearestPlayer(gameMap, player);
                    if (enemy != null) {
                        attackEnemyWithGunThenMelee(hero, gameMap, player, enemy, avoid);
                        return;
                    }
                }


                if (needBetterGun(inv)) {
                    if (findAndGetWeapon(gameMap, player, avoid)) {
                        return;
                    }
                }

                Node chest = getNearestChest(gameMap, player);
                if (chest != null && canBreakChest(inv)) {
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
                    attackEnemyWithGunThenMelee(hero, gameMap, player, enemy, avoid);
                    return;
                }
            }

            if (avoid.size() < gameMap.getMapSize() * gameMap.getMapSize()) {
                hero.move("r");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =====================================================
                H E A L
       ===================================================== */
    private boolean hasGoodWeapon(Inventory inv) throws IOException {
        if (inv.getGun() != null && !"RUBBER_GUN".equals(inv.getGun().getId()) && inv.getGun().getUseCount() > 0)
            return true;
        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) return true;
        if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) return true;
        return false;
    }

    /* =====================================================
                P H A S E   1   –  W E A P O N
       ===================================================== */
    private boolean canBreakChest(Inventory inv) throws IOException {
        if (inv.getGun() != null && inv.getGun().getUseCount() > 0) return true;
        if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) return true;
        if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) return true;
        if (inv.getThrowable() != null && inv.getThrowable().getUseCount() > 0) return true;
        return false;
    }

    private boolean findAndGetWeapon(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Weapon gun = getBestWeapon(gameMap.getAllGun(), player);
        if (gun != null && !"RUBBER_GUN".equals(gun.getId())) {
            moveToWeaponOrPickup(hero, gun, gameMap, player, avoid);
            return true;
        }

        Weapon melee = getBestWeapon(gameMap.getAllMelee(), player);
        if (melee != null && !"HAND".equals(melee.getId())) {
            moveToWeaponOrPickup(hero, melee, gameMap, player, avoid);
            return true;
        }

        Weapon special = getBestWeapon(gameMap.getAllSpecial(), player);
        if (special != null) {
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
        if (weapon.getType().toString().contains("GUN")) {
            if ("SHOTGUN".equals(id)) return 100;
            if ("CROSSBOW".equals(id)) return 90;
            if ("PISTOL".equals(id)) return 80;
            return 50;
        }

        if (weapon.getType().toString().contains("MELEE")) {
            if ("AXE".equals(id)) return 100;
            if ("SCEPTER".equals(id)) return 90;
            if ("BONE".equals(id)) return 80;
            if ("SAHUR_BAT".equals(id)) return 70;
            return 50;
        }

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
            hero.useItem(bestItem.getId());
            return true;
        }
        return false;
    }

    private boolean shouldContinueCombat(Player player, Inventory inv) throws IOException {
        return player.getHealth() > HEALTH_THRESHOLD && hasUsableWeapon(inv) && !inv.getListSupportItem().isEmpty();
    }

    private boolean needBetterGun(Inventory inv) throws IOException {
        return inv.getGun() == null || "RUBBER_GUN".equals(inv.getGun().getId()) || inv.getGun().getUseCount() <= 0;
    }

    private void handleChestLooting(GameMap gameMap, Node playerNode, List<Node> avoid) throws IOException {
        if (tryPickupAtCurrentPosition(gameMap, playerNode)) {
            waitCounter = 0;
            return;
        }

        for (String dir : directions) {
            Node neighbor = moveInDirection(playerNode, dir);
            if (neighbor != null && hasPickupableItem(gameMap, neighbor)) {
                String path = PathUtils.getShortestPath(gameMap, avoid, playerNode, neighbor, false);
                if (path != null && !path.isEmpty()) {
                    hero.move(path);
                    return;
                }
            }
        }

        waitCounter++;
        if (waitCounter >= MAX_WAIT_TURNS_FOR_PICKUP) {
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

        if (distance > 1) {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, chest, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            } else {
                String dir = getDirectionTowards(player, chest);
                if (dir != null) {
                    hero.move(dir);
                    return;
                }
            }
        }

        if (distance == 1) {
            String dir = getDirectionTo(player, chest);
            if (dir != null) {
                Inventory inv = hero.getInventory();
                boolean attackSuccessful = false;

                if (inv.getGun() != null && inv.getGun().getUseCount() > 0) {
                    hero.shoot(dir);
                    attackSuccessful = true;
                } else if (inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
                    hero.attack(dir);
                    attackSuccessful = true;
                } else if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) {
                    hero.useSpecial(dir);
                    attackSuccessful = true;
                } else if (inv.getThrowable() != null && inv.getThrowable().getUseCount() > 0) {
                    hero.throwItem(dir);
                    attackSuccessful = true;
                }

                if (attackSuccessful) {
                    targetChestLocation = new Node(chest.getX(), chest.getY());
                    waitingForItemsToAppear = true;
                    waitCounter = 0;
                } else {
                    if (!hasLootedFirstChest) {
                        hasLootedFirstChest = true;
                    }
                }
            }

        }

    }

    private String getDirectionTowards(Player player, Node target) {
        int dx = target.getX() - player.getX();
        int dy = target.getY() - player.getY();
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "r" : "l";
        } else {
            return dy > 0 ? "u" : "d";
        }
    }
    private void attackEnemyWithGunThenMelee(Hero hero, GameMap gameMap, Player player, Player enemy, List<Node> avoid) throws IOException {
        Node playerNode = new Node(player.getX(), player.getY());
        Node enemyNode = new Node(enemy.getX(), enemy.getY());
        int distance = PathUtils.distance(playerNode, enemyNode);
        Inventory inv = hero.getInventory();

        if (inv.getGun() != null && inv.getGun().getUseCount() > 0) {
            int[] gunRange = inv.getGun().getRange();
            if (distance >= gunRange[0] && distance <= gunRange[1]) {
                if (!hasObstacleBetweenForShooting(gameMap, playerNode, enemyNode)) {
                    String dir = getDirectionTo(player, enemyNode);
                    if (dir != null) {
                        hero.shoot(dir);
                        return;
                    }
                }
            }
            if (distance > gunRange[1]) {
                String path = PathUtils.getShortestPath(gameMap, avoid, playerNode, enemyNode, false);
                if (path != null && !path.isEmpty()) {
                    hero.move(String.valueOf(path.charAt(0)));
                    return;
                }
            }
        }

        if (distance == 1 && inv.getMelee() != null && !"HAND".equals(inv.getMelee().getId())) {
            String dir = getDirectionTo(player, enemyNode);
            if (dir != null) {
                hero.attack(dir);
                return;
            }
        }

        if (inv.getSpecial() != null && inv.getSpecial().getUseCount() > 0) {
            int[] specialRange = inv.getSpecial().getRange();
            if (distance >= specialRange[0] && distance <= specialRange[1]) {
                String dir = getDirectionTo(player, enemyNode);
                if (dir != null) {
                    hero.useSpecial(dir);
                    return;
                }
            }
        }

        if (inv.getThrowable() != null && inv.getThrowable().getUseCount() > 0) {
            int[] throwRange = inv.getThrowable().getRange();
            if (distance >= throwRange[0] && distance <= throwRange[1]) {
                String dir = getDirectionTo(player, enemyNode);
                if (dir != null) {
                    hero.throwItem(dir);
                    return;
                }
            }
        }

        String path = PathUtils.getShortestPath(gameMap, avoid, playerNode, enemyNode, false);
        if (path != null && !path.isEmpty()) {
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
                hero.pickupItem();
                return true;
            }
        } catch (Exception e) {
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

        List<String> highTierSpecials = List.of("MACE", "MAGIC_POTION", "ELIXIR_OF_LIFE", "GOD_LEAF", "SPIRIT_TEAR",
                "PHOENIX_FEATHERS", "UNICORN_BLOOD", "MERMAID_TAIL", "ELIXIR");
        if (type.contains("SPECIAL")) {
            if (inv.getSpecial() == null) return true;
            String currentId = inv.getSpecial().getId();
            if ("MACE".equals(id)) return true;
            if ("MACE".equals(currentId)) return false;
            if (highTierSpecials.contains(id) && !highTierSpecials.contains(currentId)) return true;
            return false;
        }

        List<String> meleeRanking = List.of("AXE", "SCEPTER", "BONE", "SAHUR_BAT");
        if (type.contains("MELEE")) {
            if (inv.getMelee() == null || "HAND".equals(inv.getMelee().getId())) return true;
            String currentId = inv.getMelee().getId();
            int currentRank = meleeRanking.indexOf(currentId);
            int newRank = meleeRanking.indexOf(id);
            return newRank != -1 && (currentRank == -1 || newRank < currentRank);
        }

        List<String> gunRanking = List.of("SHOTGUN", "CROSSBOW", "PISTOL");
        if (type.contains("GUN")) {
            if ("RUBBER_GUN".equals(id)) return false;
            if (inv.getGun() == null) return true;
            String currentId = inv.getGun().getId();
            if ("RUBBER_GUN".equals(currentId)) return true;
            int currentRank = gunRanking.indexOf(currentId);
            int newRank = gunRanking.indexOf(id);
            return newRank != -1 && (currentRank == -1 || newRank < currentRank);
        }

        if (type.contains("HEALING") || type.contains("SUPPORT")) {
            return inv.getListSupportItem().size() < 3;
        }

        if (type.contains("ARMOR")) {
            if (type.contains("HELMET")) {
                return inv.getHelmet() == null;
            } else {
                return inv.getArmor() == null;
            }
        }

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
            return true;
        }
    }

    private void moveToWeaponOrPickup(Hero hero, Weapon w, GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Node me = new Node(player.getX(), player.getY());
        Node target = new Node(w.getX(), w.getY());
        if (PathUtils.distance(me, target) == 0) {
            try {
                hero.pickupItem();
            } catch (Exception e) {
            }
        } else {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, target, false);
            if (path != null && !path.isEmpty()) {
                hero.move(path);
            }
        }
    }

    private String getDirectionTo(Player player, Node target) {
        int dx = target.getX() - player.getX();
        int dy = target.getY() - player.getY();
        if (dx == -1 && dy == 0) return "l";
        if (dx == 1 && dy == 0) return "r";
        if (dx == 0 && dy == -1) return "d";
        if (dx == 0 && dy == 1) return "u";
        return null;
    }
}
