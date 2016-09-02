package com.github.vlsi.pru.plc110;

import java.nio.ByteBuffer;

public class Register {
  private final ByteBuffer dst;
  private int index;

  public Register(ByteBuffer dst, int index) {
    this.dst = dst;
    this.index = index;
  }

  public int get() {
    return dst.getInt(index * 4);
  }

  public void set(int value) {
    dst.putInt(index * 4, value);
  }

  public short getR16(int field) {
    assert field >= 0 || field <= 2 : "r16 field should be either w0, w1, or w2. Trying to access field " + field;
    return dst.getShort(index * 4 + field);
  }

  public void setR16(int field, short value) {
    assert field >= 0 || field <= 2 : "r16 field should be either w0, w1, or w2. Trying to access field " + field;
    dst.putInt(index * 4 + field, value);
  }

  public byte getR8(int field) {
    assert field >= 0 || field <= 3 : "r8 field should be either b0, b1, b2, or b3. Trying to access field " + field;
    return dst.get(index * 4 + field);
  }

  public void setR8(int field, byte value) {
    assert field >= 0 || field <= 3 : "r8 field should be either b0, b1, b2, or b3. Trying to access field " + field;
    dst.put(index * 4 + field, value);
  }
}
