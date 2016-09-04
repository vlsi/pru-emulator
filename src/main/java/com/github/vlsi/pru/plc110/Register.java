package com.github.vlsi.pru.plc110;

public class Register {
  private int index;
  private RegisterField field;

  public Register(int index, RegisterField field) {
    this.index = index;
    this.field = field;
  }

  public static Register ofMask(int mask) {
    assert mask >=0 && mask <= 255 : "Register mask should be [0..255]. Given " + mask;
    return new Register(mask & 31, RegisterField.ofMask(mask >>> 5));
  }

  public int index() {
    return index;
  }

  public RegisterField field() {
    return field;
  }

  public int mask() {
    return index | (field.toMask() << 5);
  }

  public Register withField(RegisterField field) {
    if (this.field == field) {
      return this;
    }
    return new Register(index(), field);
  }

  @Override
  public String toString() {
    return field.toString(index);
  }
}
