package yblk.features.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Shared Inventory-Revamp chrome: gray locked center cells + side-wing panels.
 * Used by the vanilla inventory and Curios screens ({@link InventoryMenu} subclasses).
 */
@OnlyIn(Dist.CLIENT)
public final class InventoryRevampGui {
    private static final ResourceLocation INV_TEX = new ResourceLocation("textures/gui/container/inventory.png");
    private static final int PANEL = 0xFFC6C6C6;

    private InventoryRevampGui() {
    }

    public static void renderOverlay(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
        if (!SlotCalculator.isActive()) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (!(menu instanceof InventoryMenu)) {
            return;
        }
        Player player = null;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory inv) {
                player = inv.player;
                break;
            }
        }
        if (player == null) {
            return;
        }

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int unlocked = InventoryRevampEvents.clientUnlocked(player);
        int center = SlotCalculator.unlockedCenter(unlocked);

        for (Slot slot : menu.slots) {
            if (SideInventorySlot.isSideSlot(slot) || !(slot.container instanceof Inventory)) {
                continue;
            }
            int invIndex = slot.getContainerSlot();
            if (invIndex < 9 || invIndex >= 36) {
                continue;
            }
            if (invIndex - 9 >= center && slot.getItem().isEmpty()) {
                graphics.fill(left + slot.x - 1, top + slot.y - 1,
                        left + slot.x + 17, top + slot.y + 17, PANEL);
            }
        }

        paintWing(screen, graphics, true);
        paintWing(screen, graphics, false);
    }

    private static void paintWing(AbstractContainerScreen<?> screen, GuiGraphics graphics, boolean rightWing) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean any = false;
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot instanceof SideInventorySlot side) || !side.isUnlocked()) {
                continue;
            }
            boolean isRight = side.getBufferIndex() < SlotCalculator.SIDE_PER_COLUMN;
            if (isRight != rightWing) {
                continue;
            }
            any = true;
            int sx = left + slot.x;
            int sy = top + slot.y;
            minX = Math.min(minX, sx - 1);
            minY = Math.min(minY, sy - 1);
            maxX = Math.max(maxX, sx + 17);
            maxY = Math.max(maxY, sy + 17);
        }
        if (!any) {
            return;
        }

        graphics.fill(minX, minY, maxX, maxY, PANEL);
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot instanceof SideInventorySlot side) || !side.isUnlocked()) {
                continue;
            }
            boolean isRight = side.getBufferIndex() < SlotCalculator.SIDE_PER_COLUMN;
            if (isRight != rightWing) {
                continue;
            }
            graphics.blit(INV_TEX, left + slot.x - 1, top + slot.y - 1, 7, 83, 18, 18);
        }
    }
}
