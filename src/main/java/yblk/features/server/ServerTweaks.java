package yblk.features.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import yblk.features.config.FeaturesConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server list text, tab header, kits, roles, and moderation. */
public final class ServerTweaks {
    private static final Map<UUID, Vec3> FROZEN = new HashMap<>();
    private static final Map<UUID, Vec3> LAST = new HashMap<>();
    private static final Map<UUID, Integer> BREAKS = new HashMap<>();
    private static final Map<UUID, Long> KIT_AT = new HashMap<>();
    private static final Map<UUID, int[]> ORE = new HashMap<>();
    private static final Map<UUID, Integer> COUNTS = new HashMap<>();
    private static final Map<String, Long> FLAG_AT = new HashMap<>();

    private ServerTweaks() {
    }

    public static void onLogin(ServerPlayer player) {
        if (!FeaturesConfig.serverTweaksEnabled || player.server == null) {
            return;
        }
        applyMotd(player.server);
        sendTab(player);
        say(player.server, FeaturesConfig.serverJoin, player.getGameProfile().getName());
        welcome(player);
        String id = player.getUUID().toString();
        if (!FeaturesConfig.serverSeen.contains(id)) {
            List<String> seen = new ArrayList<>(FeaturesConfig.serverSeen);
            seen.add(id);
            FeaturesConfig.serverSeen = seen;
            giveKit(player, FeaturesConfig.serverStarterKit);
            saveLists();
        }
        if (FROZEN.containsKey(player.getUUID()) || frozen(player.getUUID())) {
            FROZEN.put(player.getUUID(), player.position());
        }
        rememberOperator(player);
        player.refreshTabListName();
    }

    public static void onLogout(ServerPlayer player) {
        if (!FeaturesConfig.serverTweaksEnabled || player.server == null) {
            return;
        }
        say(player.server, FeaturesConfig.serverLeave, player.getGameProfile().getName());
        LAST.remove(player.getUUID());
    }

    public static boolean firstRules(ServerPlayer player) {
        return FeaturesConfig.serverTweaksEnabled
                && !FeaturesConfig.serverRules.isEmpty()
                && !FeaturesConfig.serverSeen.contains("rules:" + player.getUUID());
    }

    public static void acceptRules(ServerPlayer player) {
        String key = "rules:" + player.getUUID();
        if (FeaturesConfig.serverSeen.contains(key)) {
            return;
        }
        List<String> seen = new ArrayList<>(FeaturesConfig.serverSeen);
        seen.add(key);
        FeaturesConfig.applyServerTweaks(FeaturesConfig.serverTweaksEnabled, FeaturesConfig.serverMotd, FeaturesConfig.serverJoin,
                FeaturesConfig.serverLeave, FeaturesConfig.serverWelcome, FeaturesConfig.serverWelcomeSub, FeaturesConfig.serverRules,
                FeaturesConfig.serverTabHeader, FeaturesConfig.serverTabFooter, FeaturesConfig.serverStarterKit, FeaturesConfig.serverCheckAction,
                FeaturesConfig.serverCheckFly, FeaturesConfig.serverCheckSpeed, FeaturesConfig.serverCheckReach, FeaturesConfig.serverCheckFall,
                FeaturesConfig.serverCheckBreak, FeaturesConfig.serverCheckXray, FeaturesConfig.serverCheckDupe,
                FeaturesConfig.serverKits, FeaturesConfig.serverRoles, FeaturesConfig.serverMutes, seen,
                FeaturesConfig.serverLog, FeaturesConfig.serverHidePing, FeaturesConfig.serverSortTab,
                FeaturesConfig.serverRolePrefix, FeaturesConfig.joinCheckActions(), true);
    }

