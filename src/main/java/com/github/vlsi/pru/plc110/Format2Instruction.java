package com.github.vlsi.pru.plc110;

public class Format2Instruction extends Instruction {
  public enum Operation {
    JMP(0),
    JAL(1),
    LDI(2),
    LMBD(3),
    SCAN(4),
    HALT(5),
    SLP(15);

    public final int value;

    Operation(int value) {
      this.value = value;
    }
  }

  public final Operation op;

  public Format2Instruction(Operation op, int code) {
    super((1<<29) // format 2
        | (op.value << 25)
        | code);
    this.op = op;
  }

  public static Format2Instruction of(int code) {
    int op = (code >> 25) & 15;
    switch (op) {
      case 2:
        return new LdiInstruction(code);

      default:
        throw new IllegalArgumentException("Unsupported format 2 instruction code " + op);
    }
  }
}
