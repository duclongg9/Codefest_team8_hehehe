import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;

import java.io.IOException;
import java.util.*;

public class StepHandler_RogueCollector {

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;

        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();

        List<Node> avoid = BaseBotLogic.buildAvoidList(gameMap, false);

        // 1. Nhặt súng nếu chưa có
        if (inv.getGun() == null) {
            Weapon gun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
            if (gun != null && BaseBotLogic.goTo(hero, gameMap, me, gun, avoid)) return;
        }

        // 1b. Nhặt vật phẩm giá trị cao
        if (BaseBotLogic.pickupValuableItem(hero, gameMap, me)) return;

        // 2. Tìm rương gần nhất (rương = obstacle có tag "DESTRUCTIBLE")
        List<Obstacle> chests = gameMap.getObstaclesByTag("DESTRUCTIBLE");
        Obstacle chest = BaseBotLogic.getClosest(chests, me);
        if (chest != null && BaseBotLogic.goTo(hero, gameMap, me, chest, avoid)) return;

        // 3. Nếu máu < 60 → tìm support item
        if (player.getHealth() < 60) {
            SupportItem heal = BaseBotLogic.getClosest(gameMap.getListSupportItems(), me);
            if (heal != null && BaseBotLogic.goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 4. Không bắn nếu không cần → chỉ xử lý khi chắn đường hoặc enemy rất yếu

        if (inv.getGun() != null) {
            int[] range = inv.getGun().getRange();
            int maxRange = (range != null && range.length > 0) ? range[0] : 3;
            Player block = getWeakPlayerNearby(gameMap.getOtherPlayerInfo(), me, maxRange);
            if (block != null) {
                hero.shoot(BaseBotLogic.getDirection(me, block));
                return;
            }
        }


        // 5. Nếu ngoài bo → quay vào bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize()/2, gameMap.getMapSize()/2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, true);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 6. Hành vi phụ: phá rương, né đạn
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me)) return;

        // 7. Không có gì → di chuyển ngẫu nhiên để tránh bị phục kích
        BaseBotLogic.moveRandom(hero, gameMap, me, avoid);
    }

    private static Player getWeakPlayerNearby(List<Player> players, Node from, int range) {
        return players.stream()
                .filter(p -> p.getHealth() != null && p.getHealth() < 40)
                .filter(p -> PathUtils.distance(from, p) <= range)
                .findFirst()
                .orElse(null);
    }
}
