//StepHandler_HunterTrapper
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparingDouble;

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

        // 1. Nếu máu yếu → tìm SupportItem
        if (player.getHealth() < RETREAT_HP) {
            SupportItem supportItem = BaseBotLogic.getClosest(gameMap.getListSupportItems(), me);
            if (supportItem != null && BaseBotLogic.goTo(hero, gameMap, me, supportItem, avoid)) return;
        }

        // 2. Nếu chưa có súng → ưu tiên loot
        if (BaseBotLogic.pickupGunIfNeeded(hero, gameMap, me))
            return;
        // 2b. Nhặt vật phẩm giá trị cao
        if (BaseBotLogic.pickupValuableItem(hero, gameMap, me)) return;

        // 3. Nếu có enemy yếu đi vào vùng mình giữ → bắn
        Player prey = getWeakApproachingPlayer(gameMap.getOtherPlayerInfo(), me, getGunRange(inv));
        if (prey != null) {
            hero.shoot(BaseBotLogic.getDirection(me, prey));
            return;
        }

//        // 3. Giữ khu vực trung tâm loot (rương/súng)
//        Weapon nearbyGun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
//        if (nearbyGun != null && PathUtils.distance(me, nearbyGun) < 4 && BaseBotLogic.goTo(hero, gameMap, me, nearbyGun, avoid)) return;

        // 4. Nếu có player đến gần và yếu → tấn công
//        Player prey = getWeakApproachingPlayer(gameMap.getOtherPlayerInfo(), me, inv.getGun() != null ? inv.getGun().getRange() : 3);
//        if (prey != null) {
//            hero.shoot(BaseBotLogic.getDirection(me, prey));
//            return;
//        }

        // 5. Luôn đảm bảo ở trong bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize() / 2, gameMap.getMapSize() / 2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, true);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 5. Ưu tiên bắn nếu có enemy gần
        if (BaseBotLogic.shootNearby(hero, gameMap, me, inv)) return;

        // 6. Né đạn, tránh enemy nếu cần
        if (BaseBotLogic.avoidEnemies(hero, gameMap, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me)) return;

        // 7. Nếu đứng cạnh rương → phá
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;

        // 8. Nếu máu cao và có súng → chủ động tìm player
        if (inv.getGun() != null && player.getHealth() >= AGGRESSIVE_HP) {
            Player enemy = getClosestPlayer(gameMap.getOtherPlayerInfo(), me);
            if (enemy != null && BaseBotLogic.goTo(hero, gameMap, me, enemy, avoid)) return;
        }

        // 9. Nếu không có gì làm → di chuyển ngẫu nhiên có kiểm soát
        BaseBotLogic.moveRandom(hero, gameMap, me, avoid);
    }

    private static int getGunRange(Inventory inv) {
        Weapon gun = inv.getGun();
        if (gun != null && gun.getRange() != null && gun.getRange().length >= 1) {
            return gun.getRange()[0];  // Lấy khoảng cách tối đa
        }
        return 3; // fallback mặc định
    }

    private static Player getWeakApproachingPlayer(List<Player> players, Node from, int range) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 60)
                .filter(p -> PathUtils.distance(from, p) <= range)
                .findFirst()
                .orElse(null);
    }

    private static Player getClosestPlayer(List<Player> players, Node from) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() > 0)
                .min(comparingDouble(p -> PathUtils.distance(from, p)))
                .orElse(null);
    }
}
