package yblk.features.restrict;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import org.slf4j.Logger;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CommandBlocker {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ThreadLocal<ServerPlayer> SENDING = new ThreadLocal<>();

    private static final Field CHILDREN = field("children");
    private static final Field LITERALS = field("literals");
    private static final Field ARGUMENTS = field("arguments");
    private static boolean warned;

    private CommandBlocker() {
    }

    public static void onCommand(CommandEvent event) {
        if (!FeaturesConfig.commandBlockerEnabled) {
            return;
        }
        ParseResults<CommandSourceStack> parsed = event.getParseResults();
        if (parsed == null || parsed.getContext() == null) {
            return;
        }
        CommandSourceStack source = parsed.getContext().getSource();
        if (source == null || !(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String raw = parsed.getReader().getString();
        if (!CommandRules.blocked(player, raw)) {
            return;
        }
        event.setCanceled(true);
        RestrictionMessages.actionBar(player, denyText(shown(raw)));
    }

    public static void resend(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    public static void prune(RootCommandNode<?> root, ServerPlayer player) {
        if (root == null || player == null || !FeaturesConfig.commandBlockerEnabled || CHILDREN == null) {
            return;
        }
        try {
            walk(root, player, new ArrayList<>(), CommandRules.compile());
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                LOGGER.warn("[{}] Command tree prune failed: {}", Features.MODID, e.toString());
            }
        }
    }

    private static void walk(CommandNode<?> node, ServerPlayer player, List<String> path, List<CommandRules.Rule> rules) {
        List<CommandNode<?>> children = new ArrayList<>(node.getChildren());
        List<String> remove = new ArrayList<>();
        for (CommandNode<?> child : children) {
            if (!(child instanceof LiteralCommandNode<?> literal)) {
                continue;
            }
            List<String> next = new ArrayList<>(path);
            next.add(literal.getLiteral());
            if (CommandRules.hides(player, next, rules)) {
                remove.add(literal.getName());
            } else {
                walk(child, player, next, rules);
            }
        }
        for (String name : remove) {
            removeChild(node, name);
        }
    }

    @SuppressWarnings("unchecked")
    private static void removeChild(CommandNode<?> parent, String name) {
        try {
            ((Map<String, ?>) CHILDREN.get(parent)).remove(name);
            ((Map<String, ?>) LITERALS.get(parent)).remove(name);
            ((Map<String, ?>) ARGUMENTS.get(parent)).remove(name);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field field(String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[{}] Command node field '{}' is unavailable", Features.MODID, name);
            return null;
        }
    }

    private static String shown(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.startsWith("/")) {
            s = s.substring(1).trim();
        }
        if (s.length() > 80) {
            s = s.substring(0, 80);
        }
        return s;
    }

    private static Component denyText(String command) {
        String custom = FeaturesConfig.commandDenyMessage;
        if (custom == null || custom.isBlank()) {
            return Component.translatable("screen." + Features.MODID + ".command_blocker.denied", command);
        }
        return Component.literal(custom.replace("{command}", command));
    }
}
