package sh.okx.civmodern.common.gui;

public enum Alignment {
  TOP_LEFT("Top Left"),
  TOP_RIGHT("Top Right"),
  BOTTOM_RIGHT("Bottom Right"),
  BOTTOM_LEFT("Bottom Left");

  private final String name;

  Alignment(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public Alignment next() {
    switch (this) {
      case TOP_LEFT:
        return TOP_RIGHT;
      case TOP_RIGHT:
        return BOTTOM_RIGHT;
      case BOTTOM_RIGHT:
        return BOTTOM_LEFT;
      default:
        return TOP_LEFT;
    }
  }
}
