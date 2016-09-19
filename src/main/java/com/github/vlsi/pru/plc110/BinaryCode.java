package com.github.vlsi.pru.plc110;

import com.github.vlsi.pru.plc110.debug.RegisterVariableLocation;

import java.util.Collections;
import java.util.List;

public class BinaryCode {
  private final List<Instruction> ins;
  private final List<RegisterVariableLocation> varLocations;

  public BinaryCode(List<Instruction> ins, List<RegisterVariableLocation> varLocations) {
    this.ins = Collections.unmodifiableList(ins);
    this.varLocations = Collections.unmodifiableList(varLocations);
  }

  public List<Instruction> getInstructions() {
    return ins;
  }

  public List<RegisterVariableLocation> getVarLocations() {
    return varLocations;
  }
}
