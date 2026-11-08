package yblk.features.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.slf4j.Logger;
import yblk.features.Features;
import yblk.features.border.BorderBarrierVisual;
import yblk.features.config.FeaturesConfig;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public final class CustomBarrierTexture {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = new ResourceLocation(Features.MODID, "dynamic/custom_barrier");
    private static final String FILE_NAME = "custom_barrier.png";

    private static DynamicTexture texture;
    private static boolean loaded;
    private static long lastFileTime = -1L;
    private static boolean dropHooked;

    private CustomBarrierTexture() {
    }

    @Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(CustomBarrierTexture::hookFileDrop);
        }
    }

    @Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            hookFileDrop();
            if (hasFile()) {
                reload();
            }
        }
    }

    public static Path storageDir() {
        return FMLPaths.CONFIGDIR.get().resolve(Features.MODID).resolve("barrier");
    }

    public static Path storageFile() {
        return storageDir().resolve(FILE_NAME);
    }

    public static boolean hasFile() {
        return Files.isRegularFile(storageFile());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /** Register GLFW drop-files onto the game window (drag from Desktop into Minecraft). */
    public static void hookFileDrop() {
        if (dropHooked) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return;
        }
        long handle = mc.getWindow().getWindow();
        GLFW.glfwSetDropCallback(handle, (window, count, names) -> {
            for (int i = 0; i < count; i++) {
                String path = GLFWDropCallback.getName(names, i);
                if (path == null) {
                    continue;
                }
                Path file = Path.of(path);
                mc.execute(() -> notifyPlayer(importFile(file)));
                break;
            }
        });
        dropHooked = true;
        LOGGER.info("Barrier texture file-drop hook registered");
    }

    public static void openStorageFolder() {
        try {
            Files.createDirectories(storageDir());
            Path dir = storageDir().toAbsolutePath();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir.toFile());
            } else {
                new ProcessBuilder("explorer.exe", dir.toString()).start();
            }
            notifyPlayerRaw(Component.translatable("message." + Features.MODID + ".border_danger.barrier.folder_hint"));
        } catch (Exception e) {
            LOGGER.error("Failed to open barrier folder", e);
            notifyPlayer(false);
        }
    }

    /** Ctrl+V: image or file list from the system clipboard. */
    public static boolean pasteFromClipboard() {
        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable == null) {
                return false;
            }
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<java.io.File> files = (List<java.io.File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                for (java.io.File file : files) {
                    if (file.getName().toLowerCase(Locale.ROOT).endsWith(".png") && importFile(file.toPath())) {
                        notifyPlayer(true);
                        return true;
                    }
                }
            }
            if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Image image = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                if (image != null && saveBuffered(toBuffered(image))) {
                    FeaturesConfig.setBorderBarrierVisual(BorderBarrierVisual.CUSTOM);
                    notifyPlayer(true);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Clipboard paste for barrier texture failed", e);
        }
        notifyPlayer(false);
        return false;
    }

    public static boolean importFile(Path source) {
        if (source == null || !Files.isRegularFile(source)) {
            return false;
        }
        String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
            LOGGER.warn("Barrier texture must be PNG/JPG: {}", source);
            return false;
        }
        try {
            Files.createDirectories(storageDir());
            if (name.endsWith(".png")) {
                Files.copy(source, storageFile(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                BufferedImage image = ImageIO.read(source.toFile());
                if (image == null || !saveBuffered(image)) {
                    return false;
                }
            }
            if (!reload()) {
                return false;
            }
            FeaturesConfig.setBorderBarrierVisual(BorderBarrierVisual.CUSTOM);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to import barrier texture from {}", source, e);
            return false;
        }
    }

    public static boolean clear() {
        try {
            unload();
            Files.deleteIfExists(storageFile());
            lastFileTime = -1L;
            if (FeaturesConfig.borderBarrierVisual == BorderBarrierVisual.CUSTOM) {
                FeaturesConfig.setBorderBarrierVisual(BorderBarrierVisual.VANILLA);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to clear barrier texture", e);
            return false;
        }
    }

    public static void pollExternalChanges() {
        Path file = storageFile();
        if (!Files.isRegularFile(file)) {
            if (loaded) {
                unload();
            }
            return;
        }
        try {
            long time = Files.getLastModifiedTime(file).toMillis();
            if (time != lastFileTime) {
                reload();
                FeaturesConfig.setBorderBarrierVisual(BorderBarrierVisual.CUSTOM);
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean reload() {
        Path file = storageFile();
        if (!Files.isRegularFile(file)) {
            unload();
            lastFileTime = -1L;
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            if (texture != null) {
                texture.close();
                texture = null;
            }
            texture = new DynamicTexture(image);
            mc.getTextureManager().register(ID, texture);
            loaded = true;
            lastFileTime = Files.getLastModifiedTime(file).toMillis();
            LOGGER.info("Loaded custom barrier texture {} ({}x{})", file, image.getWidth(), image.getHeight());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load custom barrier texture {}", file, e);
            unload();
            return false;
        }
    }

    public static void unload() {
        Minecraft mc = Minecraft.getInstance();
        if (texture != null) {
            texture.close();
            texture = null;
        }
        if (loaded) {
            mc.getTextureManager().release(ID);
            loaded = false;
        }
    }

    public static boolean isReady() {
        return loaded || (hasFile() && reload());
    }

    private static boolean saveBuffered(BufferedImage image) throws Exception {
        Files.createDirectories(storageDir());
        try (OutputStream out = Files.newOutputStream(storageFile())) {
            if (!ImageIO.write(ensureArgb(image), "png", out)) {
                return false;
            }
        }
        return reload();
    }

    private static BufferedImage toBuffered(Image image) {
        if (image instanceof BufferedImage buffered) {
            return buffered;
        }
        int w = Math.max(1, image.getWidth(null));
        int h = Math.max(1, image.getHeight(null));
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        buffered.getGraphics().drawImage(image, 0, 0, null);
        return buffered;
    }

    private static BufferedImage ensureArgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            return image;
        }
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(image, 0, 0, null);
        return copy;
    }

    private static void notifyPlayer(boolean ok) {
        notifyPlayerRaw(Component.translatable(ok
                ? "message." + Features.MODID + ".border_danger.barrier.loaded"
                : "message." + Features.MODID + ".border_danger.barrier.failed"));
    }

    private static void notifyPlayerRaw(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(message, true);
        }
    }
}
