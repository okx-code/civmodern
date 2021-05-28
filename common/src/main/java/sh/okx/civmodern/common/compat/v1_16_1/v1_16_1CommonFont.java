package sh.okx.civmodern.common.compat.v1_16_1;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.okx.civmodern.common.compat.CommonFont;

public class v1_16_1CommonFont implements CommonFont {

  private static final Logger LOGGER = LogManager.getLogger();

  private final Font font;
  private Method drawInternalMethod;

  public v1_16_1CommonFont(Font font) {
    this.font = font;

    Class<?>[] parameterTypes = {FormattedText.class, float.class, float.class, int.class, com.mojang.math.Matrix4f.class, boolean.class};
    for (Method method : font.getClass().getDeclaredMethods()) {
      LOGGER.info(method);
      if (Arrays.equals(method.getParameterTypes(), parameterTypes)) {
        method.setAccessible(true);
        this.drawInternalMethod = method;
        LOGGER.info("Font.drawInternal: " + drawInternalMethod);
        break;
      }
    }
  }

  private void drawInternal(PoseStack stack, Component component, float x, float y, int colour, boolean shadow) {
    try {
      drawInternalMethod.invoke(font, component, x, y, colour, stack.last().pose(), shadow);
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void draw(PoseStack poseStack, Component component, float x, float y, int colour) {
    drawInternal(poseStack, component, x, y, colour, false);
  }

  @Override
  public void drawShadow(PoseStack poseStack, Component component, float x, float y, int colour) {
    drawInternal(poseStack, component, x, y, colour, true);
  }

  @Override
  public void drawShadowCentred(PoseStack poseStack, Component component, float x, float y, int colour) {
    drawShadow(poseStack, component, x - font.width(component) / 2f, y, colour);
  }
}