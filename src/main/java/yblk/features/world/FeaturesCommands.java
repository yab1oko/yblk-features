package yblk.features.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yblk.features.Features;
import yblk.features.config.FeaturesConfig;
import yblk.features.server.ServerTweaks;

public final class FeaturesCommands {
    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("features")
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.literal("portal")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(ctx -> unlock(ctx.getSource(), DimensionArgument.getDimension(ctx, "dimension")))))));
        dispatcher.register(Commands.literal("kit")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> giveKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        dispatcher.register(Commands.literal("mute")
                .requires(FeaturesCommands::canMute)
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 10080))
                                .executes(ctx -> mute(ctx.getSource(), StringArgumentType.getString(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "minutes"), "")))));
        dispatcher.register(Commands.literal("unmute")
                .requires(FeaturesCommands::canMute)
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> unmute(ctx.getSource(), StringArgumentType.getString(ctx, "player")))));
    }

    private static boolean canMute(CommandSourceStack stack) {
        return stack.hasPermission(2) || (stack.getEntity() instanceof ServerPlayer player && ServerTweaks.allows(player, "mute"));
    }

    private static int giveKit(CommandSourceStack source, String name) {
        if (!FeaturesConfig.serverTweaksEnabled || !(source.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        ServerTweaks.giveKit(player, name);
        source.sendSuccess(() -> Component.translatable("command." + Features.MODID + ".kit", name), false);
        return 1;
    }

    private static int mute(CommandSourceStack source, String name, int minutes, String reason) {
        if (source.getServer() == null) {
            return 0;
        }
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);
        if (target == null) {
            return 0;
        }
        ServerTweaks.mute(target, minutes, reason);
        source.sendSuccess(() -> Component.translatable("command." + Features.MODID + ".mute", name, minutes), true);
        return 1;
    }

    private static int unmute(CommandSourceStack source, String name) {
        if (source.getServer() == null) {
            return 0;
        }
        ServerTweaks.unmute(name, source.getServer());
        source.sendSuccess(() -> Component.translatable("command." + Features.MODID + ".unmute", name), true);
        return 1;
    }

    private static int unlock(CommandSourceStack source, ServerLevel level) {
        ResourceLocation id = level.dimension().location();
        FeaturesConfig.setPortalRules(PortalRules.unlock(FeaturesConfig.portalRules, id));
        WorldSync.afterConfigReload();
        source.sendSuccess(
                () -> Component.translatable("command." + Features.MODID + ".portal_unlock", PortalRules.label(id)),
                true);
        return 1;
    }
}
