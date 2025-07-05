import jsclub.codefest.sdk.Hero;

// Map update listener located in the same package



import java.io.IOException;


public class Main3 {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "140159";
    private static final String PLAYER_NAME = "Team8-Bot3";
    private static final String SECRET_KEY = "sk-ppO7iXXEQiy93pAlB3HO_A:auaf63I91SIcLmgR7aH3XC7hUDka-VGoSCLceKmYQPJ_sl_TvfYTdl3RJ3N7Ns7NDOJ2jFtfWUe3lRnpe_DaJA";


    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener3(hero));
        hero.start(SERVER_URL);
    }
}