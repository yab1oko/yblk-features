package yblk.features.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yblk.features.inventory.InventoryRevampGui;
import yblk.features.inventory.LockedSlots;
import yblk.features.inventory.SlotCalculator;

/**
 * Locked-slot overlays + Inventory-Revamp chrome for non-{@link InventoryScreen}
 * menus that still use {@link InventoryMenu} (Curios).
 * <p>
 * Do not inject into {@code renderBg} here — it is abstract on this class.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    private static final int FOG = 0xA0606068;
    private static final int CROSS = 0xE0A0A0A8;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
                    shift = At.Shift.AFTER
            )
    )
    private void features$inventoryRevampChrome(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                                CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        // InventoryScreen paints via its own mixin on renderBg.
        if (self instanceof InventoryScreen) {
            return;
        }
        if (!(self.getMenu() instanceof InventoryMenu)) {
            return;
        }
        InventoryRevampGui.renderOverlay(self, graphics);
    }

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void features$lockedOverlay(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (!SlotCalculator.isActive() || !LockedSlots.showLockedItem(slot)) {
            return;
        }
        int x = slot.x;
        int y = slot.y;
        graphics.fill(x, y, x + 16, y + 16, FOG);
        drawCross(graphics, x, y);
    }

    private static void drawCross(GuiGraphics graphics, int slotX, int slotY) {
        for (int i = 2; i < 14; i++) {
            graphics.fill(slotX + i, slotY + i, slotX + i + 2, slotY + i + 2, CROSS);
            graphics.fill(slotX + 14 - i, slotY + i, slotX + 16 - i, slotY + i + 2, CROSS);
        }
    }
}
