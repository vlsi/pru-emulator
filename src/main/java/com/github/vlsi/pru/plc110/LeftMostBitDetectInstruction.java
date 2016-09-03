package com.github.vlsi.pru.plc110;

public class LeftMostBitDetectInstruction extends Format2Instruction {

  public final int dstRegister;
  public final RegisterField dstField;
  public final int srcRegister;
  public final int op2;
  public final boolean op2IsRegister;

  public LeftMostBitDetectInstruction(
      int dstRegister, RegisterField dstField,
      int srcRegister, RegisterField srcField,
      boolean op2IsRegister,
      byte op2) {
    super(Operation.LMBD,
        (op2IsRegister ? 0 : 1 << 24)
            | ((op2 & 0xff) << 16)
            | (srcField.fullMask(srcRegister) << 8)
            | dstField.fullMask(dstRegister));
    this.dstRegister = dstField.fullMask(dstRegister);
    this.dstField = dstField;
    this.srcRegister = srcField.fullMask(srcRegister);
    this.op2IsRegister = op2IsRegister;
    this.op2 = op2 & 0xff;
  }

  public LeftMostBitDetectInstruction(int code) {
    super(Operation.LMBD, code);
    assert (code >>> 25) == 0b001_0011 : "Instruction format should be 0b001_0011, was "
        + Integer.toBinaryString(code >>> 25) ;
    this.dstRegister = code & 0xff;
    this.dstField = RegisterField.ofMask(dstRegister >> 5);
    this.srcRegister = (code >> 8) & 0xff;
    this.op2IsRegister = (code & (1 << 24)) == 0;
    this.op2 = (code >> 16) & 0xff;
  }

  public LeftMostBitDetectInstruction(
      int dstRegister, RegisterField dstField,
      int srcRegister, RegisterField srcField,
      byte op2) {
    this(dstRegister, dstField, srcRegister, srcField, false, op2);
  }

  public LeftMostBitDetectInstruction(
      int dstRegister, RegisterField dstField,
      int srcRegister, RegisterField srcField,
      int op2Register, RegisterField op2Field) {
    this(dstRegister, dstField, srcRegister, srcField, true,
        (byte) op2Field.fullMask(op2Register));
  }

  @Override
  public String toString() {
    return op + " " +
        RegisterField.fullName(dstRegister) +
        ", " + RegisterField.fullName(srcRegister) +
        ", " + (op2IsRegister ? RegisterField.fullName(op2) : op2);
  }
}
