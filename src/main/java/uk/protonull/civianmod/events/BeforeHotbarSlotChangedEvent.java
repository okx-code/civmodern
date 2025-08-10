package uk.protonull.civianmod.events;

public record BeforeHotbarSlotChangedEvent(
    int oldSlot,
    int newSlot
) {

}
