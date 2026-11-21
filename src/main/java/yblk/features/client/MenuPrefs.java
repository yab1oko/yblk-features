package yblk.features.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import yblk.features.feature.Feature;
import yblk.features.feature.FeatureCategory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Client-only menu memory: favorites, collapsed groups, sort and filter. */
public final class MenuPrefs {
    public enum Sort {
        CATEGORY,
        AZ
    }

    public enum EnabledFilter {
        ANY,
        ON,
        OFF;

        public EnabledFilter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> FAVORITES = new HashSet<>();
    private static final Set<String> COLLAPSED = new HashSet<>();
    private static Sort sort = Sort.CATEGORY;
    private static EnabledFilter enabledFilter = EnabledFilter.ANY;
    private static boolean loaded;

    private MenuPrefs() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            readSet(root, "favorites", FAVORITES);
            readSet(root, "collapsed", COLLAPSED);
            if (root.has("sort")) {
                sort = "az".equalsIgnoreCase(root.get("sort").getAsString()) ? Sort.AZ : Sort.CATEGORY;
            }
            if (root.has("filter")) {
                enabledFilter = switch (root.get("filter").getAsString()) {
                    case "on" -> EnabledFilter.ON;
                    case "off" -> EnabledFilter.OFF;
                    default -> EnabledFilter.ANY;
                };
            }
        } catch (Exception ignored) {
            FAVORITES.clear();
            COLLAPSED.clear();
        }
    }

    public static boolean isFavorite(Feature feature) {
        load();
        return FAVORITES.contains(feature.getId());
    }

    public static void toggleFavorite(Feature feature) {
        load();
        if (!FAVORITES.remove(feature.getId())) {
            FAVORITES.add(feature.getId());
        }
        save();
    }

    public static boolean isCollapsed(String key) {
        load();
        return COLLAPSED.contains(key);
    }

    public static void toggleCollapsed(String key) {
        load();
        if (!COLLAPSED.remove(key)) {
            COLLAPSED.add(key);
        }
        save();
    }

    public static String collapseKey(FeatureCategory category) {
        return category.name().toLowerCase();
    }

    public static Sort sort() {
        load();
        return sort;
    }

    public static void toggleSort() {
        load();
        sort = sort == Sort.AZ ? Sort.CATEGORY : Sort.AZ;
        save();
    }

    public static EnabledFilter enabledFilter() {
        load();
        return enabledFilter;
    }

    public static void cycleFilter() {
        load();
        enabledFilter = enabledFilter.next();
        save();
    }

    private static void readSet(JsonObject root, String name, Set<String> into) {
        if (!root.has(name) || !root.get(name).isJsonArray()) {
            return;
        }
        for (var el : root.getAsJsonArray(name)) {
            into.add(el.getAsString());
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.add("favorites", toArray(FAVORITES));
        root.add("collapsed", toArray(COLLAPSED));
        root.addProperty("sort", sort == Sort.AZ ? "az" : "category");
        root.addProperty("filter", switch (enabledFilter) {
            case ON -> "on";
            case OFF -> "off";
            case ANY -> "any";
        });
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static JsonArray toArray(Set<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("features").resolve("menu.json");
    }
}
