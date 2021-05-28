package sh.okx.civmodern.common.compat;

import net.minecraft.client.gui.Font;

public interface CompatProvider {
  CommonFont provideFont(Font font);
}
