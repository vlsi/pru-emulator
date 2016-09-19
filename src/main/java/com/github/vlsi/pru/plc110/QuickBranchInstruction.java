package com.github.vlsi.pru.plc110;

public class QuickBranchInstruction extends Instruction implements Jump {
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
  public short offset;
  public final boolean op2IsRegister;
  public final int op2;
  public final Register srcRegister;
  public final Label target;

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
    this.op2 = (code >> 16) & 0xff;
    this.srcRegister = Register.ofMask((code >> 8) & 0xff);
    this.target = null;
  }

  private QuickBranchInstruction(
      Operation op,
      Label target, Register srcRegister,
      boolean op2IsRegister, byte op2) {
    super(updateOffset(op.header()
            | (op2IsRegister ? 0 : 1 << 24)
            | ((op2 & 0xff) << 16)
            | (srcRegister.mask() << 8),
        (short) (target.getOffset() == -1 ? 0 : target.getOffset())));
    assert offset < 1 << 10 && offset >= -1 << 10
        : "Branch offset should fit into 10 bits. Given " + offset;
    this.operation = op;
    this.offset = (short) (target.getOffset() == -1 ? 0 : target.getOffset());
    this.op2IsRegister = op2IsRegister;
    this.op2 = op2 & 0xff;
    this.srcRegister = srcRegister;
    this.target = target;
  }

  public QuickBranchInstruction(
      Operation op, Label target,
      Register srcRegister,
      int op2) {
    this(op, target, srcRegister, false, (byte) op2);
  }

  public QuickBranchInstruction(
      Operation op, Label target,
      Register srcRegister,
      Register op2Register) {
    this(op, target, srcRegister, true,
        (byte) op2Register.mask());
  }

  public QuickBranchInstruction(
      Label target) {
    this(Operation.A, target, new Register(1, RegisterField.dw), false, (byte) 0);
  }

  private static int updateOffset(int code, short offset) {
    return (code & ~(0b11 << 25 | 0xff))
        | (((offset >> 8) & 0b11) << 25)
        | (offset & 0xff);
  }

  public void setOffset(short offset) {
    this.code = updateOffset(code, offset);
    this.offset = offset;
  }

  @Override
  public Label getTarget() {
    return target;
  }

  @Override
  public void resolveTarget(int sourceOffset) {
    if (target == null) {
      return;
    }
    setOffset((short) (target.getOffset() - sourceOffset));
  }

  public boolean isBitTest() {
    return operation.ordinal() >= Operation.BS.ordinal();
  }

  @Override
  public String toString() {
    return "QB" + operation + " " +
        "offset=" + offset +
        ", " + srcRegister +
        ", " + (op2IsRegister ? Register.ofMask(op2).toString() : op2) +
        commentToString();
  }
}
