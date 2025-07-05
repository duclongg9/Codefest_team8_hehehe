//import jsclub.codefest.sdk.*;
//import jsclub.codefest.sdk.algorithm.PathUtils;
//import jsclub.codefest.sdk.base.Node;
//import jsclub.codefest.sdk.model.*;
//import jsclub.codefest.sdk.model.players.Player;
//
//import java.io.IOException;
//import java.util.List;
//
///** Champion v1.5 – 3 pha: loot ▶ chest ▶ combat */
//public class StepHandler_Champion {
//
//    public static void handleStep(GameMap map, Hero hero) throws IOException {
//
//        if (map==null || hero==null) return;
//        Player me = map.getCurrentPlayer();
//        if (me==null || me.getHealth()==null || me.getHealth()<=0) return;
//
//        Node cur  = new Node(me.getX(),me.getY());
//        Inventory inv = hero.getInventory();
//
//        List<Node> avoidCombat = BaseBotLogic.buildAvoidList(map,true);
//        List<Node> avoidLoot   = BaseBotLogic.buildAvoidList(map,false);
//
//        /* 0. anti-stuck */
//        if (BaseBotLogic.isStuck(cur)) {
//            BaseBotLogic.resolveStuck(hero,map,cur,
//                    BaseBotLogic.getClosest(map.getObstaclesByTag("PULLABLE_ROPE"),cur),
//                    avoidCombat);
//            return;
//        }
//
//        /* 1. survival */
//        BaseBotLogic.healIfNeeded(hero,inv,me.getHealth());
//        if (BaseBotLogic.dodgeBulletIfTargeted(hero,map,cur)) return;
//        if (BaseBotLogic.avoidEnemies(hero,map,cur)) return;
//
//        /* 2. zone */
//        if (BaseBotLogic.keepInsideSafeZone(hero,map,cur,avoidCombat)) return;
//
//        /* 3. chest break / loot */
//        if (BaseBotLogic.breakChestIfNearby(hero,map,cur)) return;
//
//        /* 4. pickup vũ khí xịn ngay khi chưa có hoặc gun kém */
//        if (BaseBotLogic.pickupGunIfBetter(hero,map,cur)) return;
//
//        /* 5. upgrade gun & nhặt item giá trị */
//        BaseBotLogic.autoUpgradeGun(hero,map,cur);
//        if (BaseBotLogic.pickupValuableItem(hero,map,cur)) return;
//
//        /* 6. combat */
//        if (inv.getGun()!=null) {
//            if (BaseBotLogic.shootNearby(hero,map,cur,inv)) return;
//        } else {
//            if (BaseBotLogic.attackNearby(hero,map,cur)) return;
//        }
//
//        /* 7. fallback */
//        BaseBotLogic.moveRandom(hero,map,cur,avoidCombat);
//    }
//}