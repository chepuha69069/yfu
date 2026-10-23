package applefarm;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

/** Точка входа: тик фермы и чат-команда ".apple". */
public final class AppleFarmMod implements ClientModInitializer {

    public static final String COMMAND = "apple";

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                AppleFarm.INSTANCE.tick();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        ClientSendMessageEvents.ALLOW_CHAT.register(AppleFarmMod::allowChat);
    }

    /** @return false - сообщение перехвачено и в чат не уходит. */
    private static boolean allowChat(String message) {
        if (message == null) {
            return true;
        }
        String text = message.trim();
        if (!text.startsWith(".")) {
            return true;
        }
        String name = text.substring(1).trim().split("\\s+", 2)[0];
        if (!COMMAND.equalsIgnoreCase(name)) {
            return true;
        }
        AppleMenu.open();
        return false;
    }
}
