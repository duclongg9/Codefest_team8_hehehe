import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.players.Player;

import java.util.*;
import java.io.IOException;

import static java.util.Comparator.comparingDouble;

public class StepHandler_SmartAggressive {

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

        // 1. Loot súng nếu chưa có
        if (!hasGun) {
            Weapon gun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
            if (gun != null && BaseBotLogic.goTo(hero, gameMap, me, gun, avoid)) return;
        }

        // 2. Loot SupportItem nếu máu < 50
        if (player.getHealth() < 50) {
            SupportItem heal = BaseBotLogic.getClosest(gameMap.getListSupportItems(), me);
            if (heal != null && BaseBotLogic.goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 3. Tìm địch yếu trong tầm bắn
        if (hasGun && isHealthy) {
            Player target = getWeakPlayer(gameMap.getOtherPlayerInfo(), me, inv.getGun().getRange());
            if (target != null) {
                hero.shoot(BaseBotLogic.getDirection(me, target));
                return;
            }
        }

        // 4. Nếu có địch gần → bắn luôn
        if (hasGun && BaseBotLogic.shootNearby(hero, gameMap, me, inv)) return;

        // 5. Tránh vùng tối
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize() / 2, gameMap.getMapSize() / 2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, true);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 6. Di chuyển vào giữa map (chiếm vị trí đẹp)
        Node center = new Node(gameMap.getMapSize() / 2, gameMap.getMapSize() / 2);
        if (PathUtils.distance(me, center) > 5) {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 7. Phá rương và né đạn
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me)) return;

        // 8. Di chuyển ngẫu nhiên nếu không có gì
        BaseBotLogic.moveRandom(hero, gameMap, me, avoid);
    }

    // Tìm enemy yếu trong tầm
    private static Player getWeakPlayer(List<Player> players, Node from, int[] range) {
        int maxRange = (range != null && range.length >= 1) ? range[0] : 3;
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 50)
                .filter(p -> PathUtils.distance(from, p) <= maxRange)
                .min(comparingDouble(p -> PathUtils.distance(from, p)))
                .orElse(null);
    }
}
