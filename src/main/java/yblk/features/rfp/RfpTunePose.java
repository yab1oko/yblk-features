package yblk.features.rfp;

import yblk.features.config.FeaturesConfig;

/** Pose forced while the RFP settings screen is open, for live offset tuning. */
public enum RfpTunePose {
    STAND,
    SNEAK,
    SIT,
    SWIM,
    ELYTRA;

    public String langKey() {
        return "screen.features.real_first_person.tune." + name().toLowerCase();
    }

    public float offset() {
        return switch (this) {
            case STAND -> FeaturesConfig.rfpOffsetStand;
            case SNEAK -> FeaturesConfig.rfpOffsetSneak;
            case SIT -> FeaturesConfig.rfpOffsetSit;
            case SWIM -> FeaturesConfig.rfpOffsetSwim;
            case ELYTRA -> FeaturesConfig.rfpOffsetElytra;
        };
    }
}
