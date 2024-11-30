package sh.okx.civmodern.mod.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class IceRoadSettings {
    private static final boolean DEFAULT_SNAP_PITCH = false;
    private static final boolean DEFAULT_SNAP_YAW = true;
    private static final boolean DEFAULT_AUTO_EAT = false;
    private static final boolean DEFAULT_STOP_WHEN_HUNGRY = true;

    @SerialEntry
    public boolean snapPitch = DEFAULT_SNAP_PITCH;

    @SerialEntry
    public boolean snapYaw = DEFAULT_SNAP_YAW;

    @SerialEntry
    public boolean autoEat = DEFAULT_AUTO_EAT;

    @SerialEntry
    public boolean stopWhenHungry = DEFAULT_STOP_WHEN_HUNGRY;

    // ============================================================
    // Screen generation
    // ============================================================

    static @NotNull OptionGroup generateGroup(
        final @NotNull IceRoadSettings iceRoadSettings
    ) {
        return OptionGroup.createBuilder()
            .name(Component.translatable("civmodern.config.group.iceRoad"))
            .collapsed(true)
            .option(generateSnapPitch(iceRoadSettings))
            .option(generateSnapYaw(iceRoadSettings))
            .option(generateAutoEat(iceRoadSettings))
            .option(generateStopWhenHungry(iceRoadSettings))
            .build();
    }

    private static @NotNull Option<?> generateSnapPitch(
        final @NotNull IceRoadSettings iceRoadSettings
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civmodern.config.group.iceRoad.snapPitch"))
            .controller(BooleanControllerBuilder::create)
            .binding(
                DEFAULT_SNAP_PITCH,
                () -> iceRoadSettings.snapPitch,
                (snap) -> iceRoadSettings.snapPitch = snap
            )
            .build();
    }

    private static @NotNull Option<?> generateSnapYaw(
        final @NotNull IceRoadSettings iceRoadSettings
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civmodern.config.group.iceRoad.snapYaw"))
            .controller(BooleanControllerBuilder::create)
            .binding(
                DEFAULT_SNAP_YAW,
                () -> iceRoadSettings.snapYaw,
                (snap) -> iceRoadSettings.snapYaw = snap
            )
            .build();
    }

    private static @NotNull Option<?> generateAutoEat(
        final @NotNull IceRoadSettings iceRoadSettings
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civmodern.config.group.iceRoad.autoEat"))
            .controller(BooleanControllerBuilder::create)
            .binding(
                DEFAULT_AUTO_EAT,
                () -> iceRoadSettings.autoEat,
                (eat) -> iceRoadSettings.autoEat = eat
            )
            .build();
    }

    private static @NotNull Option<?> generateStopWhenHungry(
        final @NotNull IceRoadSettings iceRoadSettings
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civmodern.config.group.iceRoad.stopWhenHungry"))
            .controller(BooleanControllerBuilder::create)
            .binding(
                DEFAULT_STOP_WHEN_HUNGRY,
                () -> iceRoadSettings.stopWhenHungry,
                (stop) -> iceRoadSettings.stopWhenHungry = stop
            )
            .build();
    }
}
