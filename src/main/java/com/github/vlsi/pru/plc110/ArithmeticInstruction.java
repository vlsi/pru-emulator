package com.github.vlsi.pru.plc110;

public class ArithmeticInstruction extends Instruction {
  public enum Operation {
    ADD,
    ADC,
    SUB,
    SUC,
    LSL,
    LSR,
    RSB,
    RSC,
    AND,
    OR,
    XOR,
    NOT,
    MIN,
    MAX,
    CLR,
    SET
  }

  private final static Operation[] ops = Operation.values();

  public final Operation operation;
  public final Register dstRegister;
  public final Register srcRegister;
  public final int op2;
  public final boolean op2IsRegister;

  public ArithmeticInstruction(
      Operation op,
      Register dstRegister,
      Register srcRegister,
      boolean op2IsRegister,
      byte op2) {
    super((op.ordinal() << 25)
        | (op2IsRegister ? 0 : 1 << 24)
        | ((op2 & 0xff) << 16)
        | (srcRegister.mask() << 8)
        | dstRegister.mask());
    this.operation = op;
    this.dstRegister = dstRegister;
    this.srcRegister = srcRegister;
    this.op2IsRegister = op2IsRegister;
    this.op2 = op2 & 0xff;
  }

  public ArithmeticInstruction(int code) {
    this(ops[(code >> 25) & 0xf],
        // dstRegister
        Register.ofMask(code & 0xff),
        // srcRegister
        Register.ofMask((code >> 8) & 0xff),
        // op2IsRegister
        (code & (1 << 24)) == 0,
        // op2
        (byte) ((code >> 16) & 0xff)
    );
    assert (code >>> 29) == 0 : "Instruction format should be 0b000, given "
        + Integer.toBinaryString(code >>> 29);
  }

  public ArithmeticInstruction(
      Operation op,
      Register dstRegister,
      Register srcRegister,
      int op2) {
    this(op, dstRegister, srcRegister, false, (byte) op2);
  }

  public ArithmeticInstruction(
      Operation op,
      Register dstRegister,
      Register srcRegister,
      Register op2Register) {
    this(op, dstRegister, srcRegister, true,
        (byte) op2Register.mask());
  }

  @Override
  public String toString() {
    return operation + " " +
        dstRegister +
        ", " + srcRegister +
        ", " + (op2IsRegister ? Register.ofMask(op2).toString() : op2) +
        commentToString();
  }
}
