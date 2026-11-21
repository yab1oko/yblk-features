package yblk.features.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import yblk.features.restrict.Restrictions;
import yblk.features.server.ServerTweaks;

import java.util.function.Supplier;

/** One moderation or kit action. Appended after the Server Tweaks snapshot. */
public class ServerActionPacket {
    private final String line;

    public ServerActionPacket(String line) {
        this.line = line == null ? "" : line;
    }

    public static void encode(ServerActionPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.line, 2048);
    }

    public static ServerActionPacket decode(FriendlyByteBuf buf) {
        return new ServerActionPacket(buf.readUtf(2048));
    }

    public static void handle(ServerActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
                return;
            }
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.server == null) {
                return;
            }
            String line = packet.line.trim();
            if (line.startsWith("kit-set ") || line.startsWith("kit-del ")) {
                if (Restrictions.canEdit(sender)) {
                    String[] all = line.split(" ");
                    if (line.startsWith("kit-del ") && all.length >= 2) {
                        ServerTweaks.deleteKit(all[1]);
                    } else if (all.length >= 4) {
                        ServerTweaks.saveKitItems(all[1], all[2], all, 3);
                    }
                }
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), ServerTweaksPacket.current(false));
                return;
            }
            if (line.equals("accept")) {
                ServerTweaks.acceptRules(sender);
                return;
            }
            boolean op = Restrictions.canEdit(sender);
            if (!op && !ServerTweaks.moderator(sender)) {
                return;
            }
            String[] parts = line.split(" ", 5);
            String action = parts[0];
            if (action.equals("kit") && parts.length >= 2) {
                ServerTweaks.giveKit(sender, parts[1]);
            } else if (action.equals("kit-save") && op && parts.length >= 2) {
                ServerTweaks.saveKit(sender, parts[1], parts.length >= 3 ? parts[2] : "");
            } else if (action.equals("mute") && (op || ServerTweaks.allows(sender, "mute"))) {
                String[] mute = line.split(" ", 4);
                if (mute.length >= 3) {
                    ServerPlayer target = sender.server.getPlayerList().getPlayerByName(mute[1]);
                    int minutes = 5;
                    try {
                        minutes = Integer.parseInt(mute[2]);
                    } catch (NumberFormatException ignored) {
                        minutes = 5;
                    }
                    if (target != null) {
                        ServerTweaks.mute(target, minutes, mute.length >= 4 ? mute[3] : "");
                    }
                }
            } else if (action.equals("unmute") && (op || ServerTweaks.allows(sender, "mute")) && parts.length >= 2) {
                ServerTweaks.unmute(parts[1], sender.server);
            } else if (action.equals("kick") && (op || ServerTweaks.allows(sender, "kick")) && parts.length >= 2) {
                ServerPlayer target = sender.server.getPlayerList().getPlayerByName(parts[1]);
                if (target != null) {
                    ServerTweaks.kick(target, parts.length >= 3 ? line.substring(line.indexOf(parts[2])) : "");
                }
            } else if (action.equals("ban") && op) {
                String[] ban = line.split(" ", 4);
                if (ban.length >= 2) {
                    ServerPlayer target = sender.server.getPlayerList().getPlayerByName(ban[1]);
                    int minutes = 0;
                    String why = "";
                    if (ban.length >= 3) {
                        try {
                            minutes = Integer.parseInt(ban[2]);
                            why = ban.length >= 4 ? ban[3] : "";
                        } catch (NumberFormatException ignored) {
                            why = line.substring(line.indexOf(ban[2]));
                        }
                    }
                    if (target != null) {
                        ServerTweaks.ban(target, why, minutes);
                    }
                }
            } else if (action.equals("freeze") && (op || ServerTweaks.allows(sender, "freeze")) && parts.length >= 2) {
                ServerPlayer target = sender.server.getPlayerList().getPlayerByName(parts[1]);
                if (target != null) {
                    ServerTweaks.toggleFreeze(target);
                }
            } else if (action.equals("role") && op && parts.length >= 3) {
                ServerPlayer target = sender.server.getPlayerList().getPlayerByName(parts[1]);
                if (target != null) {
                    ServerTweaks.setRole(target, parts[2]);
                }
            } else if (action.equals("perm") && op && parts.length >= 3) {
                ServerPlayer target = sender.server.getPlayerList().getPlayerByName(parts[1]);
                if (target != null) {
                    ServerTweaks.togglePerm(target, parts[2]);
                }
            } else if (action.equals("inv") && (op || ServerTweaks.allows(sender, "inventory")) && parts.length >= 2) {
                ServerTweaks.view(sender.server, parts[1]);
            } else if (action.equals("set") && (op || ServerTweaks.allows(sender, "inventory")) && parts.length >= 5) {
                int count = 1;
                try {
                    count = Integer.parseInt(parts[4]);
                } catch (NumberFormatException ignored) {
                    count = 1;
                }
                ServerTweaks.setStack(sender.server, parts[1], parts[2], parts[3], count);
            } else if (action.equals("tp") && (op || ServerTweaks.allows(sender, "teleport")) && parts.length >= 2) {
                ServerTweaks.teleportTo(sender, parts[1]);
            } else if (action.equals("vanish") && (op || ServerTweaks.allows(sender, "vanish"))) {
                ServerTweaks.vanish(sender);
            }
            ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), ServerTweaksPacket.current(false));
        });
        context.setPacketHandled(true);
    }
}
