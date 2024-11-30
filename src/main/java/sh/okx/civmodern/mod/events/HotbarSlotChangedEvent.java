package sh.okx.civmodern.mod.events;

public record HotbarSlotChangedEvent(
    int oldSlot,
    int newSlot
) {

}
