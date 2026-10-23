package applefarm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Мини-система настроек (замена настроек Releon). */
public abstract class Setting {

    public final String name;
    public final String description;
    private Supplier<Boolean> visible = () -> true;

    private Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean isVisible() {
        try {
            return visible.get();
        } catch (Throwable t) {
            return true;
        }
    }

    protected void setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
    }

    /** Переключатель. */
    public static final class Flag extends Setting {
        private boolean value;

        public Flag(String name, String description, boolean value) {
            super(name, description);
            this.value = value;
        }

        public Flag visibleWhen(Supplier<Boolean> condition) {
            setVisible(condition);
            return this;
        }

        public boolean get() {
            return value;
        }

        public void set(boolean value) {
            this.value = value;
        }
    }

    /** Число (целое, если задан целый диапазон). */
    public static final class Num extends Setting {
        private float value;
        public final float min;
        public final float max;
        public final boolean integral;

        public Num(String name, String description, int value, int min, int max) {
            super(name, description);
            this.value = value;
            this.min = min;
            this.max = max;
            this.integral = true;
        }

        public Num(String name, String description, float value, float min, float max) {
            super(name, description);
            this.value = value;
            this.min = min;
            this.max = max;
            this.integral = false;
        }

        public float getFloat() {
            return value;
        }

        public int getInt() {
            return Math.round(value);
        }

        public void set(float v) {
            v = Math.max(min, Math.min(max, v));
            this.value = integral ? Math.round(v) : v;
        }
    }

    /** Выбор из списка. */
    public static final class Mode extends Setting {
        private final List<String> modes = new ArrayList<>();
        private String value;

        public Mode(String name, String description, String value, String... modes) {
            super(name, description);
            this.modes.addAll(List.of(modes));
            this.value = value;
        }

        public String get() {
            return value;
        }

        public boolean is(String mode) {
            return value.equals(mode);
        }

        public List<String> modes() {
            return modes;
        }

        public void set(String mode) {
            if (modes.contains(mode)) {
                this.value = mode;
            }
        }
    }
}
