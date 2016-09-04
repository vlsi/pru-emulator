package com.github.vlsi.pru.plc110;

public class LeftMostBitDetectInstruction extends Format2Instruction {

  public final Register dstRegister;
  public final Register srcRegister;
  public final int op2;
  public final boolean op2IsRegister;

  public LeftMostBitDetectInstruction(
      Register dstRegister,
      Register srcRegister,
      boolean op2IsRegister,
      byte op2) {
    super(Operation.LMBD,
        (op2IsRegister ? 0 : 1 << 24)
            | ((op2 & 0xff) << 16)
            | (srcRegister.mask() << 8)
            | dstRegister.mask());
    this.dstRegister = dstRegister;
    this.srcRegister = srcRegister;
    this.op2IsRegister = op2IsRegister;
    this.op2 = op2 & 0xff;
  }

  public LeftMostBitDetectInstruction(int code) {
    super(Operation.LMBD, code);
    assert (code >>> 25) == 0b001_0011 : "Instruction format should be 0b001_0011, was "
        + Integer.toBinaryString(code >>> 25) ;
    this.dstRegister = Register.ofMask(code & 0xff);
    this.srcRegister = Register.ofMask((code >> 8) & 0xff);
    this.op2IsRegister = (code & (1 << 24)) == 0;
    this.op2 = (code >> 16) & 0xff;
  }

  public LeftMostBitDetectInstruction(
      Register dstRegister,
      Register srcRegister,
      byte op2) {
    this(dstRegister, srcRegister, false, op2);
  }

  public LeftMostBitDetectInstruction(
      Register dstRegister,
      Register srcRegister,
      Register op2Register) {
    this(dstRegister, srcRegister, true,
        (byte) op2Register.mask());
  }

  @Override
  public String toString() {
    return op + " " +
        dstRegister +
        ", " + srcRegister +
        ", " + (op2IsRegister ? Register.ofMask(op2).toString() : op2);
  }
}
