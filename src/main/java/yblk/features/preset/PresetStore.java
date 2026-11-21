package yblk.features.preset;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Enabled modules and every config value, as JSON under config/features/presets/. */
public final class PresetStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PresetStore() {
    }

    public static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve(Features.MODID).resolve("presets");
    }

    public static List<String> names() {
        Path dir = directory();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> {
                        String name = path.getFileName().toString();
                        return name.substring(0, name.length() - 5);
                    })
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public static String snapshotJson() {
        JsonObject root = new JsonObject();
        Object values = FeaturesConfig.SPEC.getValues();
        if (values instanceof UnmodifiableConfig config) {
            writeConfig(config, root);
        }
        return GSON.toJson(root);
    }

    public static boolean save(String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) {
            return false;
        }
        try {
            Files.createDirectories(directory());
            Files.writeString(directory().resolve(name + ".json"), snapshotJson(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String read(String rawName) {
        String name = sanitize(rawName);
        Path path = directory().resolve(name + ".json");
        if (!Files.isRegularFile(path)) {
            return "";
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }

    public static boolean delete(String rawName) {
        String name = sanitize(rawName);
        try {
            return Files.deleteIfExists(directory().resolve(name + ".json"));
        } catch (IOException e) {
            return false;
        }
    }

    /** Writes the snapshot into the live config and the toml on disk. */
    public static boolean apply(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return false;
        }
        Object values = FeaturesConfig.SPEC.getValues();
        if (!(values instanceof Config live)) {
            return false;
        }
        applyInto(live, root, new ArrayList<>());
        FeaturesConfig.SPEC.afterReload();
        FeaturesConfig.reloadFromSpec();
        try {
            FeaturesConfig.SPEC.save();
        } catch (RuntimeException ignored) {
        }
        return true;
    }

    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < trimmed.length() && out.length() < 32; i++) {
            char c = trimmed.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                out.append(c);
            } else if (c == ' ' && out.length() > 0 && out.charAt(out.length() - 1) != '-') {
                out.append('-');
            }
        }
        return out.toString();
    }

    private static void writeConfig(UnmodifiableConfig config, JsonObject out) {
        for (Map.Entry<String, Object> entry : config.valueMap().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig child) {
                JsonObject nested = new JsonObject();
                writeConfig(child, nested);
                out.add(entry.getKey(), nested);
            } else if (value instanceof List<?> list) {
                JsonArray array = new JsonArray();
                for (Object item : list) {
                    array.add(String.valueOf(item));
                }
                out.add(entry.getKey(), array);
            } else if (value instanceof Boolean bool) {
                out.addProperty(entry.getKey(), bool);
            } else if (value instanceof Number number) {
                out.addProperty(entry.getKey(), number);
            } else if (value != null) {
                out.addProperty(entry.getKey(), String.valueOf(value));
            }
        }
    }

    private static void applyInto(Config live, JsonObject json, List<String> path) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            List<String> next = new ArrayList<>(path);
            next.add(entry.getKey());
            JsonElement element = entry.getValue();
            if (element.isJsonObject()) {
                Object child = live.get(next);
                if (child instanceof Config nested) {
                    applyInto(nested, element.getAsJsonObject(), new ArrayList<>());
                }
            } else {
                live.set(next, coerce(live.get(next), element));
            }
        }
    }

    private static Object coerce(Object current, JsonElement element) {
        if (element.isJsonArray()) {
            List<String> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(item.getAsString());
            }
            return list;
        }
        if (current instanceof Boolean || element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        if (current instanceof Integer) {
            return element.getAsInt();
        }
        if (current instanceof Long) {
            return element.getAsLong();
        }
        if (current instanceof Double || current instanceof Float) {
            return element.getAsDouble();
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            double number = element.getAsDouble();
            if (number == Math.rint(number) && !Double.isInfinite(number)) {
                return (int) number;
            }
            return number;
        }
        return element.isJsonNull() ? "" : element.getAsString();
    }
}
