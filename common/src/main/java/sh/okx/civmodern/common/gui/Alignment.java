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
    return switch (this) {
      case TOP_LEFT -> TOP_RIGHT;
      case TOP_RIGHT -> BOTTOM_RIGHT;
      case BOTTOM_RIGHT -> BOTTOM_LEFT;
      default -> TOP_LEFT;
    };
  }
}
