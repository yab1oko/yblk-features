package yblk.features.wetness;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class Wetness {
    public static final float MAX = 100f;

    private float amount;
    /** Seconds spent wet while on dry land (for freeze buildup). */
    private int wetLandSeconds;
    private boolean wasInWater;

    public float get() {
        return amount;
    }

    public void set(float value) {
        amount = Mth.clamp(value, 0f, MAX);
        if (amount <= 0.01f) {
            amount = 0f;
            wetLandSeconds = 0;
        }
    }

    public void add(float delta) {
        set(amount + delta);
    }

    public int getWetLandSeconds() {
        return wetLandSeconds;
    }

    public void setWetLandSeconds(int seconds) {
        wetLandSeconds = Math.max(0, seconds);
    }

    public boolean wasInWater() {
        return wasInWater;
    }

    public void setWasInWater(boolean inWater) {
        wasInWater = inWater;
    }

    public boolean isWet() {
        return amount > 0.5f;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("amount", amount);
        tag.putInt("wetLand", wetLandSeconds);
        tag.putBoolean("wasWater", wasInWater);
        return tag;
    }

    public void load(CompoundTag tag) {
        amount = tag.getFloat("amount");
        wetLandSeconds = tag.getInt("wetLand");
        wasInWater = tag.getBoolean("wasWater");
    }

    public static Wetness of(Player player) {
        return WetnessCapability.get(player);
    }
}
