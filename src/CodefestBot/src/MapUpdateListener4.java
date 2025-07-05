import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.model.GameMap;
import java.io.IOException;

public class MapUpdateListener4 implements Emitter.Listener {
    private final Hero hero;

    public MapUpdateListener4(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        if (args == null || args.length == 0) return;
        GameMap gameMap = hero.getGameMap();
        gameMap.updateOnUpdateMap(args[0]);
        try {
            StepHandler_Superman.handleStep(gameMap, hero);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}