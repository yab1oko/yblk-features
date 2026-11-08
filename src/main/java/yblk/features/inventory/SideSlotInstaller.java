package yblk.features.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import yblk.features.mixin.AbstractContainerMenuAccessor;

/**
 * Adds Inventory-Revamp side-wing slots once per menu.
 * CuriosContainer extends InventoryMenu then clears/rebuilds slots — call again after that.
 */
public final class SideSlotInstaller {
    private SideSlotInstaller() {
    }

    public static void ensureSideSlots(AbstractContainerMenu menu, Inventory inventory, Player player) {
        if (menu == null || inventory == null || player == null) {
            return;
        }
        for (Slot slot : menu.slots) {
            if (SideInventorySlot.isSideSlot(slot)) {
                return;
            }
        }

        int gridY = SlotLayout.GRID_ORIGIN_Y;
        for (Slot slot : menu.slots) {
            if (slot.container == inventory && slot.getContainerSlot() == 9) {
                gridY = slot.y;
                break;
            }
        }

        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) menu;
        ExtraInventory extra = ExtraInventoryCapability.get(player);
        for (int i = 0; i < SlotCalculator.SIDE_PER_COLUMN; i++) {
            int row = i / 3;
            int col = i % 3;
            accessor.features$addSlot(new SideInventorySlot(player, extra.getHandler(), i,
                    SlotLayout.RIGHT_WING_ORIGIN_X + col * 18, gridY + row * 18));
        }
        for (int i = 0; i < SlotCalculator.SIDE_PER_COLUMN; i++) {
            int row = i / 3;
            int col = 2 - (i % 3);
            int buffer = SlotCalculator.SIDE_PER_COLUMN + i;
            accessor.features$addSlot(new SideInventorySlot(player, extra.getHandler(), buffer,
                    SlotLayout.LEFT_WING_ORIGIN_X + col * 18, gridY + row * 18));
        }
    }
}
