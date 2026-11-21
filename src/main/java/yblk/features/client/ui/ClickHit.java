package yblk.features.client.ui;

public record ClickHit(int x, int y, int w, int h, Runnable action) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
