package com.github.vlsi.pru.plc110;

public class QuickBranchInstruction extends Instruction {
  private final static Operation[] VALUES_4 = Operation.values();
  private final static Operation[] VALUES_5 =
      {Operation.NEVER, Operation.BC, Operation.BS, Operation.A};

  public enum Operation {
    NEVER,
    // Format 4a or 4b
    LT,
    EQ,
    LE,
    GT,
    NE,
    GE,
    A,
    // Format 5a or 5b
    BS,
    BC;

    public int header() {
      if (this.compareTo(A) <= 0) {
        // 4a or 4b
        return (1 << 30) | (ordinal() << 27);
      }
      return (0b110 << 29) | ((this == BS ? 0b10 : 0b01) << 27);
    }

    static Operation get(boolean gt, boolean eq, boolean lt) {
      return VALUES_4[(gt ? 4 : 0) | (eq ? 2 : 0) | (lt ? 1 : 0)];
    }
  }

  public final Operation operation;
  public final short offset;
  public final boolean op2IsRegister;
  public final byte op2;
  public final int srcRegister;

  public QuickBranchInstruction(int code) {
    super(code);
    if ((code >>> 30) == 0b01) {
      // Format 4a or 4b
      this.operation = VALUES_4[(code >> 27) & 0b111];
    } else {
      // Format 5a or 5b
      this.operation = VALUES_5[(code >> 27) & 0b11];
    }
    short offset = (short) ((((code >> 25) & 0b11) << 8) | code & 0xff);
    if ((offset & 1 << 9) != 0) {
      // When 10-bit offset is negative, pad the rest bits so we get signed short
      offset |= 0b111111 << 10;
    }
    this.offset = offset;
    this.op2IsRegister = (code & (1 << 24)) == 0;
    this.op2 = (byte) ((code >> 16) & 0xff);
    this.srcRegister = (code >> 8) & 0xff;
  }

  private QuickBranchInstruction(
      Operation op,
      short offset, int srcRegister,
      RegisterField srcField, boolean op2IsRegister, byte op2) {
    super(op.header()
        | (((offset >> 8) & 3) << 25)
        | (op2IsRegister ? 0 : 1 << 24)
        | ((op2 & 0xff) << 16)
        | (srcField.fullMask(srcRegister) << 8)
        | (offset & 0xff));
    assert offset < 1 << 10 && offset >= -1 << 10
        : "Branch offset should fit into 10 bits. Given " + offset;
    this.operation = op;
    this.offset = offset;
    this.op2IsRegister = op2IsRegister;
    this.op2 = op2;
    this.srcRegister = srcField.fullMask(srcRegister);
  }

  public QuickBranchInstruction(
      Operation op, short offset,
      int srcRegister, RegisterField srcField,
      byte op2) {
    this(op, offset, srcRegister, srcField, false, op2);
  }

  public QuickBranchInstruction(
      Operation op, short offset,
      int srcRegister, RegisterField srcField,
      int op2Register, RegisterField op2Field) {
    this(op, offset, srcRegister, srcField, true,
        (byte) op2Field.fullMask(op2Register));
  }

  public QuickBranchInstruction(
      short offset) {
    this(Operation.A, offset, 1, RegisterField.dw, false, (byte) 0);
  }

  public boolean isBitTest() {
    return operation.ordinal() >= Operation.BS.ordinal();
  }

  @Override
  public String toString() {
    return "QB" + operation + " " +
        "offset=" + offset +
        ", " + RegisterField.fullName(srcRegister) +
        ", " + (op2IsRegister ? RegisterField.fullName(op2) : op2);
  }
}
