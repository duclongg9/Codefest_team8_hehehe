import jsclub.codefest.sdk.Hero;

// Map update listener located in the same package



import java.io.IOException;


public class Main2 {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "198897";
    private static final String PLAYER_NAME = "Team8-Bot2";
    private static final String SECRET_KEY = "sk-GdaqH5KQTMOJv3saId8OGQ:Tl-3murL582L5qOgCMqYu83NTXWKbzfHoMmzyUc0A7hMYh4ah3qs0ysgn_lz6tLT5QmvxPgxv0f52qTfB9IrDA";


    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener2(hero));
        hero.start(SERVER_URL);
    }
}