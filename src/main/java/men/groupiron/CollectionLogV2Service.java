package men.groupiron;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class CollectionLogV2Service {
    // item id -> quantity
    private final Map<Integer, Integer> clogItems = new HashMap<>();

    public synchronized void storeClogItem(int itemId, int quantity) {
        if (quantity <= 0) return;
        clogItems.put(itemId, quantity);
    }

    public synchronized void consumeClogItems(Map<String, Object> updates) {
        if (clogItems.isEmpty()) return;
        updates.put("collection_log_v2", new HashMap<>(clogItems));
    }

    public synchronized void clearClogItems() {
        clogItems.clear();
    }

    @Subscribe
    public synchronized void onGameStateChanged(GameStateChanged ev) {
        if (ev.getGameState() != GameState.LOGGED_IN) {
            clogItems.clear();
        }
    }

    public synchronized Map<Integer, Integer> snapshotItems() {
        return Collections.unmodifiableMap(new HashMap<>(clogItems));
    }
}
