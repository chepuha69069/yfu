package net.fabricmc.fabric.api.client.event.lifecycle.v1;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.class_310;
public final class ClientTickEvents {
    public static final Event<EndTick> END_CLIENT_TICK = null;
    @FunctionalInterface
    public interface EndTick { void onEndTick(class_310 client); }
}
