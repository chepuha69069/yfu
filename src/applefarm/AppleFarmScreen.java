package applefarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;

/**
 * Небольшое стеклянное меню в стиле iOS "liquid glass".
 * Прозрачная панель без затемнения экрана: игра остаётся видна сквозь стекло.
 */
public class AppleFarmScreen extends class_437 {

    // ---------- палитра (ARGB) ----------
    private static final int WHITE = 0xFFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xB3FFFFFF;
    private static final int TEXT_FAINT = 0x80FFFFFF;
    private static final int GREEN = 0xFF34C759;
    private static final int TINT = 0xBFD8FF; // лёгкий холодный оттенок стекла

    // ---------- размеры ----------
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 330;
    private static final int PANEL_R = 16;
    private static final int PAD = 14;
    private static final int HEADER_H = 84;
    private static final int FOOTER_H = 26;
    private static final int GAP = 6;

    private enum Kind { FLAG, SLIDER, MODE }

    private static final class Row {
        final Setting setting;
        final Kind kind;
        final String name;
        final String description;
        int y;      // экранная координата верхнего края (обновляется каждый кадр)
        int h;

        Row(Setting setting, Kind kind, String name, String description) {
            this.setting = setting;
            this.kind = kind;
            this.name = name;
            this.description = description;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final List<Row> visible = new ArrayList<>();

    private int left, top, panelW, panelH;
    private int listTop, listBottom;
    private int cardX, cardW;

    private float openProgress;
    private long lastFrame = System.nanoTime();
    private float scroll, scrollTarget, contentHeight;
    private Row dragging;
    private Row hovered;
    private boolean hoverPower;

    public AppleFarmScreen() {
        super(class_2561.method_43470("Apple Farm"));
    }

    // ================= жизненный цикл =================

    @Override
    protected void method_25426() {
        rows.clear();
        for (Setting s : AppleMenu.settings()) {
            if (s == null) {
                continue;
            }
            String name = safe(s.name);
            String desc = safe(s.description);
            if (s instanceof Setting.Flag) {
                rows.add(new Row(s, Kind.FLAG, name, desc));
            } else if (s instanceof Setting.Num) {
                rows.add(new Row(s, Kind.SLIDER, name, desc));
            } else if (s instanceof Setting.Mode) {
                rows.add(new Row(s, Kind.MODE, name, desc));
            }
        }
    }

    @Override
    public void method_25393() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1.0e9f);
        lastFrame = now;
        openProgress = Math.min(1.0f, openProgress + dt * 6.0f);
        scroll += (scrollTarget - scroll) * Math.min(1.0f, dt * 16.0f);
    }

    @Override
    public boolean method_25422() {
        return false; // игра не ставится на паузу
    }

