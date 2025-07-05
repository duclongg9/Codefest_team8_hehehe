import jsclub.codefest.sdk.Hero;

// Map update listener located in the same package



import java.io.IOException;


public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "141421";
    private static final String PLAYER_NAME = "Team8-Bot1";
    private static final String SECRET_KEY = "sk-Uu8f9Ci0S1ajs1FVzPj6VA:LL1RmJEI8qXH3vXsbywkIKdRTjHQV9nt4Hwp6ZDsRuhiND5F4gHnnZ8UUe6Dg1fJcREWowybEsJxQirLiqF2jg";



    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener(hero));
        hero.start(SERVER_URL);
    }
}