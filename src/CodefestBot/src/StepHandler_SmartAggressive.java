// StepHandler_SmartAggressive.java
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.players.Player;

// Reuse shared helper methods
import java.util.*;
import java.io.IOException;

// Shared behaviours
import static java.util.Comparator.comparingDouble;

/**
 * Aggressive strategy that still prioritizes staying alive.
 * Uses common helpers from {@link BaseBotLogic} to reduce duplication.
 */

public class StepHandler_SmartAggressive {

    private static final Random RAND = new Random();
    private static final int MIN_ATTACK_HP = 50;

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;
        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();

        boolean hasGun = inv.getGun() != null;
        boolean isHealthy = player.getHealth() >= MIN_ATTACK_HP;

        List<Node> avoid = BaseBotLogic.buildAvoidList(gameMap, !hasGun || !isHealthy);

        // --- ƯU TIÊN LOOT ---
        if (!hasGun) {
            Weapon gun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
            if (gun != null && BaseBotLogic.goTo(hero, gameMap, me, gun, avoid)) return;
        }

        HealingItem heal = BaseBotLogic.getClosest(gameMap.getListHealingItems(), me);
        if (player.getHealth() < 50 && heal != null && BaseBotLogic.goTo(hero, gameMap, me, heal, avoid)) return;

        // --- ENGAGE KHI CÓ LỢI ---
        if (hasGun && isHealthy) {
            Player target = getWeakPlayer(gameMap.getOtherPlayerInfo(), me, inv.getGun().getRange());
            if (target != null) {
                hero.shoot(BaseBotLogic.getDirection(me, target));
                return;
            }
        }

        if (hasGun && BaseBotLogic.shootNearby(hero, gameMap, me, inv)) return;

        // --- TRÁNH VÙNG TỐI ---
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize()/2, gameMap.getMapSize()/2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // --- MOVE CHIẾM GIỮ VÙNG ĐẸP ---
        Node center = new Node(gameMap.getMapSize()/2, gameMap.getMapSize()/2);
        if (PathUtils.distance(me, center) > 5) {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // --- DODGE VÀ MỞ RƯƠNG ---
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me)) return;

        // --- RANDOM SAFE MOVE ---
        BaseBotLogic.moveRandom(hero, gameMap, me, avoid);
    }

    // Find a weak target within range
    private static Player getWeakPlayer(List<Player> players, Node from, int range) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 50)
                .filter(p -> PathUtils.distance(from, p) <= range)
                .min(comparingDouble(p -> PathUtils.distance(from, p)))
                .orElse(null);
    }

}