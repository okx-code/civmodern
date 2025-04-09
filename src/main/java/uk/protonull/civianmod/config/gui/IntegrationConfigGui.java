package uk.protonull.civianmod.config.gui;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.LongSliderControllerBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.config.CivianModConfig;
import uk.protonull.civianmod.features.ClickRailDest;

public final class IntegrationConfigGui {
    public static @NotNull ConfigCategory generateCategory(
        final @NotNull CivianModConfig config
    ) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("civianmod.config.tab.integrations.title"))
            .tooltip(Component.translatable("civianmod.config.tab.integrations.desc"))
            .option(LabelOption.create(Component.translatable("civianmod.config.tab.integrations.label.click-dest")))
            .option(generateClickDestEnabled(config))
            .option(generateClickDestCooldown(config))
            .build();
    }

    private static @NotNull Option<?> generateClickDestEnabled(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.tab.integrations.option.click-dest-enabled.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.integrations.option.click-dest-enabled.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                ClickRailDest.DEFAULT_ENABLED,
                () -> config.clickDestEnabled,
                (enabled) -> config.clickDestEnabled = enabled
            )
            .build();
    }

    private static @NotNull Option<?> generateClickDestCooldown(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Long>createBuilder()
            .name(Component.translatable("civianmod.config.tab.integrations.option.click-dest-cooldown.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.integrations.option.click-dest-cooldown.desc")))
            .controller((opt) -> LongSliderControllerBuilder.create(opt)
                .range(0L, 3000L)
                .step(20L)
                .formatValue((val) -> Component.translatable("civianmod.config.tab.integrations.option.click-dest-cooldown.format", val))
            )
            .binding(
                ClickRailDest.DEFAULT_COOLDOWN,
                () -> config.clickDestCooldown,
                (cooldown) -> config.clickDestCooldown = cooldown
            )
            .build();
    }
}
