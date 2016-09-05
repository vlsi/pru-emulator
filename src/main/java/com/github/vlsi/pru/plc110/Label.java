package com.github.vlsi.pru.plc110;

public class Label {
  private final String name;
  private int offset;

  public Label(String name) {
    this(name, -1);
  }

  public Label(String name, int offset) {
    this.name = name;
    this.offset = offset;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }
}
