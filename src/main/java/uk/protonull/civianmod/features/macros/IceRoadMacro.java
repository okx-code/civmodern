package uk.protonull.civianmod.features.macros;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.config.CivianModConfig;
import uk.protonull.civianmod.config.IceRoadSettings;
import uk.protonull.civianmod.events.ClientTickEvent;

public class IceRoadMacro {
    private final KeyMapping key;
    // alternate between true and false for maximum jumpage
    private boolean enabled = false;
    private boolean jump = false;
    private boolean waitingForFood = false;

    private ItemStack eating;

    public IceRoadMacro(KeyMapping key) {
        this.key = key;
    }

    @Subscribe
    private void tick(
        final @NotNull ClientTickEvent event
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        final IceRoadSettings settings = CivianModConfig.HANDLER.instance().iceRoadSettings;

        while (this.key.consumeClick()) {
            if (enabled) {
                mc.options.keySprint.setDown(false);
                mc.options.keyUp.setDown(false);
                if (jump) {
                    jump = false;
                    if (!mc.player.isPassenger()) {
                        mc.options.keyJump.setDown(false);
                    }
                }
                mc.options.keyUse.setDown(false);
                waitingForFood = false;
                eating = null;
                enabled = false;
            } else {
                if (settings.snapYaw) {
                    float roty = Math.round(mc.player.getYRot() / 45) * 45;
                    mc.player.setYRot(roty);
                }
                if (settings.snapPitch) {
                    float rotx = Math.round(mc.player.getXRot() / 45) * 45;
                    mc.player.setXRot(rotx);
                }

                enabled = true;
            }
        }

        if (enabled) {
            if (!jump) {
                AUTO_EAT:
                if (settings.autoEat) {
                    if (this.eating != null) {
                        if (!mc.player.isUsingItem() || !this.eating.equals(mc.player.getUseItem())) {
                            this.eating = null;
                            mc.options.keyUse.setDown(false);
                        } else {
                            break AUTO_EAT;
                        }
                    }

                    ItemStack mainhand = mc.player.getMainHandItem();
                    if (tryEat(mainhand)) {
                        this.eating = mainhand;
                        mc.options.keyUse.setDown(true);
                        return;
                    }
                }

                if (settings.stopWhenHungry && mc.player.getFoodData().getFoodLevel() <= 6) {
                    waitingForFood = true;
                    mc.options.keyUp.setDown(false);
                    return;
                } else if (waitingForFood) {
                    waitingForFood = false;
                }

                if (!mc.player.isPassenger()) {
                    mc.options.keyJump.setDown(true);
                }
                jump = true;
            } else {
                if (!mc.player.isPassenger()) {
                    mc.options.keyJump.setDown(false);
                }
                jump = false;
            }
            mc.options.keySprint.setDown(true);
            mc.options.keyUp.setDown(true);
        }
    }

    private boolean tryEat(ItemStack item) {
        Minecraft mc = Minecraft.getInstance();

        FoodProperties food = item.getItem().components().get(DataComponents.FOOD);
        if (food != null && food.nutrition() > 0) {
            if (mc.player.getFoodData().getFoodLevel() + food.nutrition() <= 20) {
                return true;
            }
        }
        return false;
    }
}
