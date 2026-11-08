package yblk.features.hud;

public enum HudElement {
    HEARTS("hearts"),
    HUNGER("hunger"),
    ARMOR("armor"),
    HOTBAR("hotbar"),
    EXPERIENCE("experience"),
    EFFECTS("effects"),
    CROSSHAIR("crosshair");

    private final String key;

    HudElement(String key) {
        this.key = key;
    }

    public String configKey() {
        return key;
    }

    public String langKey() {
        return "screen.features.hud_hide.element." + key;
    }

    public String showOnChangeLangKey() {
        return "screen.features.hud_hide.show_on_change." + key;
    }

    public boolean supportsShowOnChange() {
        return this != CROSSHAIR;
    }
}
