

import BaseBotLogic;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.Inventory;

import java.io.IOException;
import java.util.List;

public class StepHandler_Superman {

    /* tham số chiến thuật */
    private static final int SAFE_MARGIN = 3;
    private static final int LOW_HP = 80;
    private static final int CRITICAL_HP = 60;
    private static final int ENGAGE_RANGE = 4;

    public static void handleStep(GameMap map, Hero hero) throws IOException {
        if (map == null || hero == null) return;
        Player me = map.getCurrentPlayer();
        if (me == null || me.getHealth() == null || me.getHealth() <= 0) return;

        Node cur = new Node(me.getX(), me.getY());
        Inventory inv = hero.getInventory();
        List<Node> avoid = BaseBotLogic.buildAvoidList(map, true);

        /* 0. tự gỡ kẹt nếu stuck ≥ 6 step */
        if (BaseBotLogic.isStuck(cur)) {
            BaseBotLogic.resolveStuck(hero, map, cur,
                    BaseBotLogic.getClosest(map.getObstaclesByTag("DESTRUCTIBLE"), cur),
                    avoid);
            return;
        }

        /* 1. heal sớm */
        BaseBotLogic.useSupportIfLowHP(hero, inv, me.getHealth());

        /* 2. né bullet & NPC */
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, map, cur)) return;
        if (BaseBotLogic.avoidEnemies(hero, map, cur)) return;

        /* 3. giữ trong bo với SAFE_MARGIN */
        if (!PathUtils.checkInsideSafeArea(cur, map.getSafeZone() - SAFE_MARGIN, map.getMapSize())) {
            Node center = new Node(map.getMapSize()/2, map.getMapSize()/2);
            BaseBotLogic.goTo(hero, map, cur, center, avoid);
            return;
        }

        /* 4. loot súng nếu chưa có */
        if (BaseBotLogic.pickupGunIfNeeded(hero, map, cur)) return;

        /* 5. phá chest gần nhất (≤2 ô) rồi loot */
        if (BaseBotLogic.breakChestIfNearby(hero, map, cur)) return;

        /* 6. nhặt Item/Weapon nhiều điểm */
        if (BaseBotLogic.pickupValuableItem(hero, map, cur)) return;

        /* 7. bắn địch trong ENGAGE_RANGE */
        int maxR = inv.getGun()!=null && inv.getGun().getRange()!=null ?
                Math.min(inv.getGun().getRange()[0], ENGAGE_RANGE) : 0;
        if (maxR>0 && BaseBotLogic.shootNearby(hero, map, cur, inv)) return;

        /* 8. nếu HP thấp < CRITICAL_HP → tìm SupportItem */
        if (me.getHealth() < CRITICAL_HP) {
            SupportItem s = BaseBotLogic.getClosest(map.getListSupportItems(), cur);
            if (s != null && BaseBotLogic.goTo(hero, map, cur, s, avoid)) return;
        }

        /* 9. không việc gì làm → đi random trong bo */
        BaseBotLogic.moveRandom(hero, map, cur, avoid);
    }
}
