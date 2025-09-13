package men.groupiron;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ItemNameLookup {
    private static final String ITEM_CACHE_BASE_URL = "https://static.runelite.net/cache/item/";

    private final Map<String, Integer> nameToId = new ConcurrentHashMap<>(16384);

    @Inject
    private HttpRequestService httpRequestService;

    @Inject
    private Gson gson;

    public void startUp() {
        init();
    }

    public void shutDown() {
        nameToId.clear();
    }

    public Integer findItemId(@NonNull String name) {
        return nameToId.get(name);
    }

    private void init() {
        queryNamesById()
                .thenAcceptBothAsync(
                        queryNotedItemIds().exceptionally(e -> {
                            log.error("Failed to read noted items", e);

                            return Collections.emptySet();
                        }),
                        this::populate)
                .exceptionally(e -> {
                    log.error("Failed to read item names", e);

                    return null;
                });
    }

    private void populate(@NonNull Map<Integer, String> namesById, @NonNull Set<Integer> notedIds) {
        namesById.forEach((id, name) -> {
            if (!notedIds.contains(id)) nameToId.putIfAbsent(name, id);
        });

        log.debug("Completed initialization of item cache with {} entries", nameToId.size());
    }

    private CompletableFuture<Map<Integer, String>> queryNamesById() {
        return queryCache("names.json", new TypeToken<Map<Integer, String>>() {});
    }

    private CompletableFuture<Set<Integer>> queryNotedItemIds() {
        return queryCache("notes.json", new TypeToken<Map<Integer, Integer>>() {})
                .thenApply(Map::keySet);
    }

    private <T> CompletableFuture<T> queryCache(@NonNull String fileName, @NonNull TypeToken<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = ITEM_CACHE_BASE_URL + fileName;
                HttpRequestService.HttpResponse response = httpRequestService.get(url, null);
                if (!response.isSuccessful()) {
                    throw new RuntimeException("HTTP " + response.getCode() + " for " + url);
                }

                return gson.fromJson(response.getBody(), type.getType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
