package yblk.features.spawn;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MobSpawnCatalog {
    private MobSpawnCatalog() {
    }

    public static List<Entry> allEntries() {
        List<Entry> list = new ArrayList<>();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id == null) {
                continue;
            }
            if (type.getCategory() == MobCategory.MISC) {
                continue;
            }
            if (!type.canSummon()) {
                continue;
            }
            list.add(new Entry(id, type, MobAttitude.of(type, id)));
        }
        list.sort(Comparator.comparing(e -> e.id.toString()));
        return list;
    }

    public static List<Entry> filtered(MobTab tab) {
        List<Entry> all = allEntries();
        if (tab == MobTab.ALL) {
            return all;
        }
        List<Entry> filtered = new ArrayList<>();
        for (Entry entry : all) {
            if (tab.matches(entry.attitude)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public static boolean isBlocked(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return id != null && FeaturesConfig.isMobSpawnBlocked(id);
    }

    public static void setBlocked(ResourceLocation id, boolean blocked) {
        Set<String> next = new HashSet<>(FeaturesConfig.blockedMobIds);
        if (blocked) {
            next.add(id.toString());
        } else {
            next.remove(id.toString());
        }
        FeaturesConfig.setBlockedMobIds(next);
    }

    public static void setAllBlocked(List<Entry> entries, boolean blocked) {
        Set<String> next = new HashSet<>(FeaturesConfig.blockedMobIds);
        for (Entry entry : entries) {
            if (blocked) {
                next.add(entry.id.toString());
            } else {
                next.remove(entry.id.toString());
            }
        }
        FeaturesConfig.setBlockedMobIds(next);
    }

    public record Entry(ResourceLocation id, EntityType<?> type, MobAttitude attitude) {
        public boolean isBlocked() {
            return FeaturesConfig.isMobSpawnBlocked(id);
        }

        public boolean isAllowed() {
            return !isBlocked();
        }
    }

    public enum MobTab {
        ALL,
        NEUTRAL,
        PASSIVE,
        HOSTILE;

        public boolean matches(MobAttitude attitude) {
            return switch (this) {
                case ALL -> true;
                case NEUTRAL -> attitude == MobAttitude.NEUTRAL;
                case PASSIVE -> attitude == MobAttitude.PASSIVE;
                case HOSTILE -> attitude == MobAttitude.HOSTILE;
            };
        }

        public String langKey() {
            return "screen.features.mob_spawn.tab." + name().toLowerCase();
        }
    }
}
