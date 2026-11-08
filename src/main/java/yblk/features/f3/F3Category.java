package yblk.features.f3;

public enum F3Category {
    IMPORTANT,
    OTHER;

    public String langKey() {
        return "screen.features.f3_hide.category." + name().toLowerCase();
    }
}
