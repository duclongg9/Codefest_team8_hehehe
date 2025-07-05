

import BaseBotLogic;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.Inventory;

import java.io.IOException;
import java.util.List;

public class StepHandler_Joker {

    private static final int LOW_HP = 80;
    private static final int ENGAGE_RANGE = 4;

    public static void handleStep(GameMap map, Hero hero) throws IOException {
        if (map == null || hero == null) return;
        Player me = map.getCurrentPlayer();
        if (me == null || me.getHealth() == null || me.getHealth() <= 0) return;

        Node cur = new Node(me.getX(), me.getY());
        Inventory inv = hero.getInventory();
        List<Node> avoid = BaseBotLogic.buildAvoidList(map, true);

        /* anti‑stuck */
        if (BaseBotLogic.isStuck(cur)) {
            BaseBotLogic.resolveStuck(hero, map, cur,
                    BaseBotLogic.getClosest(map.getObstaclesByTag("DESTRUCTIBLE"), cur),
                    avoid);
            return;
        }

        /* heal */
        BaseBotLogic.useSupportIfLowHP(hero, inv, me.getHealth());

        /* né nguy hiểm */
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, map, cur)) return;
        if (BaseBotLogic.avoidEnemies(hero, map, cur)) return;

        /* giữ trong bo */
        if (!PathUtils.checkInsideSafeArea(cur, map.getSafeZone(), map.getMapSize())) {
            Node center = new Node(map.getMapSize()/2, map.getMapSize()/2);
            BaseBotLogic.goTo(hero, map, cur, center, avoid);
            return;
        }

        /* súng */
        if (BaseBotLogic.pickupGunIfNeeded(hero, map, cur)) return;

        /* chest & item */
        if (BaseBotLogic.breakChestIfNearby(hero, map, cur)) return;
        if (BaseBotLogic.pickupValuableItem(hero, map, cur)) return;

        /* bắn khi địch ≤ ENGAGE_RANGE (chỉ phòng thân) */
        int gunR = inv.getGun()!=null && inv.getGun().getRange()!=null ?
                Math.min(inv.getGun().getRange()[0], ENGAGE_RANGE) : 0;
        if (gunR>0 && BaseBotLogic.shootNearby(hero, map, cur, inv)) return;

        /* đi dạo lành mạnh */
        BaseBotLogic.moveRandom(hero, map, cur, avoid);
    }
}
