package uk.protonull.civianmod.config.gui;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.config.CivianModConfig;
import uk.protonull.civianmod.features.macros.IceRoadMacro;

public final class MacroConfigGui {
    public static @NotNull ConfigCategory generateCategory(
        final @NotNull CivianModConfig config
    ) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("civianmod.config.tab.macros.title"))
            .tooltip(Component.translatable("civianmod.config.tab.macros.desc"))
            .option(LabelOption.create(Component.translatable("civianmod.config.tab.macros.label.ice-road")))
            .option(generateSnapPitch(config))
            .option(generateSnapYaw(config))
            .option(generateAutoEat(config))
            .option(generateStopWhenHungry(config))
            .build();
    }

    private static @NotNull Option<?> generateSnapPitch(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.tab.macros.option.ice-road-pitch.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.macros.option.ice-road-pitch.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                IceRoadMacro.DEFAULT_SNAP_PITCH,
                () -> config.iceRoadSnapPitch,
                (snap) -> config.iceRoadSnapPitch = snap
            )
            .build();
    }

    private static @NotNull Option<?> generateSnapYaw(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.tab.macros.option.ice-road-yaw.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.macros.option.ice-road-yaw.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                IceRoadMacro.DEFAULT_SNAP_YAW,
                () -> config.iceRoadSnapYaw,
                (snap) -> config.iceRoadSnapYaw = snap
            )
            .build();
    }

    private static @NotNull Option<?> generateAutoEat(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.tab.macros.option.ice-road-eat.label"))
            .controller(BooleanControllerBuilder::create)
            .binding(
                IceRoadMacro.DEFAULT_AUTO_EAT,
                () -> config.iceRoadAutoEat,
                (eat) -> config.iceRoadAutoEat = eat
            )
            .build();
    }

    private static @NotNull Option<?> generateStopWhenHungry(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.tab.macros.option.ice-road-starving.label"))
            .controller(BooleanControllerBuilder::create)
            .binding(
                IceRoadMacro.DEFAULT_STOP_WHEN_STARVING,
                () -> config.iceRoadStopWhenStarving,
                (stop) -> config.iceRoadStopWhenStarving = stop
            )
            .build();
    }
}
