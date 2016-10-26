package com.github.vlsi.pru.plc110;

public class Label {
  public enum LabelType {
    NON_INITIALIZED, RELATIVE, ABSOLUTE
  }
  private final String name;
  private int offset;
  private LabelType type = LabelType.NON_INITIALIZED;

  public Label(String name) {
    this.name = name;
  }

  public boolean isInitialized() {
    return type != LabelType.NON_INITIALIZED;
  }

  public LabelType getType() {
    return type;
  }

  public int computeOffsetRelativeTo(int sourceOffset) {
    switch (type) {
      case ABSOLUTE:
        return offset - sourceOffset;
      case RELATIVE:
        return offset;
      default:
        throw new IllegalStateException("Label " + name + " is not initialized");
    }
  }

  public Label setRelativeOffset(int offset) {
    type = LabelType.RELATIVE;
    this.offset = offset;
    return this;
  }

  public int getRelativeOffset() {
    switch (type) {
      case ABSOLUTE:
        throw new IllegalStateException(
            "getRelativeOffset should not be used for absolute labels. Label name is " + name);
      case RELATIVE:
        return offset;
      default:
        throw new IllegalStateException("Label " + name + " is not initialized");
    }
  }

  public Label setAbsoluteOffset(int offset) {
    type = LabelType.ABSOLUTE;
    this.offset = offset;
    return this;
  }

  public int getAbsoluteOffset() {
    switch (type) {
      case ABSOLUTE:
        return offset;
      case RELATIVE:
        throw new IllegalStateException(
            "getAbsoluteOffset should not be used for relative labels. Label name is " + name);
      default:
        throw new IllegalStateException("Label " + name + " is not initialized");
    }
  }
}
