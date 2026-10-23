package applefarm;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_310;

/** Связка GUI с фермой: открытие экрана, чтение и запись настроек. */
public final class AppleMenu {

    private AppleMenu() {
    }

    public static void open() {
        class_310 client = class_310.method_1551();
        // execute() - чтобы экран открылся после того, как чат закроется сам
        client.execute(() -> {
            if (!(client.field_1755 instanceof AppleFarmScreen)) {
                client.method_1507(new AppleFarmScreen());
            }
        });
    }

    public static AppleFarm module() { return AppleFarm.INSTANCE; }
    public static boolean isEnabled() { return AppleFarm.INSTANCE.isEnabled(); }
    public static void toggle() { AppleFarm.INSTANCE.toggle(); }
    public static List<Setting> settings() { return new ArrayList<>(AppleFarm.INSTANCE.settings()); }
    public static boolean isVisible(Setting s) { return s.isVisible(); }

    public static boolean flag(Setting s) { return s instanceof Setting.Flag f && f.get(); }
    public static void flag(Setting s, boolean v) { if (s instanceof Setting.Flag f) f.set(v); }

    public static float number(Setting s) { return s instanceof Setting.Num n ? n.getFloat() : 0.0f; }
    public static void number(Setting s, float v) { if (s instanceof Setting.Num n) n.set(v); }
    public static boolean integral(Setting s) { return s instanceof Setting.Num n && n.integral; }
    public static float minimum(Setting s) { return s instanceof Setting.Num n ? n.min : 0.0f; }
    public static float maximum(Setting s) { return s instanceof Setting.Num n ? n.max : 0.0f; }

    public static String mode(Setting s) { return s instanceof Setting.Mode m ? m.get() : ""; }
    public static List<String> modes(Setting s) { return s instanceof Setting.Mode m ? m.modes() : new ArrayList<>(); }
    public static void mode(Setting s, String v) { if (s instanceof Setting.Mode m) m.set(v); }
}
