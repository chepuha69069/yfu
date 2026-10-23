package net.fabricmc.fabric.api.client.message.v1;
import net.fabricmc.fabric.api.event.Event;
public final class ClientSendMessageEvents {
    public static final Event<AllowChat> ALLOW_CHAT = null;
    @FunctionalInterface
    public interface AllowChat { boolean allowSendChatMessage(String message); }
}
