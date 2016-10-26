package com.github.vlsi.pru.plc110;

public class HaltInstruction extends Format2Instruction {
  public HaltInstruction() {
    super(Operation.HALT, 0);
  }

  @Override
  public String toString() {
    return op +
        commentToString();
  }
}
