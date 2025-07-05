import jsclub.codefest.sdk.Hero;

// Map update listener located in the same package



import java.io.IOException;


public class Main4 {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "198897";
    private static final String PLAYER_NAME = "Team8-Bot4";
    private static final String SECRET_KEY = "sk-CIWbSOsQSG2yaFmYIpJRTw:E4WFjJIV4drMuK_L3Dxm0B0pv9FYfY4LdTK9UwuV7bQeToK3TqLzyHaLl4QOeizl0lhO74GJ25tBVs2AMWiSRw";


    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener4(hero));
        hero.start(SERVER_URL);
    }
}