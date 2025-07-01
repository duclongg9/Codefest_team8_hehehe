import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;

import java.io.IOException;

public class StepHandler_Template {

    public static void handleStep(GameMap map, Hero hero) throws IOException {
        if (map == null || hero == null) return;
        if (map.getCurrentPlayer() == null || map.getCurrentPlayer().getHealth() == null || map.getCurrentPlayer().getHealth() <= 0)
            return;

        Node me = new Node(map.getCurrentPlayer().getX(), map.getCurrentPlayer().getY());
        Inventory inv = hero.getInventory();

        if (BaseBotLogic.pickupGunIfNeeded(hero, map, me)) return;
        if (BaseBotLogic.avoidEnemies(hero, map, me)) return;
        if (BaseBotLogic.shootNearby(hero, map, me, inv)) return;
        if (BaseBotLogic.breakChestIfNearby(hero, map, me)) return;
        BaseBotLogic.dodgeBulletIfTargeted(hero, map, me);
        // Custom strategy goes here


    }
}