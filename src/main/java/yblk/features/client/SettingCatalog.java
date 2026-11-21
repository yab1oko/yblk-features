package yblk.features.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import yblk.features.Features;
import yblk.features.feature.Feature;
import yblk.features.feature.FeatureRegistry;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Setting labels from en_us and ru_ru, so search works in either language. */
public final class SettingCatalog {
    public record Hit(String featureId, String key, String label) {
    }

    private static List<Hit> hits = List.of();
    private static boolean loaded;

    private SettingCatalog() {
    }

    public static List<Hit> search(String query) {
        load();
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Hit> out = new ArrayList<>();
        for (Hit hit : hits) {
            Feature feature = find(hit.featureId);
            String title = feature == null ? hit.featureId : feature.getTitle().getString();
            if (MenuSearch.matches(query, hit.label, title)) {
                out.add(hit);
                if (out.size() >= 8) {
                    break;
                }
            }
        }
        return out;
    }

    public static String haystack(String featureId) {
        load();
        StringBuilder joined = new StringBuilder();
        for (Hit hit : hits) {
            if (hit.featureId.equals(featureId)) {
                joined.append(' ').append(hit.label);
            }
        }
        return joined.toString();
    }

    private static Feature find(String id) {
        for (Feature feature : FeatureRegistry.all()) {
            if (feature.getId().equals(id)) {
                return feature;
            }
        }
        return null;
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        List<Hit> parsed = new ArrayList<>();
        read(parsed, "en_us");
        read(parsed, "ru_ru");
        hits = parsed;
    }

    private static void read(List<Hit> into, String lang) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        ResourceLocation location = new ResourceLocation(Features.MODID, "lang/" + lang + ".json");
        minecraft.getResourceManager().getResource(location).ifPresent(resource -> {
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (String key : root.keySet()) {
                    String featureId = featureIdOf(key);
                    if (featureId == null) {
                        continue;
                    }
                    into.add(new Hit(featureId, key, root.get(key).getAsString()));
                }
            } catch (Exception ignored) {
            }
        });
    }

    /** screen.features.&lt;screen&gt;.&lt;setting&gt; → feature id. Titles without a setting suffix are skipped. */
    private static String featureIdOf(String key) {
        String prefix = "screen." + Features.MODID + ".";
        if (!key.startsWith(prefix)) {
            return null;
        }
        String rest = key.substring(prefix.length());
        int dot = rest.indexOf('.');
        if (dot <= 0 || dot == rest.length() - 1) {
            return null;
        }
        String screen = rest.substring(0, dot);
        return switch (screen) {
            case "mob_spawn" -> "mob_spawn_block";
            case "color_picker" -> null;
            case "inventory_revamp", "border_danger", "hud_hide", "f3_hide", "hide_names",
                    "real_first_person", "true_night", "local_chat", "wetness", "item_remover",
                    "portal_lock", "elytra_rules", "mob_drops", "villager_trades", "item_weight",
                    "server_tweaks" -> screen;
            default -> null;
        };
    }
}
