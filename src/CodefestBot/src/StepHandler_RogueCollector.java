// StepHandler_RogueCollector.java
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

import static java.util.Comparator.comparingDouble;

public class StepHandler_RogueCollector {

    public static void handleStep(GameMap gameMap, Hero hero) throws IOException {
        if (gameMap == null || hero == null) return;
        Player player = gameMap.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;
        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();

        List<Node> avoid = BaseBotLogic.buildAvoidList(gameMap, false);

        // 1. Ưu tiên nhặt súng nếu chưa có
        if (inv.getGun() == null) {
            Weapon gun = BaseBotLogic.getClosest(gameMap.getAllGun(), me);
            if (gun != null && BaseBotLogic.goTo(hero, gameMap, me, gun, avoid)) return;
        }

        // 2. Ưu tiên rương nếu có thể (collector mode)
        Obstacle chest = BaseBotLogic.getClosest(gameMap.getListObstacles(), me);
        if (chest != null && BaseBotLogic.goTo(hero, gameMap, me, chest, avoid)) return;

        // 3. Nhặt healing nếu máu < 60
        if (player.getHealth() < 60) {
            SupportItem heal = BaseBotLogic.getClosest(gameMap.getListSupportItems(), me);
            if (heal != null && BaseBotLogic.goTo(hero, gameMap, me, heal, avoid)) return;
        }

        // 4. Bắn chỉ khi bị chắn đường hoặc địch yếu
//        if (inv.getGun() != null) {
//            Player block = getWeakPlayerNearby(gameMap.getOtherPlayerInfo(), me, inv.getGun().getRange());
//            if (block != null) {
//                hero.shoot(BaseBotLogic.getDirection(me, block));
//                return;
//            }
//            if (BaseBotLogic.shootNearby(hero, gameMap, me, inv)) return;
//        }

        // 5. Luôn đảm bảo trong bo
        if (!PathUtils.checkInsideSafeArea(me, gameMap.getSafeZone(), gameMap.getMapSize())) {
            Node center = new Node(gameMap.getMapSize()/2, gameMap.getMapSize()/2);
            String path = PathUtils.getShortestPath(gameMap, avoid, me, center, false);
            if (path != null && !path.isEmpty()) {
                hero.move(String.valueOf(path.charAt(0)));
                return;
            }
        }

        // 6. Nếu không có gì: di chuyển an toàn
        if (BaseBotLogic.breakChestIfNearby(hero, gameMap, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, gameMap, me)) return;

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