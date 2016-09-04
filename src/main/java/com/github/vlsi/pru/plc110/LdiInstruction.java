package com.github.vlsi.pru.plc110;

public class LdiInstruction extends Format2Instruction {
  public final Register dstRegister;
  public final short value;

  public LdiInstruction(
      Register dstRegister,
      short value) {
    super(Operation.LDI,
        ((value & 0xffff) << 8)
            | dstRegister.mask());
    this.dstRegister = dstRegister;
    this.value = value;
  }

  public LdiInstruction(int code) {
    this(
        // dstRegister
        Register.ofMask(code & 0xff),
        (short) ((code >> 8) & 0xffff)
    );
  }

  @Override
  public String toString() {
    return op + " " +
        dstRegister +
        ", " + value;
  }
}
