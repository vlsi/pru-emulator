package com.github.vlsi.pru.plc110;

import com.github.vlsi.pru.plc110.debug.RegisterVariableLocation;

import java.util.ArrayList;
import java.util.List;

public class CodeEmitter {
  final List<Instruction> result = new ArrayList<>();
  final List<Integer> jumpInstructions = new ArrayList<>();
  final List<RegisterVariableLocation> varLocations = new ArrayList<>();

  public void visitInstruction(Instruction instruction) {
    if (instruction instanceof Jump) {
      if (((Jump) instruction).getTarget() != null) {
        jumpInstructions.add(result.size());
      }
    }
    result.add(instruction);
  }

  public void visitLabel(Label label) {
    label.setAbsoluteOffset(result.size());
  }

  public void visitRegisterVariable(String name, String typeName, Label start, Label end, Register reg) {
    varLocations.add(new RegisterVariableLocation(name, typeName, start, end, reg));
  }

  public BinaryCode visitEnd() {
    for (Integer i : jumpInstructions) {
      Jump jump = (Jump) result.get(i);
      Label target = jump.getTarget();
      if (!target.isInitialized()) {
        throw new IllegalStateException("Unresolved jump target " + target + " for jump instruction " + jump);
      }
      jump.resolveTarget(i);
    }
    for (RegisterVariableLocation var : varLocations) {
      if (!var.start.isInitialized()) {
        throw new IllegalStateException("Unresolved start location for variable " + var.name);
      }
      if (!var.end.isInitialized()) {
        throw new IllegalStateException("Unresolved end location for variable " + var.name);
      }
    }
    return new BinaryCode(result, varLocations);
  }
}
