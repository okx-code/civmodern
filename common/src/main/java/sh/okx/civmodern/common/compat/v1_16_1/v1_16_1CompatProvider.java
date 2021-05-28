package sh.okx.civmodern.common.compat.v1_16_1;

import net.minecraft.client.gui.Font;
import sh.okx.civmodern.common.compat.CommonFont;
import sh.okx.civmodern.common.compat.CompatProvider;

public class v1_16_1CompatProvider implements CompatProvider {

  @Override
  public CommonFont provideFont(Font font) {
    return new v1_16_1CommonFont(font);
  }
}
