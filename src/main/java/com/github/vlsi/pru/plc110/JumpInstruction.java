package com.github.vlsi.pru.plc110;

public class JumpInstruction extends Format2Instruction {
  public final int dstRegister;
  public final int op2;
  public final boolean op2IsRegister;

  private JumpInstruction(
      Operation op,
      boolean op2IsRegister, int op2,
      int dstRegister, RegisterField dstField
  ) {
    super(op, (op2IsRegister ? 0 : 1 << 24)
        | (op2IsRegister ? ((op2 & 0xff) << 16) : ((op2 & 0xffff) << 8))
        | dstField.fullMask(dstRegister)
    );
    assert op == Operation.JAL || op == Operation.JMP
        : "op should be JAL or JMP. Given " + op;
    this.dstRegister = dstField.fullMask(dstRegister);
    this.op2 = op2;
    this.op2IsRegister = op2IsRegister;
  }

  public JumpInstruction(int code) {
    super(((code >> 25) & 15) == 0 ? Operation.JMP : Operation.JAL, code);
    this.op2IsRegister = (code & (1 << 24)) == 0;
    if (this.op2IsRegister) {
      this.op2 = (code >> 8) & 0xffff;
    } else {
      this.op2 = (code >> 16) & 0xff;
    }
    this.dstRegister = code & 0xff;
  }

  private JumpInstruction(
      Operation op,
      int op2Register, RegisterField op2Field,
      int dstRegister, RegisterField dstField
  ) {
    this(op, true, (byte) op2Field.fullMask(op2Register), dstRegister, dstField);
  }

  private JumpInstruction(
      Operation op,
      int op2,
      int dstRegister, RegisterField dstField
  ) {
    this(op, false, op2, dstRegister, dstField);
  }

  @Override
  public String toString() {
    return op + " " +
        (op == Operation.JAL ? ", dstRegister=" + RegisterField.fullName(dstRegister) : "") +
        ", " + (op2IsRegister ? RegisterField.fullName(op2) : op2);
  }
}