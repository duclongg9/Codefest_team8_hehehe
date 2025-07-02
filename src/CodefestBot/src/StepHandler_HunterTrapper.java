// StepHandler_HunterTrapper.java
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.players.Player;

import java.io.IOException;
import java.util.*;

public class StepHandler_HunterTrapper {

    private static final int AGGRESSIVE_HP = 70;
    private static final int RETREAT_HP = 40;

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;
        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();

        List<Node> avoid = BaseBotLogic.buildAvoidList(gameMap, player.getHealth() < AGGRESSIVE_HP);

        // 1. Nếu máu yếu, ưu tiên hồi máu
        if (player.getHealth() < RETREAT_HP) {
            HealingItem heal = BaseBotLogic.getClosest(gameMap.getListHealingItems(), me);
            if (heal != null && BaseBotLogic.goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 2. Nếu chưa có súng → loot gấp
        if (BaseBotLogic.pickupGunIfNeeded(hero, gameMap, me))
            return;


//        // 3. Giữ khu vực trung tâm loot (rương/súng)
        Weapon nearbyGun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
        if (nearbyGun != null && PathUtils.distance(me, nearbyGun) < 4 && BaseBotLogic.goTo(hero, gameMap, me, nearbyGun, avoid)) return;

        // 4. Nếu có player đến gần và yếu → tấn công
        Player prey = getWeakApproachingPlayer(gameMap.getOtherPlayerInfo(), me, inv.getGun() != null ? inv.getGun().getRange() : 3);
        if (prey != null) {
            hero.shoot(BaseBotLogic.getDirection(me, prey));
            return;
        }

        // 5. Luôn đảm bảo ở trong bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize()/2, gameMap.getMapSize()/2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        if (BaseBotLogic.avoidEnemies(hero, gameMap, me)) return;
        if (BaseBotLogic.shootNearby(hero, gameMap, me, inv)) return;
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;
        BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me);

        // 6. Nếu không có gì làm, giữ vị trí và không chạy lung tung

        // (tạo cảm giác mai phục)
    }

    private static Player getWeakApproachingPlayer(List<Player> players, Node from, int range) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 60)
                .filter(p -> PathUtils.distance(from, p) <= range)
                .findFirst()
                .orElse(null);
    }


}