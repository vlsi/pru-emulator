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
  public final int dstRegister;
  public final RegisterField dstField;
  public final int srcRegister;
  public final int op2;
  public final boolean op2IsRegister;

  public ArithmeticInstruction(
      Operation op,
      int dstRegister, RegisterField dstField,
      int srcRegister, RegisterField srcField,
      boolean op2IsRegister,
      byte op2) {
    super((op.ordinal() << 25)
        | (op2IsRegister ? 0 : 1 << 24)
        | ((op2 & 0xff) << 16)
        | (srcField.fullMask(srcRegister) << 8)
        | dstField.fullMask(dstRegister));
    this.operation = op;
    this.dstRegister = dstField.fullMask(dstRegister);
    this.dstField = dstField;
    this.srcRegister = srcField.fullMask(srcRegister);
    this.op2IsRegister = op2IsRegister;
    this.op2 = op2 & 0xff;
  }

  public ArithmeticInstruction(int code) {
    this(ops[(code >> 25) & 0xf],
        // dstRegister
        code & 31, RegisterField.ofMask(code >> 5),
        // srcRegister
        (code >> 8) & 31, RegisterField.ofMask(code >> (8 + 5)),
        // op2IsRegister
        (code & (1 << 24)) == 0,
        // op2
        (byte) ((code >> 16) & 0xff)
    );
    assert (code >>> 29) == 0 : "Instruction format should be 0b000";
  }

  public ArithmeticInstruction(
      Operation op,
      int dstRegister, RegisterField dstField,
      int srcRegister, RegisterField srcField,
      byte op2) {
    this(op, dstRegister, dstField, srcRegister, srcField, false, op2);
  }

  public ArithmeticInstruction(
      Operation op,
      int dstRegister, RegisterField dstField,
      int srcRegister, RegisterField srcField,
      int op2Register, RegisterField op2Field) {
    this(op, dstRegister, dstField, srcRegister, srcField, true,
        (byte) op2Field.fullMask(op2Register));
  }

  @Override
  public String toString() {
    return "ArithmeticInstruction{" +
        "operation=" + operation +
        ", dstRegister=" + RegisterField.fullName(dstRegister) +
        ", srcRegister=" + RegisterField.fullName(dstRegister) +
        ", op2=" + (op2IsRegister ? RegisterField.fullName(op2) : op2) +
        ", op2IsRegister=" + op2IsRegister +
        '}';
  }
}
