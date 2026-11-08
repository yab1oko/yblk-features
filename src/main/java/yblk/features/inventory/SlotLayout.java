package yblk.features.inventory;

/** Default coordinates for the vanilla InventoryMenu storage grid. */
public final class SlotLayout {
    public static final int MAIN_ORIGIN_X = 8;
    public static final int GRID_ORIGIN_Y = 84;
    /** Vanilla inventory texture width — wings sit fully outside this box. */
    public static final int INVENTORY_WIDTH = 176;
    /** 3 columns × 18px; innermost left column ends just left of the GUI. */
    public static final int LEFT_WING_ORIGIN_X = -3 * 18;
    /** First right-wing column starts just right of the GUI. */
    public static final int RIGHT_WING_ORIGIN_X = INVENTORY_WIDTH + 1;

    private SlotLayout() {
    }
}
