package yblk.features.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yblk.features.Features;
import yblk.features.f3.F3Element;
import yblk.features.f3.F3Hide;
import yblk.features.f3.F3LineClassifier;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Features.MODID, value = Dist.CLIENT)
public final class F3HideClient {
    private F3HideClient() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!F3Hide.isActive()) {
            return;
        }
        filterSide(event.getLeft(), true);
        filterSide(event.getRight(), false);
    }

    private static void filterSide(List<String> lines, boolean left) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        F3LineClassifier classifier = new F3LineClassifier(left);
        List<String> out = new ArrayList<>(lines.size());
        for (String raw : lines) {
            F3LineClassifier.Classified classified = classifier.classify(raw);
            if (classified.line() == null) {
                continue;
            }
            // Blank separators — keep for now, collapse later
            if (classified.element() == null) {
                out.add("");
                continue;
            }

            if (classified.element() == F3Element.DIFFICULTY) {
                String rewritten = F3LineClassifier.rewriteDifficultyLine(
                        classified.line(),
                        F3Hide.isHidden(F3Element.DIFFICULTY),
                        F3Hide.isHidden(F3Element.DAY));
                if (rewritten != null && !rewritten.isBlank()) {
                    out.add(rewritten);
                }
                continue;
            }

            if (F3Hide.isHidden(classified.element())) {
                continue;
            }
            out.add(classified.line());
        }
        collapseBlanks(out);
        lines.clear();
        lines.addAll(out);
    }

    private static void collapseBlanks(List<String> lines) {
        List<String> cleaned = new ArrayList<>(lines.size());
        boolean prevBlank = true; // trim leading blanks
        for (String line : lines) {
            boolean blank = line == null || line.isBlank();
            if (blank) {
                if (!prevBlank) {
                    cleaned.add("");
                }
                prevBlank = true;
            } else {
                cleaned.add(line);
                prevBlank = false;
            }
        }
        while (!cleaned.isEmpty() && cleaned.get(cleaned.size() - 1).isBlank()) {
            cleaned.remove(cleaned.size() - 1);
        }
        lines.clear();
        lines.addAll(cleaned);
    }
}
