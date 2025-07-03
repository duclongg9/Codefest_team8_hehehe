// StepHandler_ZoneDominator.java
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

public class StepHandler_ZoneDominator {

    private static final int SAFE_HP = 60;
    private static final int CONTROL_RADIUS = 6;

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();
        List<Node> avoid = BaseBotLogic.buildAvoidList(gameMap, player.getHealth() < SAFE_HP);

        // 1. Nếu chưa có súng → loot
        if (inv.getGun() == null) {
            Weapon gun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
            if (gun != null && BaseBotLogic.goTo(hero, gameMap, me, gun, avoid)) return;
        }

        // 2. Nếu máu thấp → tìm hồi máu
        if (player.getHealth() < SAFE_HP) {
            SupportItem heal = BaseBotLogic.getClosest(gameMap.getListSupportItems(), me);
            if (heal != null && BaseBotLogic.goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 3. Nếu có thính gần rơi, đoán vị trí và đến chiếm
        // (giả định bot biết rồng bay theo đường từ top->bottom, cố thủ vùng trung tâm)
        Node center = new Node(gameMap.getMapSize() / 2, gameMap.getMapSize() / 2);
        if (PathUtils.distance(me, center) > CONTROL_RADIUS) {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 4. Nếu có player lảng vảng khu trung tâm → bắn
//        if (inv.getGun() != null) {
//            Player intruder = getTargetInCenter(gameMap.getOtherPlayerInfo(), center, inv.getGun().getRange());
//            if (intruder != null) {
//                hero.shoot(BaseBotLogic.getDirection(me, intruder));
//                return;
//            }
//            if (BaseBotLogic.shootNearby(hero, gameMap, me, inv)) return;
//        }

        // 5. Luôn trong bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 6. Nếu đang ở trung tâm rồi → đứng canh
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me)) return;
    }

    private static Player getTargetInCenter(List<Player> players, Node center, int range) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 70)
                .filter(p -> PathUtils.distance(center, p) <= CONTROL_RADIUS)
                .filter(p -> PathUtils.distance(center, p) <= range)
                .findFirst()
                .orElse(null);
    }
}