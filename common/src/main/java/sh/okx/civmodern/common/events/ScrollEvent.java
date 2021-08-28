package sh.okx.civmodern.common.events;

public class ScrollEvent implements Event {

  private final boolean up;

  public ScrollEvent(boolean up) {
    this.up = up;
  }

  public boolean isUp() {
    return up;
  }
}
