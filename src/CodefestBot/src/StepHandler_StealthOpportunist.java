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
    private static final int LATE_GAME_STEP = 480; // = 4 phút (trong 5 phút game)

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;

        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();
        List<Node> avoid = BaseBotLogic.buildAvoidList(gameMap, true);

        // 1. Nếu chưa có súng, đi loot súng
        if (inv.getGun() == null) {
            Weapon gun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
            if (gun != null && BaseBotLogic.goTo(hero, gameMap, me, gun, avoid)) return;
        }

        // 1b. Nhặt vật phẩm giá trị cao nếu tiện
        if (BaseBotLogic.pickupValuableItem(hero, gameMap, me)) return;

        // 2. Nếu máu < 50 → ưu tiên SupportItem
        if (player.getHealth() < 50) {
            SupportItem heal = BaseBotLogic.getClosest(gameMap.getListSupportItems(), me);
            if (heal != null && BaseBotLogic.goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 3. Nếu late game → chủ động săn người yếu
        int step = gameMap.getStepNumber();
        if (step >= LATE_GAME_STEP && inv.getGun() != null && player.getHealth() >= MIN_ATTACK_HP) {
            Player target = getWeakPlayer(gameMap.getOtherPlayerInfo(), me, inv.getGun().getRange());
            if (target != null) {
                hero.shoot(BaseBotLogic.getDirection(me, target));
                return;
            }
        }

        // 4. Nếu enemy ở gần trong range → bắn
        if (inv.getGun() != null && BaseBotLogic.shootNearby(hero, gameMap, me, inv)) return;

        // 5. Luôn đảm bảo trong bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize() / 2, gameMap.getMapSize() / 2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, true);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 6. Nếu có vũ khí gần (trong 5 ô) thì đi lấy
        Weapon w = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
        if (w != null && PathUtils.distance(me, w) < 5 && BaseBotLogic.goTo(hero, gameMap, me, w, avoid)) return;

        // 7. Hành vi né và phá rương nếu tiện
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me)) return;

        // 8. Nếu không có gì → di chuyển ngẫu nhiên an toàn
        BaseBotLogic.moveRandom(hero, gameMap, me, avoid);
    }

    private static Player getWeakPlayer(List<Player> players, Node from, int[] range) {
        int maxRange = (range != null && range.length >= 1) ? range[0] : 3;
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 40)
                .filter(p -> PathUtils.distance(from, p) <= maxRange)
                .min(comparingDouble(p -> PathUtils.distance(from, p)))
                .orElse(null);
    }
}
