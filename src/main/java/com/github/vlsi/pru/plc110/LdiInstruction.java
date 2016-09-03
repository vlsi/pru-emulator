package com.github.vlsi.pru.plc110;

public class LdiInstruction extends Format2Instruction {
  public final int dstRegister;
  public final short value;

  public LdiInstruction(
      int dstRegister, RegisterField dstField,
      short value) {
    super(Operation.LDI,
        ((value & 0xffff) << 8)
            | dstField.fullMask(dstRegister));
    this.dstRegister = dstField.fullMask(dstRegister);
    this.value = value;
  }

  public LdiInstruction(int code) {
    this(
        // dstRegister
        code & 31, RegisterField.ofMask(code >> 5),
        (short) ((code >> 8) & 0xffff)
    );
  }

  @Override
  public String toString() {
    return op + " " +
        RegisterField.fullName(dstRegister) +
        ", " + value;
  }
}
