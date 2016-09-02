package com.github.vlsi.pru.plc110;

public abstract class Instruction {
  public final int code;

  public Instruction(int code) {
    this.code = code;
  }

  public static Instruction of(int code) {
    int format = code >>> 29;
    switch (format) {
      case 0:
        return new ArithmeticInstruction(code);
      case 1:
        return Format2Instruction.of(code);
    }
    throw new IllegalArgumentException("Unknown instruction " + Integer.toHexString(code)
        + ", format " + Integer.toBinaryString(format));
  }
}
