import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.Hero;

import java.io.IOException;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "153904";
    private static final String PLAYER_NAME = "Team8-Bot1";
    private static final String SECRET_KEY = "sk-GdaqH5KQTMOJv3saId8OGQ:Tl-3murL582L5qOgCMqYu83NTXWKbzfHoMmzyUc0A7hMYh4ah3qs0ysgn_lz6tLT5QmvxPgxv0f52qTfB9IrDA";


    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);

        Emitter.Listener onMapUpdate = new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                GameMap gameMap = hero.getGameMap();
                gameMap.updateOnUpdateMap(args[0]);

            }
        };

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}

