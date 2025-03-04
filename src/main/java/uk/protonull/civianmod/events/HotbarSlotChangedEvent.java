package uk.protonull.civianmod.events;

public record HotbarSlotChangedEvent(
    int oldSlot,
    int newSlot
) {

}
