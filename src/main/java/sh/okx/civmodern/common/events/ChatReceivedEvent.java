package sh.okx.civmodern.common.events;

import net.minecraft.network.chat.Component;

public record ChatReceivedEvent(Component message) {
}
