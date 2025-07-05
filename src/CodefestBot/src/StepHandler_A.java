import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.weapon.Bullet;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.npcs.Ally;

import java.io.IOException;
import java.util.*;


public class StepHandler_A {
    private static final int LOW_HP = 60;
    private static final int AGGRESSIVE_HP = 70;
    private static Node lastPosition = null;
    private static int stuckCount = 0;

    public static void handleStep(GameMap map, Hero hero) throws IOException {
        if (map == null || hero == null) return;
        Player player = map.getCurrentPlayer();
        if (player == null || player.getHealth() == null || player.getHealth() <= 0) return;

        Node me = new Node(player.getX(), player.getY());
        Inventory inv = hero.getInventory();
        float hp = player.getHealth();
        boolean hasGun = inv.getGun() != null;

        // Theo dõi stuck vị trí
        if (lastPosition != null && lastPosition.equals(me)) stuckCount++;
        else stuckCount = 0;
        lastPosition = me;

        // Kỹ năng cơ bản
        if (BaseBotLogic.pickupGunIfNeeded(hero, map, me)) return;
        if (BaseBotLogic.avoidEnemies(hero, map, me)) return;
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, map, me)) return;
        if (BaseBotLogic.breakChestIfNearby(hero, map, me)) return;
        if (BaseBotLogic.shootNearby(hero, map, me, inv)) return;

        // Hồi máu nếu HP thấp
        if (hp < LOW_HP && !inv.getListSupportItem().isEmpty()) {
            hero.useItem(inv.getListSupportItem().get(0).getId());
            return;
        }

        // Xây danh sách tránh
        List<Node> avoid = BaseBotLogic.buildAvoidList(map, !hasGun || hp < LOW_HP);
        for (Bullet b : map.getListBullets()) {
            avoid.add(new Node(b.getDestinationX(), b.getDestinationY()));
        }

        // Nhặt súng nếu chưa có
        if (!hasGun) {
            Weapon gun = getClosest(map.getAllGun(), me);
            if (gun != null && BaseBotLogic.goTo(hero, map, me, gun, avoid)) return;
        }

        // Nhặt vật phẩm hồi máu nếu HP yếu
        if (hp < LOW_HP) {
            SupportItem heal = getClosest(map.getListSupportItems(), me);
            if (heal != null && BaseBotLogic.goTo(hero, map, me, heal, avoid)) return;
        }

        // Tìm NPC đồng minh (vòng xanh) gần nhất và đến nếu gần
        Ally nearestAlly = getClosest(map.getListAllies(), me);
        if (nearestAlly != null && PathUtils.distance(me, nearestAlly) <= 5) {
            if (BaseBotLogic.goTo(hero, map, me, nearestAlly, avoid)) return;
        }

        // Tìm mục tiêu yếu và gần
        if (hasGun && hp > AGGRESSIVE_HP) {
            Player target = map.getOtherPlayerInfo().stream()
                    .filter(p -> p.getHealth() != null && p.getHealth() < 60)
                    .filter(p -> PathUtils.distance(me, p) <= inv.getGun().getRange()[0])
                    .sorted(Comparator.comparingDouble(p -> p.getHealth()))
                    .findFirst().orElse(null);
            if (target != null) {
                hero.shoot(getDirection(me, target));
                return;
            }
        }

        // Di chuyển về trung tâm nếu ở ngoài bo hoặc quá xa
        Node center = new Node(map.getMapSize() / 2, map.getMapSize() / 2);
        if (!PathUtils.checkInsideSafeArea(me, map.getSafeZone(), map.getMapSize()) || PathUtils.distance(me, center) > 6) {
            if (BaseBotLogic.goTo(hero, map, me, center, avoid)) return;
        }

        // Nếu bị kẹt nhiều bước thì di chuyển ngẫu nhiên
        if (stuckCount >= 3) {
            BaseBotLogic.moveRandom(hero, map, me, avoid);
            stuckCount = 0;
            return;
        }

        BaseBotLogic.moveRandom(hero, map, me, avoid);
    }

    private static <T extends Node> T getClosest(List<T> list, Node from) {
        return list.stream().min(Comparator.comparingDouble(n -> PathUtils.distance(from, n))).orElse(null);
    }

    private static String getDirection(Node from, Node to) {
        if (to.x < from.x) return "l";
        if (to.x > from.x) return "r";
        if (to.y < from.y) return "u";
        if (to.y > from.y) return "d";
        return "";
    }
}