    @Override
    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
        // фон не рисуем: никакого затемнения и размытия, только стекло
    }

    // ================= геометрия =================

    private void layout() {
        panelW = Math.min(PANEL_W, field_22789 - 20);
        panelH = Math.min(PANEL_H, field_22790 - 20);
        left = (field_22789 - panelW) / 2;
        top = (field_22790 - panelH) / 2;
        cardX = left + PAD;
        cardW = panelW - PAD * 2;
        listTop = top + HEADER_H;
        listBottom = top + panelH - FOOTER_H;

        visible.clear();
        float y = 0;
        for (Row r : rows) {
            if (!AppleMenu.isVisible(r.setting)) {
                continue;
            }
            r.h = switch (r.kind) {
                case FLAG -> 30;
                case SLIDER -> 42;
                case MODE -> 52;
            };
            r.y = (int) y;
            visible.add(r);
            y += r.h + GAP;
        }
        contentHeight = Math.max(0, y - GAP);
        float max = Math.max(0, contentHeight - (listBottom - listTop));
        scrollTarget = clamp(scrollTarget, 0, max);
        scroll = clamp(scroll, 0, max);
        for (Row r : visible) {
            r.y = listTop + r.y - Math.round(scroll);
        }
    }

    private int powerX() { return left + PAD; }
    private int powerY() { return top + 44; }
    private int powerW() { return panelW - PAD * 2; }
    private int powerH() { return 28; }

    // ================= отрисовка =================

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        layout();
        float a = easeOut(openProgress);
        int slide = Math.round((1.0f - a) * 12.0f);
        int oy = slide;

        hoverPower = inside(mouseX, mouseY, powerX(), powerY(), powerW(), powerH());
        hovered = rowAt(mouseX, mouseY);

        drawGlassPanel(ctx, left, top + oy, panelW, panelH, a);
        drawHeader(ctx, oy, a);

        ctx.method_44379(left + 2, listTop + oy, left + panelW - 2, listBottom + oy);
        for (Row r : visible) {
            if (r.y + r.h < listTop || r.y > listBottom) {
                continue;
            }
            drawRow(ctx, r, oy, a);
        }
        ctx.method_44380();

        drawScrollbar(ctx, oy, a);
        drawFooter(ctx, oy, a);
    }

    private void drawHeader(class_332 ctx, int oy, float a) {
        boolean on = AppleMenu.isEnabled();
        boolean found = AppleMenu.module() != null;

        text(ctx, "Apple Farm", left + PAD, top + 13 + oy, withAlpha(TEXT, a));
        String sub = !found ? "модуль не найден" : on ? "работает" : "выключено";
        text(ctx, sub, left + PAD, top + 26 + oy, withAlpha(on ? GREEN : TEXT_FAINT, a));

        // кнопка включения
        int x = powerX(), y = powerY() + oy, w = powerW(), h = powerH();
        int fill = on ? withAlpha(GREEN & 0xFFFFFF | 0xFF000000, 0.78f * a) : withAlpha(0x38FFFFFF, a);
        if (hoverPower) {
            fill = on ? withAlpha(0xFF4CD964, 0.9f * a) : withAlpha(0x4DFFFFFF, a);
        }
        roundRect(ctx, x, y, w, h, h / 2, fill);
        roundRect(ctx, x + 1, y + 1, w - 2, h / 2 - 1, h / 2 - 1, withAlpha(0x2EFFFFFF, a)); // блик
        roundOutline(ctx, x, y, w, h, h / 2, withAlpha(on ? 0x66FFFFFF : 0x55FFFFFF, a));

        String label = on ? "Выключить" : "Включить";
        class_327 font = field_22793;
        int tw = font.method_1727(label);
        disc(ctx, x + (w - tw) / 2 - 10, y + h / 2, 3, withAlpha(on ? 0xFFFFFFFF : 0x99FFFFFF, a));
        text(ctx, label, x + (w - tw) / 2 + 2, y + (h - 8) / 2, withAlpha(TEXT, a));

        fillRect(ctx, left + PAD, top + HEADER_H - 7 + oy, panelW - PAD * 2, 1, withAlpha(0x26FFFFFF, a));
    }

    private void drawRow(class_332 ctx, Row r, int oy, float a) {
        int y = r.y + oy;
        boolean hot = r == hovered;
        float fade = a * clamp(1.25f - Math.max(0, (r.y - listTop)) / 400.0f, 0.4f, 1.0f);

        roundRect(ctx, cardX, y, cardW, r.h, 10, withAlpha(hot ? 0x2CFFFFFF : 0x18FFFFFF, fade));
        roundOutline(ctx, cardX, y, cardW, r.h, 10, withAlpha(0x22FFFFFF, fade));
        fillRect(ctx, cardX + 10, y, cardW - 20, 1, withAlpha(0x30FFFFFF, fade)); // верхний блик карточки

        switch (r.kind) {
            case FLAG -> drawFlag(ctx, r, y, fade);
            case SLIDER -> drawSlider(ctx, r, y, fade);
            case MODE -> drawMode(ctx, r, y, fade);
        }
    }

    private void drawFlag(class_332 ctx, Row r, int y, float a) {
        boolean on = AppleMenu.flag(r.setting);
        int sw = 38, sh = 20;
        int sx = cardX + cardW - sw - 12;
        int sy = y + (r.h - sh) / 2;

        text(ctx, fit(r.name, cardW - sw - 36), cardX + 12, y + (r.h - 8) / 2, withAlpha(TEXT, a));

        roundRect(ctx, sx, sy, sw, sh, sh / 2, on ? withAlpha(0xFF000000 | GREEN, 0.85f * a) : withAlpha(0x40FFFFFF, a));
        roundOutline(ctx, sx, sy, sw, sh, sh / 2, withAlpha(0x40FFFFFF, a));
        int knob = sh - 4;
        int kx = on ? sx + sw - knob - 2 : sx + 2;
        disc(ctx, kx + knob / 2, sy + sh / 2 + 1, knob / 2, withAlpha(0x40000000, a));
        disc(ctx, kx + knob / 2, sy + sh / 2, knob / 2, withAlpha(0xFFFFFFFF, a));
    }

    private void drawSlider(class_332 ctx, Row r, int y, float a) {
        float value = AppleMenu.number(r.setting);
        float min = AppleMenu.minimum(r.setting);
        float max = AppleMenu.maximum(r.setting);
        float t = max - min <= 0 ? 0 : clamp((value - min) / (max - min), 0, 1);
        String shown = AppleMenu.integral(r.setting)
            ? String.valueOf(Math.round(value))
            : String.format(Locale.ROOT, "%.1f", value);

        int vw = field_22793.method_1727(shown);
        text(ctx, fit(r.name, cardW - vw - 36), cardX + 12, y + 8, withAlpha(TEXT, a));
        text(ctx, shown, cardX + cardW - 12 - vw, y + 8, withAlpha(TEXT_DIM, a));

        int tx = cardX + 12, tw = cardW - 24, ty = y + 27, th = 6;
        roundRect(ctx, tx, ty, tw, th, th / 2, withAlpha(0x38FFFFFF, a));
        int fw = Math.round(tw * t);
        if (fw > 0) {
            roundRect(ctx, tx, ty, Math.max(fw, th), th, th / 2, withAlpha(0xE6FFFFFF, a));
        }
        int kx = tx + fw;
        disc(ctx, kx, ty + th / 2 + 1, 6, withAlpha(0x40000000, a));
        disc(ctx, kx, ty + th / 2, 6, withAlpha(0xFFFFFFFF, a));
    }

    private void drawMode(class_332 ctx, Row r, int y, float a) {
        text(ctx, fit(r.name, cardW - 24), cardX + 12, y + 8, withAlpha(TEXT, a));
        List<String> modes = AppleMenu.modes(r.setting);
        String current = AppleMenu.mode(r.setting);
        int px = cardX + 12;
        for (String m : modes) {
            int w = field_22793.method_1727(m) + 18;
            boolean sel = m.equals(current);
            roundRect(ctx, px, y + 24, w, 20, 10, sel ? withAlpha(0x55FFFFFF, a) : withAlpha(0x20FFFFFF, a));
            roundOutline(ctx, px, y + 24, w, 20, 10, withAlpha(sel ? 0x77FFFFFF : 0x2AFFFFFF, a));
            text(ctx, m, px + 9, y + 30, withAlpha(sel ? TEXT : TEXT_DIM, a));
            px += w + 6;
        }
    }

    private void drawScrollbar(class_332 ctx, int oy, float a) {
        float viewport = listBottom - listTop;
        if (contentHeight <= viewport) {
            return;
        }
        int barH = Math.max(24, (int) (viewport * viewport / contentHeight));
        float max = contentHeight - viewport;
        int barY = listTop + (int) ((viewport - barH) * (scroll / Math.max(1.0f, max)));
        roundRect(ctx, left + panelW - 8, barY + oy, 3, barH, 1, withAlpha(0x66FFFFFF, a));
    }

    private void drawFooter(class_332 ctx, int oy, float a) {
        String hint = hovered != null && !hovered.description.isEmpty()
            ? hovered.description
            : "ESC - закрыть";
        text(ctx, fit(hint, panelW - PAD * 2), left + PAD, top + panelH - FOOTER_H + 9 + oy, withAlpha(TEXT_FAINT, a));
    }

    // ================= стекло =================

    /** Полупрозрачная панель: градиент, холодный оттенок, световой блик, подсвеченная кромка. */
    private void drawGlassPanel(class_332 ctx, int x, int y, int w, int h, float a) {
        // мягкая тень
        for (int i = 3; i >= 1; i--) {
            roundRect(ctx, x - i, y - i + 3, w + i * 2, h + i * 2, PANEL_R + i, withAlpha(0x0E000000, a));
        }
        // тело: сверху светлее, снизу прозрачнее
        for (int row = 0; row < h; row++) {
            float t = row / (float) (h - 1);
            int inset = cornerInset(PANEL_R, row, h);
            int alpha = (int) (lerp(0x50, 0x1C, t) * a);
            int tint = lerp(WHITE, TINT, t);
            fillRect(ctx, x + inset, y + row, w - inset * 2, 1, (alpha << 24) | tint);
        }
        // светящийся изгиб в верхней части
        int sheenH = h / 3;
        for (int row = 2; row < sheenH; row++) {
            float t = 1.0f - row / (float) sheenH;
            int inset = cornerInset(PANEL_R, row, h) + 3;
            int alpha = (int) (0x2A * t * t * a);
            fillRect(ctx, x + inset, y + row, w - inset * 2, 1, (alpha << 24) | WHITE);
        }
        // кромка: ярче сверху-слева, слабее снизу
        for (int row = 0; row < h; row++) {
            float t = row / (float) (h - 1);
            int inset = cornerInset(PANEL_R, row, h);
            int alpha = (int) (lerp(0xA0, 0x38, t) * a);
            int c = (alpha << 24) | WHITE;
            boolean edgeRow = row == 0 || row == h - 1;
            if (edgeRow) {
                fillRect(ctx, x + inset, y + row, w - inset * 2, 1, c);
            } else {
                fillRect(ctx, x + inset, y + row, 1, 1, c);
                fillRect(ctx, x + w - inset - 1, y + row, 1, 1, c);
            }
        }
    }

    private static int cornerInset(int radius, int row, int h) {
        int d = Math.min(row, h - 1 - row);
        if (d >= radius) {
            return 0;
        }
        float dy = radius - d - 0.5f;
        float dx = (float) Math.sqrt(Math.max(0, radius * radius - dy * dy));
        return Math.max(0, (int) Math.ceil(radius - dx));
    }

    // ================= ввод =================

    @Override
    public boolean method_25402(double mx, double my, int button) {
        if (button == 0) {
            if (inside(mx, my, powerX(), powerY(), powerW(), powerH())) {
                AppleMenu.toggle();
                return true;
            }
            Row r = rowAt(mx, my);
            if (r != null) {
                switch (r.kind) {
                    case FLAG -> AppleMenu.flag(r.setting, !AppleMenu.flag(r.setting));
                    case SLIDER -> {
                        dragging = r;
                        applyDrag(mx);
                    }
                    case MODE -> clickMode(r, mx, my);
                }
                return true;
            }
        }
        return super.method_25402(mx, my, button);
    }

    @Override
    public boolean method_25406(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.method_25406(mx, my, button);
    }

    @Override
    public boolean method_25403(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            applyDrag(mx);
            return true;
        }
        return super.method_25403(mx, my, button, dx, dy);
    }

    @Override
    public boolean method_25401(double mx, double my, double horizontal, double vertical) {
        if (contentHeight > listBottom - listTop) {
            scrollTarget -= (float) vertical * 22.0f;
            return true;
        }
        return super.method_25401(mx, my, horizontal, vertical);
    }

    @Override
    public boolean method_25404(int key, int scancode, int modifiers) {
        if (key == 256) { // ESC
            method_25419();
            return true;
        }
        return super.method_25404(key, scancode, modifiers);
    }

    private Row rowAt(double mx, double my) {
        if (mx < cardX || mx > cardX + cardW || my < listTop || my > listBottom) {
            return null;
        }
        for (Row r : visible) {
            if (my >= r.y && my < r.y + r.h) {
                return r;
            }
        }
        return null;
    }

    private void clickMode(Row r, double mx, double my) {
        int px = cardX + 12;
        for (String m : AppleMenu.modes(r.setting)) {
            int w = field_22793.method_1727(m) + 18;
            if (mx >= px && mx <= px + w && my >= r.y + 24 && my <= r.y + 44) {
                if (!m.equals(AppleMenu.mode(r.setting))) {
                    AppleMenu.mode(r.setting, m);
                }
                return;
            }
            px += w + 6;
        }
    }

    private void applyDrag(double mx) {
        if (dragging == null) {
            return;
        }
        int tx = cardX + 12, tw = cardW - 24;
        float t = clamp((float) ((mx - tx) / Math.max(1, tw)), 0, 1);
        float min = AppleMenu.minimum(dragging.setting);
        float max = AppleMenu.maximum(dragging.setting);
        float v = min + (max - min) * t;
        if (AppleMenu.integral(dragging.setting)) {
            v = Math.round(v);
        }
        AppleMenu.number(dragging.setting, v);
    }

    // ================= примитивы =================

    private void text(class_332 ctx, String s, int x, int y, int color) {
        if ((color >>> 24) < 8) {
            return;
        }
        ctx.method_25303(field_22793, s, x, y, color);
    }

    private String fit(String s, int width) {
        if (field_22793.method_1727(s) <= width) {
            return s;
        }
        return field_22793.method_27523(s, Math.max(0, width - 8)) + "...";
    }

    private static void fillRect(class_332 ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        ctx.method_25294(x, y, x + w, y + h, color);
    }

    private static void roundRect(class_332 ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        int r = Math.min(radius, Math.min(w, h) / 2);
        for (int row = 0; row < h; row++) {
            int inset = cornerInset(r, row, h);
            fillRect(ctx, x + inset, y + row, w - inset * 2, 1, color);
        }
    }

    private static void roundOutline(class_332 ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        int r = Math.min(radius, Math.min(w, h) / 2);
        for (int row = 0; row < h; row++) {
            int inset = cornerInset(r, row, h);
            if (row == 0 || row == h - 1) {
                fillRect(ctx, x + inset, y + row, w - inset * 2, 1, color);
            } else {
                fillRect(ctx, x + inset, y + row, 1, 1, color);
                fillRect(ctx, x + w - inset - 1, y + row, 1, 1, color);
            }
        }
    }

    private static void disc(class_332 ctx, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(Math.max(0, radius * radius - dy * dy));
            fillRect(ctx, cx - dx, cy + dy, dx * 2 + 1, 1, color);
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static float easeOut(float t) {
        float k = 1.0f - t;
        return 1.0f - k * k * k;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int lerp(int c1, int c2, float t) {
        int r = (int) lerp((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = (int) lerp((c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF, t);
        int b = (int) lerp(c1 & 0xFF, c2 & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    /** Умножает альфу ARGB-цвета на коэффициент. */
    private static int withAlpha(int argb, float factor) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * clamp(factor, 0, 1));
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
