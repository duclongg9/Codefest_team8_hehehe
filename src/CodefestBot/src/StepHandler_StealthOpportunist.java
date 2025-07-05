// StepHandler_StealthOpportunist.java
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.players.Player;

import java.io.IOException;
import java.util.*;

import static java.util.Comparator.comparingDouble;

public class StepHandler_StealthOpportunist {

    private static final int MIN_ATTACK_HP = 60;
    private static final int LATE_GAME_TIME = 420; // seconds

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;
        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();

        List<Node> avoid = BaseBotLogic.buildAvoidList(gameMap, true);

        // 1. Nếu chưa có súng, ưu tiên loot
        if (inv.getGun() == null) {
            Weapon gun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
            if (gun != null && BaseBotLogic.goTo(hero, gameMap, me, gun, avoid)) return;
        }

        // 2. Nếu máu yếu < 50, tìm hồi máu
        if (player.getHealth() < 50) {
            SupportItem heal = BaseBotLogic.getClosest(gameMap.getListSupportItems(), me);
            if (heal != null && BaseBotLogic.goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 3. Late game (game > 7 phút), chủ động săn player yếu
//        int mapTime = gameMap.getMapSize();
//        if (mapTime >= LATE_GAME_TIME && inv.getGun() != null && player.getHealth() >= MIN_ATTACK_HP) {
//            Player target = getWeakPlayer(gameMap.getOtherPlayerInfo(), me, inv.getGun().getRange());
//            if (target != null) {
//                hero.shoot(BaseBotLogic.getDirection(me, target));
//                return;
//            }
//        }
//        if (inv.getGun() != null && BaseBotLogic.shootNearby(hero, gameMap, me, inv)) return;

        // 4. Luôn đảm bảo ở trong bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize()/2, gameMap.getMapSize()/2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 5. Di chuyển nhẹ vào vùng sáng hoặc gần loot
        Weapon w = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
        if (w != null && PathUtils.distance(me, w) < 5 && BaseBotLogic.goTo(hero, gameMap, me, w, avoid)) return;

        // 6. Random tránh enemy
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me)) return;

        BaseBotLogic.moveRandom(hero, gameMap, me, avoid);
    }

    private static Player getWeakPlayer(List<Player> players, Node from, int range) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 40)
                .filter(p -> PathUtils.distance(from, p) <= range)
                .min(comparingDouble(p -> PathUtils.distance(from, p)))
                .orElse(null);
    }
}