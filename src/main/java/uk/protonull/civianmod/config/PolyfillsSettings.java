package uk.protonull.civianmod.config;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.LongSliderControllerBuilder;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.features.ClickRailDest;

public final class PolyfillsSettings {
    @SerialEntry
    public boolean clickDestEnabled = ClickRailDest.DEFAULT_ENABLED;
    @SerialEntry
    public long clickDestCooldown = ClickRailDest.DEFAULT_COOLDOWN;

    void apply() {
        ClickRailDest.enabled = this.clickDestEnabled;
        ClickRailDest.cooldown = this.clickDestCooldown;
    }

    // ============================================================
    // Screen generation
    // ============================================================

    static @NotNull OptionGroup generateGroup(
        final @NotNull PolyfillsSettings settings
    ) {
        return OptionGroup.createBuilder()
            .name(Component.translatable("civianmod.config.group.polyfills"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.polyfills.desc")))
            .collapsed(true)
            .option(generateClickDestEnabled(settings))
            .option(generateClickDestCooldown(settings))
            .build();
    }

    private static @NotNull Option<?> generateClickDestEnabled(
        final @NotNull PolyfillsSettings settings
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.group.polyfills.feature.click-dest.enabled"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.polyfills.feature.click-dest.enabled.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                ClickRailDest.DEFAULT_ENABLED,
                () -> settings.clickDestEnabled,
                (enabled) -> settings.clickDestEnabled = enabled
            )
            .build();
    }

    private static @NotNull Option<?> generateClickDestCooldown(
        final @NotNull PolyfillsSettings settings
    ) {
        return Option.<Long>createBuilder()
            .name(Component.translatable("civianmod.config.group.polyfills.feature.click-dest.cooldown"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.polyfills.feature.click-dest.cooldown.desc")))
            .controller((opt) -> LongSliderControllerBuilder.create(opt)
                .range(0L, 3000L)
                .step(20L)
                .formatValue((val) -> Component.translatable("civianmod.config.group.polyfills.feature.click-dest.cooldown.formatter", val))
            )
            .binding(
                ClickRailDest.DEFAULT_COOLDOWN,
                () -> settings.clickDestCooldown,
                (cooldown) -> settings.clickDestCooldown = cooldown
            )
            .build();
    }
}
