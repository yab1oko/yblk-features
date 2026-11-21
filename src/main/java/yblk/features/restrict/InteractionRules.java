package yblk.features.restrict;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stored as {@code b:id}, {@code e:id}, {@code bt:tag}, or {@code et:tag}.
 * A suffix {@code |open,use,right,break} limits which actions are blocked. No suffix means all four.
 */
public final class InteractionRules {
    public enum Kind {
        BLOCK, ENTITY, BLOCK_TAG, ENTITY_TAG
    }

    public static final class Rule {
        public final Kind kind;
        public final ResourceLocation id;
        public final boolean open;
        public final boolean use;
        public final boolean right;
        public final boolean breakBlock;

        public Rule(Kind kind, ResourceLocation id, boolean open, boolean use, boolean right, boolean breakBlock) {
            this.kind = kind;
            this.id = id;
            this.open = open;
            this.use = use;
            this.right = right;
            this.breakBlock = breakBlock;
        }

        public String key() {
            return prefix() + id;
        }

        public String encode() {
            String base = key();
            if (open && use && right && breakBlock) {
                return base;
            }
            StringBuilder sb = new StringBuilder(base).append('|');
            if (open) {
                sb.append("open,");
            }
            if (use) {
                sb.append("use,");
            }
            if (right) {
                sb.append("right,");
            }
            if (breakBlock) {
                sb.append("break,");
            }
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.setLength(sb.length() - 1);
            }
            return sb.toString();
        }

        public Rule with(boolean open, boolean use, boolean right, boolean breakBlock) {
            return new Rule(kind, id, open, use, right, breakBlock);
        }

        public boolean matchesBlock(BlockState state) {
            if (kind == Kind.BLOCK) {
                ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                return id.equals(key);
            }
            if (kind == Kind.BLOCK_TAG) {
                return state.is(TagKey.create(Registries.BLOCK, id));
            }
            return false;
        }

        public boolean matchesEntity(Entity entity) {
            if (kind == Kind.ENTITY) {
                ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                return id.equals(key);
            }
            if (kind == Kind.ENTITY_TAG) {
                return entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, id));
            }
            return false;
        }

        private String prefix() {
            return switch (kind) {
                case BLOCK -> "b:";
                case ENTITY -> "e:";
                case BLOCK_TAG -> "bt:";
                case ENTITY_TAG -> "et:";
            };
        }
    }

    private InteractionRules() {
    }

    public static List<Rule> all() {
        List<Rule> out = new ArrayList<>();
        for (String raw : FeaturesConfig.interactionRules) {
            Rule rule = parse(raw);
            if (rule != null) {
                out.add(rule);
            }
        }
        return out;
    }

    public static Rule find(String key) {
        if (key == null) {
            return null;
        }
        for (Rule rule : all()) {
            if (rule.key().equals(key)) {
                return rule;
            }
        }
        return null;
    }

    public static Rule parse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        boolean open = true;
        boolean use = true;
        boolean right = true;
        boolean breakBlock = true;
        int bar = s.indexOf('|');
        if (bar >= 0) {
            String flags = s.substring(bar + 1);
            open = flags.contains("open");
            use = flags.contains("use");
            right = flags.contains("right");
            breakBlock = flags.contains("break");
            s = s.substring(0, bar);
        }
        Kind kind;
        String idPart;
        if (s.startsWith("bt:")) {
            kind = Kind.BLOCK_TAG;
            idPart = s.substring(3);
        } else if (s.startsWith("et:")) {
            kind = Kind.ENTITY_TAG;
            idPart = s.substring(3);
        } else if (s.startsWith("b:")) {
            kind = Kind.BLOCK;
            idPart = s.substring(2);
        } else if (s.startsWith("e:")) {
            kind = Kind.ENTITY;
            idPart = s.substring(2);
        } else {
            return null;
        }
        if (idPart.startsWith("#")) {
            idPart = idPart.substring(1);
        }
        if (!idPart.contains(":")) {
            idPart = "minecraft:" + idPart;
        }
        ResourceLocation id = ResourceLocation.tryParse(idPart);
        if (id == null || (!open && !use && !right && !breakBlock)) {
            return null;
        }
        return new Rule(kind, id, open, use, right, breakBlock);
    }

    public static Rule block(ResourceLocation id) {
        return new Rule(Kind.BLOCK, id, true, true, true, true);
    }

    public static Rule entity(ResourceLocation id) {
        return new Rule(Kind.ENTITY, id, true, true, true, true);
    }

    public static Rule blockTag(ResourceLocation id) {
        return new Rule(Kind.BLOCK_TAG, id, true, true, true, true);
    }

    public static Rule entityTag(ResourceLocation id) {
        return new Rule(Kind.ENTITY_TAG, id, true, true, true, true);
    }

    public static void put(Rule rule) {
        if (rule == null) {
            return;
        }
        List<String> next = new ArrayList<>();
        boolean replaced = false;
        for (String raw : FeaturesConfig.interactionRules) {
            Rule existing = parse(raw);
            if (existing != null && existing.key().equals(rule.key())) {
                if (!replaced) {
                    next.add(rule.encode());
                    replaced = true;
                }
            } else if (raw != null && !raw.isBlank()) {
                next.add(raw.trim());
            }
        }
        if (!replaced) {
            next.add(rule.encode());
        }
        FeaturesConfig.setInteractionRules(next);
    }

    public static void remove(String key) {
        List<String> next = new ArrayList<>();
        for (String raw : FeaturesConfig.interactionRules) {
            Rule existing = parse(raw);
            if (existing != null && existing.key().equals(key)) {
                continue;
            }
            if (raw != null && !raw.isBlank()) {
                next.add(raw.trim());
            }
        }
        FeaturesConfig.setInteractionRules(next);
    }

    public static boolean blocksBreak(BlockState state) {
        for (Rule rule : all()) {
            if (rule.breakBlock && rule.matchesBlock(state)) {
                return true;
            }
        }
        return false;
    }

    public static boolean blocksAttack(Entity entity) {
        for (Rule rule : all()) {
            if (rule.breakBlock && rule.matchesEntity(entity)) {
                return true;
            }
        }
        return false;
    }

    public static boolean blocksUse(BlockState state, boolean menu, boolean holding) {
        for (Rule rule : all()) {
            if (!rule.matchesBlock(state)) {
                continue;
            }
            if (rule.right || (rule.open && menu) || (rule.use && holding)) {
                return true;
            }
        }
        return false;
    }

    public static boolean blocksInteract(Entity entity, boolean menu, boolean holding) {
        for (Rule rule : all()) {
            if (!rule.matchesEntity(entity)) {
                continue;
            }
            if (rule.right || (rule.open && menu) || (rule.use && holding)) {
                return true;
            }
        }
        return false;
    }

    public static Block namedBlock(ResourceLocation id) {
        return id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
    }

    public static EntityType<?> namedEntity(ResourceLocation id) {
        return id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
    }
}