    public static void tick(ServerPlayer player) {
        if (!FeaturesConfig.serverTweaksEnabled) {
            return;
        }
        if (player.tickCount % 40 == 0) {
            sendTab(player);
        }
        if (player.tickCount % 100 == 0) {
            player.refreshTabListName();
        }
        if (FeaturesConfig.serverCheckDupe && player.tickCount % 20 == 0 && player.containerMenu == player.inventoryMenu) {
            int total = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                total += player.getInventory().getItem(i).getCount();
            }
            Integer before = COUNTS.put(player.getUUID(), total);
            if (before != null && total - before > 256) {
                flag(player, "dupe");
            }
        }
        UUID id = player.getUUID();
        if (frozen(id)) {
            Vec3 at = FROZEN.computeIfAbsent(id, ignored -> player.position());
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(at.x, at.y, at.z);
            player.fallDistance = 0f;
        }
        Vec3 now = player.position();
        Vec3 prev = LAST.put(id, now);
        if (prev != null && !player.isCreative() && !player.isSpectator() && player.getVehicle() == null && !player.isFallFlying()) {
            double flat = Math.sqrt(square(now.x - prev.x) + square(now.z - prev.z));
            if (FeaturesConfig.serverCheckSpeed && flat > 2.4) {
                flag(player, "speed");
            }
            if (FeaturesConfig.serverCheckFly && !player.onGround() && !player.isInWater() && now.y - prev.y > 0.8 && player.tickCount % 10 == 0) {
                flag(player, "fly");
            }
        }
    }

    public static boolean muted(ServerPlayer player) {
        if (!FeaturesConfig.serverTweaksEnabled) {
            return false;
        }
        long now = System.currentTimeMillis();
        for (String line : FeaturesConfig.serverMutes) {
            String[] parts = line.split(" ", 3);
            if (parts.length < 2 || !parts[0].equals(player.getUUID().toString())) {
                continue;
            }
            try {
                if (Long.parseLong(parts[1]) > now) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    public static void mute(ServerPlayer target, int minutes, String reason) {
        long until = System.currentTimeMillis() + Math.max(1, minutes) * 60_000L;
        List<String> next = new ArrayList<>();
        for (String line : FeaturesConfig.serverMutes) {
            if (!line.startsWith(target.getUUID().toString())) {
                next.add(line);
            }
        }
        String why = reason == null || reason.isBlank() ? "muted" : reason.trim();
        next.add(target.getUUID() + " " + until + " " + why);
        FeaturesConfig.serverMutes = next;
        note(target.getGameProfile().getName() + " muted " + minutes + "m");
        saveLists();
    }

    public static void unmute(String name, MinecraftServer server) {
        ServerPlayer target = server.getPlayerList().getPlayerByName(name);
        if (target == null) {
            return;
        }
        List<String> next = new ArrayList<>();
        for (String line : FeaturesConfig.serverMutes) {
            if (!line.startsWith(target.getUUID().toString())) {
                next.add(line);
            }
        }
        FeaturesConfig.serverMutes = next;
        note(name + " unmuted");
        saveLists();
    }

    public static void kick(ServerPlayer target, String reason) {
        target.connection.disconnect(Component.literal(reason == null || reason.isBlank() ? "Kicked" : reason));
        note(target.getGameProfile().getName() + " kicked");
    }

    public static void ban(ServerPlayer target, String reason, int minutes) {
        String why = reason == null || reason.isBlank() ? "Banned" : reason;
        java.util.Date expires = minutes <= 0 ? null : new java.util.Date(System.currentTimeMillis() + minutes * 60_000L);
        target.server.getPlayerList().getBans().add(new UserBanListEntry(target.getGameProfile(), new java.util.Date(), "Features", expires, why));
        target.connection.disconnect(Component.literal(why));
        note(target.getGameProfile().getName() + " banned");
    }

    public static void toggleFreeze(ServerPlayer target) {
        UUID id = target.getUUID();
        List<String> next = new ArrayList<>(FeaturesConfig.serverRoles);
        String key = "freeze:" + id;
        if (frozen(id)) {
            next.remove(key);
            FROZEN.remove(id);
            note(target.getGameProfile().getName() + " unfrozen");
        } else {
            next.add(key);
            FROZEN.put(id, target.position());
            note(target.getGameProfile().getName() + " frozen");
        }
        FeaturesConfig.serverRoles = next;
        saveLists();
    }

    public static void setRole(ServerPlayer target, String role) {
        UUID id = target.getUUID();
        List<String> next = new ArrayList<>();
        for (String line : FeaturesConfig.serverRoles) {
            if (!line.startsWith(id.toString())) {
                next.add(line);
            }
        }
        if ("operator".equals(role)) {
            target.server.getPlayerList().op(target.getGameProfile());
            next.add(id + " operator");
        } else if ("moderator".equals(role)) {
            target.server.getPlayerList().deop(target.getGameProfile());
            next.add(id + " moderator mute kick freeze inventory teleport vanish");
        } else {
            target.server.getPlayerList().deop(target.getGameProfile());
        }
        FeaturesConfig.serverRoles = next;
        note(target.getGameProfile().getName() + " role " + role);
        saveLists();
        refreshTabNames(target.server);
    }

    public static boolean moderator(ServerPlayer player) {
        String prefix = player.getUUID() + " moderator";
        for (String line : FeaturesConfig.serverRoles) {
            if (line.equals(prefix) || line.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    public static void saveKit(ServerPlayer player, String name) {
        saveKit(player, name, "");
    }

    public static void saveKit(ServerPlayer player, String name, String role) {
        if (name == null || name.isBlank()) {
            return;
        }
        StringBuilder line = new StringBuilder(name.trim().toLowerCase());
        int added = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && added < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) {
                continue;
            }
            line.append(' ').append(id).append(' ').append(stack.getCount());
            added++;
        }
        if (role != null && !role.isBlank()) {
            line.append(" role:").append(role.trim().toLowerCase().replace(' ', '_'));
        }
        if (added == 0) {
            return;
        }
        List<String> kits = new ArrayList<>();
        for (String kit : FeaturesConfig.serverKits) {
            if (!kit.startsWith(name.trim().toLowerCase() + " ")) {
                kits.add(kit);
            }
        }
        kits.add(line.toString());
        FeaturesConfig.serverKits = kits;
        note("kit " + name);
        saveLists();
    }

    public static void saveKitItems(String name, String role, String[] parts, int index) {
        if (name == null || name.isBlank() || parts == null) {
            return;
        }
        StringBuilder line = new StringBuilder(name.trim().toLowerCase());
        int added = 0;
        for (int i = index; i + 1 < parts.length && added < 9; i += 2) {
            if (parts[i].startsWith("role:")) {
                break;
            }
            ResourceLocation id = ResourceLocation.tryParse(parts[i]);
            if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
                continue;
            }
            int count = 1;
            try {
                count = Integer.parseInt(parts[i + 1]);
            } catch (NumberFormatException ignored) {
                count = 1;
            }
            line.append(' ').append(id).append(' ').append(Math.max(1, Math.min(64, count)));
            added++;
        }
        if (added == 0) {
            return;
        }
        if (role != null && !role.isBlank() && !"-".equals(role)) {
            line.append(" role:").append(role.trim().toLowerCase().replace(' ', '_'));
        }
        List<String> kits = new ArrayList<>();
        String prefix = name.trim().toLowerCase() + " ";
        for (String kit : FeaturesConfig.serverKits) {
            if (!kit.startsWith(prefix) && !kit.equals(name.trim().toLowerCase())) {
                kits.add(kit);
            }
        }
        kits.add(line.toString());
        FeaturesConfig.serverKits = kits;
        note("kit " + name);
        saveLists();
    }

    public static void deleteKit(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String prefix = name.trim().toLowerCase() + " ";
        List<String> kits = new ArrayList<>();
        for (String kit : FeaturesConfig.serverKits) {
            if (!kit.startsWith(prefix) && !kit.equals(name.trim().toLowerCase())) {
                kits.add(kit);
            }
        }
        FeaturesConfig.serverKits = kits;
        saveLists();
    }

    public static void giveKit(ServerPlayer player, String name) {
        if (name == null || name.isBlank() || player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!player.hasPermissions(2)) {
            Long last = KIT_AT.get(player.getUUID());
            if (last != null && now - last < 30_000L) {
                return;
            }
        }
        String prefix = name.trim().toLowerCase() + " ";
        for (String kit : FeaturesConfig.serverKits) {
            if (!kit.startsWith(prefix)) {
                continue;
            }
            String[] parts = kit.split(" ");
            String required = null;
            for (String part : parts) {
                if (part.startsWith("role:")) {
                    required = part.substring(5);
                }
            }
            if (required != null && !player.hasPermissions(2) && !required.equals(roleOf(player))) {
                return;
            }
            for (int i = 1; i + 1 < parts.length; i += 2) {
                if (parts[i].startsWith("role:")) {
                    i -= 1;
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(parts[i]);
                if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
                    continue;
                }
                int count = 1;
                try {
                    count = Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException ignored) {
                    count = 1;
                }
                ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id), Math.max(1, Math.min(64, count)));
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
            KIT_AT.put(player.getUUID(), now);
            return;
        }
    }

    public static boolean allows(ServerPlayer player, String perm) {
        if (player != null && player.hasPermissions(2)) {
            return true;
        }
        if (player == null) {
            return false;
        }
        String prefix = player.getUUID() + " moderator";
        for (String line : FeaturesConfig.serverRoles) {
            if (line.startsWith(prefix) && line.contains(" " + perm)) {
                return true;
            }
        }
        return false;
    }

    public static void view(MinecraftServer server, String name) {
        List<String> lines = new ArrayList<>();
        if (server == null || name == null || name.isBlank()) {
            FeaturesConfig.serverInvView = lines;
            return;
        }
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            lines.add(online.getGameProfile().getName());
            for (int i = 0; i < online.getInventory().getContainerSize(); i++) {
                addLine(lines, slotLabel(i), online.getInventory().getItem(i));
            }
            for (int i = 0; i < online.getEnderChestInventory().getContainerSize(); i++) {
                addLine(lines, "ender " + i, online.getEnderChestInventory().getItem(i));
            }
            curios(online, lines);
        } else {
            lines.add(name);
            readOffline(server, name, lines);
        }
        FeaturesConfig.serverInvView = lines;
    }

    public static void setStack(MinecraftServer server, String name, String slot, String item, int count) {
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayerByName(name);
        if (player == null || slot == null) {
            return;
        }
        ItemStack stack = stackOf(item, count);
        if (slot.startsWith("ender")) {
            int index = number(slot.substring(5).trim());
            if (index >= 0 && index < player.getEnderChestInventory().getContainerSize()) {
                player.getEnderChestInventory().setItem(index, stack);
            }
        } else if ("offhand".equals(slot)) {
            player.getInventory().setItem(40, stack);
        } else if (slot.startsWith("armor")) {
            int index = 36 + number(slot.substring(5).trim());
            if (index >= 36 && index <= 39) {
                player.getInventory().setItem(index, stack);
            }
        } else {
            int index = number(slot);
            if (index >= 0 && index < player.getInventory().getContainerSize()) {
                player.getInventory().setItem(index, stack);
            }
        }
        view(server, name);
        note(name + " inventory");
    }

    public static void teleportTo(ServerPlayer actor, String name) {
        ServerPlayer target = actor.server.getPlayerList().getPlayerByName(name);
        if (target == null) {
            return;
        }
        actor.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
    }

    public static void vanish(ServerPlayer player) {
        player.setInvisible(!player.isInvisible());
        note(player.getGameProfile().getName() + (player.isInvisible() ? " vanished" : " visible"));
    }

    public static void onBreak(ServerPlayer player, net.minecraft.world.level.block.state.BlockState state) {
        if (!FeaturesConfig.serverTweaksEnabled || player == null) {
            return;
        }
        if (FeaturesConfig.serverCheckBreak) {
            int now = player.tickCount;
            Integer last = BREAKS.put(player.getUUID(), now);
            if (last != null && now - last < 2) {
                flag(player, "fast-break");
            }
        }
        if (!FeaturesConfig.serverCheckXray || state == null) {
            return;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return;
        }
        String path = id.getPath();
        boolean ore = path.endsWith("_ore") || path.contains("_ore");
        boolean rock = path.equals("stone") || path.equals("deepslate") || path.equals("netherrack") || path.equals("tuff");
        if (!ore && !rock) {
            return;
        }
        int[] counts = ORE.computeIfAbsent(player.getUUID(), ignored -> new int[2]);
        if (ore) {
            counts[0]++;
        } else {
            counts[1]++;
        }
        int mined = counts[0] + counts[1];
        if (counts[0] >= 24 && mined >= 40 && counts[0] / (float) mined > 0.45f) {
            flag(player, "xray");
            counts[0] = 0;
            counts[1] = 0;
        }
    }

    public static void onReach(ServerPlayer player, double distance) {
        if (FeaturesConfig.serverTweaksEnabled && FeaturesConfig.serverCheckReach && distance > 7.5) {
            flag(player, "reach");
        }
    }

    public static void applyMotd(MinecraftServer server) {
        if (server != null && FeaturesConfig.serverMotd != null && !FeaturesConfig.serverMotd.isBlank()) {
            server.setMotd(FeaturesConfig.serverMotd);
            server.invalidateStatus();
        }
    }

    public static void refreshTabNames(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.refreshTabListName();
        }
    }

    private static void rememberOperator(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            return;
        }
        String prefix = player.getUUID() + " ";
        for (String line : FeaturesConfig.serverRoles) {
            if (line.startsWith(prefix)) {
                return;
            }
        }
        List<String> roles = new ArrayList<>(FeaturesConfig.serverRoles);
        roles.add(player.getUUID() + " operator");
        FeaturesConfig.serverRoles = roles;
        saveLists();
    }

    public static String roleName(ServerPlayer player) {
        return roleOf(player);
    }

    public static void notePublic(String line) {
        note(line);
        saveLists();
    }

    public static String fill(String raw, ServerPlayer player) {
        if (raw == null) {
            return "";
        }
        MinecraftServer server = player.server;
        String tps = "20";
        if (server != null) {
            double tick = Math.max(1.0, server.getAverageTickTime());
            tps = String.format(java.util.Locale.ROOT, "%.1f", Math.min(20.0, 1000.0 / tick));
        }
        return raw.replace("{online}", Integer.toString(server == null ? 1 : server.getPlayerCount()))
                .replace("{max}", Integer.toString(server == null ? 20 : server.getMaxPlayers()))
                .replace("{tps}", tps)
                .replace("{player}", player.getGameProfile().getName())
                .replace("{world}", player.level().dimension().location().toString())
                .replace('&', '\u00A7');
    }

    private static void sendTab(ServerPlayer player) {
        if (FeaturesConfig.serverTabHeader.isBlank() && FeaturesConfig.serverTabFooter.isBlank()) {
            return;
        }
        player.connection.send(new ClientboundTabListPacket(
                Component.literal(fill(FeaturesConfig.serverTabHeader, player)),
                Component.literal(fill(FeaturesConfig.serverTabFooter, player))));
    }

    private static void welcome(ServerPlayer player) {
        if (FeaturesConfig.serverWelcome.isBlank() && FeaturesConfig.serverWelcomeSub.isBlank()) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 16));
        if (!FeaturesConfig.serverWelcomeSub.isBlank()) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(fill(FeaturesConfig.serverWelcomeSub, player))));
        }
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(fill(FeaturesConfig.serverWelcome, player))));
    }

    private static void say(MinecraftServer server, String raw, String name) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal(raw.replace("{player}", name).replace('&', '\u00A7')), false);
    }

    private static boolean frozen(UUID id) {
        return FeaturesConfig.serverRoles.contains("freeze:" + id);
    }

    public static void flag(ServerPlayer player, String check) {
        String key = player.getUUID() + " " + check;
        long now = System.currentTimeMillis();
        Long last = FLAG_AT.get(key);
        if (last != null && now - last < 10_000L) {
            return;
        }
        FLAG_AT.put(key, now);
        note(player.getGameProfile().getName() + " " + check);
        saveLists();
        String action = FeaturesConfig.checkActionFor(check);
        if (!"log".equals(action) && player.server != null) {
            net.minecraft.network.chat.Component message = Component.translatable(
                    "message." + yblk.features.Features.MODID + ".check",
                    player.getGameProfile().getName(), check);
            for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                if (other.hasPermissions(2)) {
                    other.sendSystemMessage(message);
                }
            }
        }
        if (player.server != null) {
            yblk.features.network.ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                    yblk.features.network.ServerTweaksPacket.current(false));
        }
        if ("kick".equals(action)) {
            player.connection.disconnect(Component.literal(check));
        }
    }

    private static void note(String line) {
        List<String> log = new ArrayList<>(FeaturesConfig.serverLog);
        log.add(line);
        if (log.size() > 40) {
            log = new ArrayList<>(log.subList(log.size() - 40, log.size()));
        }
        FeaturesConfig.serverLog = log;
    }

    private static void saveLists() {
        FeaturesConfig.applyServerTweaks(FeaturesConfig.serverTweaksEnabled, FeaturesConfig.serverMotd, FeaturesConfig.serverJoin,
                FeaturesConfig.serverLeave, FeaturesConfig.serverWelcome, FeaturesConfig.serverWelcomeSub, FeaturesConfig.serverRules,
                FeaturesConfig.serverTabHeader, FeaturesConfig.serverTabFooter, FeaturesConfig.serverStarterKit, FeaturesConfig.serverCheckAction,
                FeaturesConfig.serverCheckFly, FeaturesConfig.serverCheckSpeed, FeaturesConfig.serverCheckReach, FeaturesConfig.serverCheckFall,
                FeaturesConfig.serverCheckBreak, FeaturesConfig.serverCheckXray, FeaturesConfig.serverCheckDupe,
                FeaturesConfig.serverKits, FeaturesConfig.serverRoles, FeaturesConfig.serverMutes,
                FeaturesConfig.serverSeen, FeaturesConfig.serverLog, FeaturesConfig.serverHidePing,
                FeaturesConfig.serverSortTab, FeaturesConfig.serverRolePrefix, FeaturesConfig.joinCheckActions(), true);
    }

    private static double square(double value) {
        return value * value;
    }

    private static String roleOf(ServerPlayer player) {
        if (player.hasPermissions(2)) {
            return "operator";
        }
        String prefix = player.getUUID() + " ";
        for (String line : FeaturesConfig.serverRoles) {
            if (line.startsWith(prefix)) {
                String[] parts = line.split(" ");
                return parts.length > 1 ? parts[1] : "player";
            }
        }
        return "player";
    }

    public static void togglePerm(ServerPlayer target, String perm) {
        if (target == null || perm == null || !"moderator".equals(roleOf(target))) {
            return;
        }
        String prefix = target.getUUID() + " moderator";
        List<String> next = new ArrayList<>();
        for (String line : FeaturesConfig.serverRoles) {
            if (!line.startsWith(prefix)) {
                next.add(line);
                continue;
            }
            if (line.contains(" " + perm)) {
                next.add(line.replace(" " + perm, ""));
            } else {
                next.add(line + " " + perm);
            }
        }
        FeaturesConfig.serverRoles = next;
        saveLists();
    }

    private static void addLine(List<String> lines, String slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) {
            lines.add(slot + " " + id + " " + stack.getCount());
        }
    }

    private static String slotLabel(int slot) {
        if (slot >= 36 && slot <= 39) {
            return "armor " + (slot - 36);
        }
        if (slot == 40) {
            return "offhand";
        }
        return Integer.toString(slot);
    }

    private static ItemStack stackOf(String item, int count) {
        if (count <= 0 || item == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(item.trim());
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ForgeRegistries.ITEMS.getValue(id), Math.min(64, count));
    }

    private static int number(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void readOffline(MinecraftServer server, String name, List<String> lines) {
        try {
            if (server.getProfileCache() == null) {
                lines.add("offline");
                return;
            }
            var profile = server.getProfileCache().get(name);
            if (profile.isEmpty()) {
                lines.add("offline");
                return;
            }
            java.io.File file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(profile.get().getId() + ".dat").toFile();
            if (!file.isFile()) {
                lines.add("offline");
                return;
            }
            CompoundTag root = NbtIo.readCompressed(file);
            readTagList(lines, root.getList("Inventory", 10), false);
            readTagList(lines, root.getList("EnderItems", 10), true);
        } catch (Exception ignored) {
            lines.add("offline");
        }
    }

    private static void readTagList(List<String> lines, ListTag list, boolean ender) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int count = tag.getByte("Count");
            if (count <= 0) {
                continue;
            }
            String slot = ender ? "ender " + tag.getByte("Slot") : slotLabel(tag.getByte("Slot"));
            lines.add(slot + " " + tag.getString("id") + " " + count);
        }
    }

    private static void curios(ServerPlayer player, List<String> lines) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            return;
        }
        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object optInv = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            Object handler = optInv instanceof net.minecraftforge.common.util.LazyOptional<?> lazy ? lazy.orElse(null) : null;
            if (handler == null) {
                return;
            }
            Object curiosMap = handler.getClass().getMethod("getCurios").invoke(handler);
            if (!(curiosMap instanceof Map<?, ?> map)) {
                return;
            }
            for (var entry : map.entrySet()) {
                Object stacks = entry.getValue().getClass().getMethod("getStacks").invoke(entry.getValue());
                int slots = (Integer) stacks.getClass().getMethod("getSlots").invoke(stacks);
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) stacks.getClass().getMethod("getStackInSlot", int.class).invoke(stacks, i);
                    addLine(lines, "curios " + entry.getKey() + " " + i, stack);
                }
            }
        } catch (Throwable ignored) {
            lines.add("curios");
        }
    }
}
