package com.github.vlsi.pru.plc110;

public abstract class Instruction {
  public int code;

  public Instruction(int code) {
    this.code = code;
  }

  public static Instruction of(int code) {
    if ((code >>> 30) == 0b01) {
      // Format 4a or 4b
      return new QuickBranchInstruction(code);
    }

    int format = code >>> 29;
    switch (format) {
      case 0b000:
        return new ArithmeticInstruction(code);
      case 0b001:
        return Format2Instruction.of(code);
      case 0b110:
        // Format 5a or 5b
        return new QuickBranchInstruction(code);
      case 0b111:
      case 0b100:
        // Format 6a, 6b, 6c, or 6d
        return new MemoryTransferInstruction(code);
    }
    throw new IllegalArgumentException("Unknown instruction " + Integer.toHexString(code)
        + ", format " + Integer.toBinaryString(format));
  }
}
