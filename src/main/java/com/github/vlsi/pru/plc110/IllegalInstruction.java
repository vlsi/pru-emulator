package com.github.vlsi.pru.plc110;

public class IllegalInstruction extends Instruction {
  public IllegalInstruction(int code) {
    super(code);
  }

  @Override
  public String toString() {
    return "IllegalInstruction: 0x" + Integer.toUnsignedString(code);
  }
}
