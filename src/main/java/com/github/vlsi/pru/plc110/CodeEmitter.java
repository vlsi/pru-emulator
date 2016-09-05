package com.github.vlsi.pru.plc110;

import java.util.ArrayList;
import java.util.List;

public class CodeEmitter {
  final List<Instruction> result = new ArrayList<>();
  final List<Integer> jumpInstructions = new ArrayList<>();

  public void visitInstruction(Instruction instruction) {
    if (instruction instanceof Jump) {
      if (((Jump) instruction).getTarget() != null) {
        jumpInstructions.add(result.size());
      }
    }
    result.add(instruction);
  }

  public void visitLabel(Label label) {
    label.setOffset(result.size());
  }

  public List<Instruction> visitEnd() {
    for (Integer i : jumpInstructions) {
      Jump jump = (Jump) result.get(i);
      Label target = jump.getTarget();
      if (target.getOffset() == -1) {
        throw new IllegalStateException("Unresolved jump target " + target + " for jump instruction " + jump);
      }
      jump.resolveTarget(i);
    }
    return result;
  }
}
