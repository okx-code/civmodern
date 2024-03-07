package sh.okx.civmodern.common.events;

public class ClientTickEvent {
    /** Pre-made tick event instance to prevent unnecessary allocation (yay, micro-optimisations) */
    public static final ClientTickEvent PREMADE = new ClientTickEvent();
}
