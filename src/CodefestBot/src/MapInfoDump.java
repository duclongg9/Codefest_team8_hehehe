import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.model.GameMap;

public class MapInfoDump {
    // TODO: Fill in your credentials
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "YOUR_GAME_ID";
    private static final String PLAYER_NAME = "YOUR_BOT_NAME";
    private static final String SECRET_KEY = "YOUR_SECRET_KEY";

    public static void main(String[] args) throws Exception {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapListener(hero));
        hero.start(SERVER_URL);
    }

    private static class MapListener implements Emitter.Listener {
        private final Hero hero;
        MapListener(Hero hero) { this.hero = hero; }

        @Override
        public void call(Object... args) {
            if (args == null || args.length == 0) return;
            GameMap map = hero.getGameMap();
            map.updateOnUpdateMap(args[0]);
            dump(map);
        }
    }

    private static void dump(GameMap m) {
        System.out.println("Step: " + m.getStepNumber());
        System.out.println("Map size: " + m.getMapSize());
        System.out.println("Safe zone radius: " + m.getSafeZone());
        System.out.println("Weapons: " + m.getListWeapons().size());
        System.out.println("Support items: " + m.getListSupportItems().size());
        System.out.println("Armors: " + m.getListArmors().size());
        System.out.println("Enemies: " + m.getListEnemies().size());
        System.out.println("Allies: " + m.getListAllies().size());
        System.out.println("Obstacles: " + m.getListObstacles().size());
        System.out.println("Bullets: " + m.getListBullets().size());
        System.out.println("Other players: " + m.getOtherPlayerInfo().size());
        System.out.println("Current HP: " +
                (m.getCurrentPlayer() != null ? m.getCurrentPlayer().getHealth() : "n/a"));
        System.out.println("-----");
    }
}