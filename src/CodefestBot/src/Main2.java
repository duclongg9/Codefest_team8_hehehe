import jsclub.codefest.sdk.Hero;

import java.io.IOException;

public class Main2 {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "194755";
    private static final String PLAYER_NAME = "Huyen";
    private static final String SECRET_KEY = "sk-dkta4PFURHmV_RECouwuPA:w38nWATI0S0gtXWTi0ZvzlRsfg5ynYNQVfNiWnhK28dO2Dl3qgNVORAnZ_19B5RPpfIWUCn1xiIXczA8hNvvPA";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener(hero));
        hero.start(SERVER_URL);
    }
}

