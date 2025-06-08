package sh.okx.civmodern.common.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sh.okx.civmodern.common.parser.ParsedWaypoint;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatComponent.class)
public class ChatMixin {
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    protected Component chatMixinReplace(Component original,
                                         @Local(argsOnly = true) LocalRef<MessageSignature> signatureRef,
                                         @Local(argsOnly = true) LocalRef<GuiMessageTag> tagRef) {

        List<Component> flatList = original.toFlatList();

        List<Component> output = new ArrayList<>();

        for (Component component : flatList) {
            if (component.getContents() instanceof PlainTextContents plainTextContents) {
                String text = plainTextContents.text();

                List<ParsedWaypoint> waypoints = ParsedWaypoint.parseWaypoints(text);
                if (waypoints.isEmpty()) {
                    output.add(component);
                    continue;
                }

                int lastPos = 0;
                for (ParsedWaypoint waypoint : waypoints) {
                    output.add(Component.literal(text.substring(lastPos, waypoint.textPosStart())).setStyle(component.getStyle()));
                    String waypointText = text.substring(waypoint.textPosStart(), waypoint.textPosEnd());
                    output.add(Component.literal(text)
                        .withStyle(s -> component.getStyle()
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/civmodern_openwaypoint " + waypointText))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to set highlighted waypoint\nControl click to add a new waypoint")))));
                    lastPos = waypoint.textPosEnd();
                }

                output.add(Component.literal(text.substring(lastPos)).setStyle(component.getStyle()));
            } else {
                output.add(component);
            }
        }

        MutableComponent result = Component.empty();
        result.getSiblings().addAll(output);
        return result;
    }
    // [name:hello,x:1,y:1,z:1] wfwefew[name:hello,x:1,y:1,z:2]
}
