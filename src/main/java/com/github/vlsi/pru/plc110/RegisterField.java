package com.github.vlsi.pru.plc110;

public enum RegisterField {
  b0(8),
  b1(8),
  b2(8),
  b3(8),
  w0(16),
  w1(16),
  w2(16),
  dw(32);

  private static final RegisterField[] VALUES = RegisterField.values();

  private final int bitWidth;
  private final int bitMask;

  RegisterField(int bitWidth) {
    this.bitWidth = bitWidth;
    this.bitMask = (1 << bitWidth) - 1;
  }

  public int getBitWidth() {
    return bitWidth;
  }

  public int getBitMask() {
    return bitMask;
  }

  public static RegisterField ofMask(int mask) {
    return VALUES[mask & 7];
  }

  public int toMask() {
    return ordinal();
  }

  public int fullMask(int registerIndex) {
    return registerIndex | (toMask() << 5);
  }

  public static String fullName(int register) {
    return RegisterField.ofMask(register >> 5).toString(register & 31);
  }

  public String toString(int registerIndex) {
    if (this == dw) {
      return "R" + registerIndex;
    }
    return "R" + registerIndex + "." + name();
  }

  public static RegisterField b(int field) {
    assert field >= 0 || field <= 3 : "r8 field should be either b0, b1, b2, or b3. Trying to access field " + field;
    return VALUES[field];
  }

  public static RegisterField w(int field) {
    assert field >= 0 || field <= 2 : "r16 field should be either w0, w1, or w2. Trying to access field " + field;
    return VALUES[field + 4];
  }

}
