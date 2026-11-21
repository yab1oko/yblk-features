package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.config.FeaturesConfig;
import yblk.features.restrict.Restrictions;
import yblk.features.server.ServerTweaks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server Tweaks settings. Appended after item weight so older ids stay put. */
public class ServerTweaksPacket {
    private final boolean enabled;
    private final boolean openRules;
    private final String motd;
    private final String join;
    private final String leave;
    private final String welcome;
    private final String welcomeSub;
    private final List<String> rules;
    private final String header;
    private final String footer;
    private final String starter;
    private final String checkAction;
    private final boolean fly;
    private final boolean speed;
    private final boolean reach;
    private final boolean fall;
    private final boolean fastBreak;
    private final boolean xray;
    private final boolean dupe;
    private final List<String> kits;
    private final List<String> roles;
    private final List<String> mutes;
    private final List<String> log;
    private final List<String> inventory;
    private final boolean hidePing;
    private final boolean sortTab;
    private final boolean rolePrefix;
    private final String checkActions;

    public ServerTweaksPacket(boolean enabled, boolean openRules, String motd, String join, String leave, String welcome,
                              String welcomeSub, List<String> rules, String header, String footer, String starter,
                              String checkAction, boolean fly, boolean speed, boolean reach, boolean fall, boolean fastBreak,
                              boolean xray, boolean dupe, List<String> kits, List<String> roles, List<String> mutes,
                              List<String> log, List<String> inventory, boolean hidePing, boolean sortTab, boolean rolePrefix,
                              String checkActions) {
        this.enabled = enabled;
        this.openRules = openRules;
        this.motd = motd;
        this.join = join;
        this.leave = leave;
        this.welcome = welcome;
        this.welcomeSub = welcomeSub;
        this.rules = rules;
        this.header = header;
        this.footer = footer;
        this.starter = starter;
        this.checkAction = checkAction;
        this.fly = fly;
        this.speed = speed;
        this.reach = reach;
        this.fall = fall;
        this.fastBreak = fastBreak;
        this.xray = xray;
        this.dupe = dupe;
        this.kits = kits;
        this.roles = roles;
        this.mutes = mutes;
        this.log = log;
        this.inventory = inventory;
        this.hidePing = hidePing;
        this.sortTab = sortTab;
        this.rolePrefix = rolePrefix;
        this.checkActions = checkActions == null ? "" : checkActions;
    }

    public static ServerTweaksPacket current(boolean openRules) {
        return new ServerTweaksPacket(FeaturesConfig.serverTweaksEnabled, openRules, FeaturesConfig.serverMotd,
                FeaturesConfig.serverJoin, FeaturesConfig.serverLeave, FeaturesConfig.serverWelcome, FeaturesConfig.serverWelcomeSub,
                new ArrayList<>(FeaturesConfig.serverRules), FeaturesConfig.serverTabHeader, FeaturesConfig.serverTabFooter,
                FeaturesConfig.serverStarterKit, FeaturesConfig.serverCheckAction, FeaturesConfig.serverCheckFly,
                FeaturesConfig.serverCheckSpeed, FeaturesConfig.serverCheckReach,                 FeaturesConfig.serverCheckFall,
                FeaturesConfig.serverCheckBreak, FeaturesConfig.serverCheckXray, FeaturesConfig.serverCheckDupe,
                new ArrayList<>(FeaturesConfig.serverKits), new ArrayList<>(FeaturesConfig.serverRoles),
                new ArrayList<>(FeaturesConfig.serverMutes), new ArrayList<>(FeaturesConfig.serverLog),
                new ArrayList<>(FeaturesConfig.serverInvView), FeaturesConfig.serverHidePing,
                FeaturesConfig.serverSortTab, FeaturesConfig.serverRolePrefix, FeaturesConfig.joinCheckActions());
    }

    public static void encode(ServerTweaksPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enabled);
        buf.writeBoolean(packet.openRules);
        buf.writeUtf(packet.motd);
        buf.writeUtf(packet.join);
        buf.writeUtf(packet.leave);
        buf.writeUtf(packet.welcome);
        buf.writeUtf(packet.welcomeSub);
        CommandBlockerPacket.writeList(buf, packet.rules);
        buf.writeUtf(packet.header);
        buf.writeUtf(packet.footer);
        buf.writeUtf(packet.starter);
        buf.writeUtf(packet.checkAction);
        buf.writeBoolean(packet.fly);
        buf.writeBoolean(packet.speed);
        buf.writeBoolean(packet.reach);
        buf.writeBoolean(packet.fall);
        buf.writeBoolean(packet.fastBreak);
        buf.writeBoolean(packet.xray);
        buf.writeBoolean(packet.dupe);
        CommandBlockerPacket.writeList(buf, packet.kits);
        CommandBlockerPacket.writeList(buf, packet.roles);
        CommandBlockerPacket.writeList(buf, packet.mutes);
        CommandBlockerPacket.writeList(buf, packet.log);
        CommandBlockerPacket.writeList(buf, packet.inventory);
        buf.writeBoolean(packet.hidePing);
        buf.writeBoolean(packet.sortTab);
        buf.writeBoolean(packet.rolePrefix);
        buf.writeUtf(packet.checkActions);
    }

    public static ServerTweaksPacket decode(FriendlyByteBuf buf) {
        return new ServerTweaksPacket(buf.readBoolean(), buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                buf.readUtf(), buf.readUtf(), CommandBlockerPacket.readList(buf), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                buf.readUtf(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(),
                CommandBlockerPacket.readList(buf), CommandBlockerPacket.readList(buf), CommandBlockerPacket.readList(buf),
                CommandBlockerPacket.readList(buf), CommandBlockerPacket.readList(buf),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf());
    }

    public static void handle(ServerTweaksPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender == null || !Restrictions.canEdit(sender)) {
                    return;
                }
                FeaturesConfig.applyServerTweaks(packet.enabled, packet.motd, packet.join, packet.leave, packet.welcome,
                        packet.welcomeSub, packet.rules, packet.header, packet.footer, packet.starter, packet.checkAction,
                        packet.fly, packet.speed, packet.reach, packet.fall, packet.fastBreak, packet.xray, packet.dupe,
                        packet.kits, packet.roles,
                        packet.mutes, FeaturesConfig.serverSeen, packet.log, packet.hidePing, packet.sortTab,
                        packet.rolePrefix, packet.checkActions, true);
                ServerTweaks.applyMotd(sender.server);
                ServerTweaks.refreshTabNames(sender.server);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), current(false));
            } else {
                FeaturesConfig.applyServerTweaks(packet.enabled, packet.motd, packet.join, packet.leave, packet.welcome,
                        packet.welcomeSub, packet.rules, packet.header, packet.footer, packet.starter, packet.checkAction,
                        packet.fly, packet.speed, packet.reach, packet.fall, packet.fastBreak, packet.xray, packet.dupe,
                        packet.kits, packet.roles,
                        packet.mutes, FeaturesConfig.serverSeen, packet.log, packet.hidePing, packet.sortTab,
                        packet.rolePrefix, packet.checkActions, false);
                FeaturesConfig.serverInvView = new ArrayList<>(packet.inventory);
                FeaturesConfig.serverTweaksPromptRules = packet.openRules && !packet.rules.isEmpty();
            }
        });
        context.setPacketHandled(true);
    }
}
