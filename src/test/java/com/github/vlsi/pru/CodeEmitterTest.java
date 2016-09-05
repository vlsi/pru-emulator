package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Format2Instruction;
import com.github.vlsi.pru.plc110.JumpInstruction;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CodeEmitterTest {
  @Test
  public void qbBackward() {
    CodeEmitter ce = new CodeEmitter();
    Label start = new Label("start");
    ce.visitLabel(start);
    ce.visitInstruction(new LdiInstruction(CommonRegisters.R1_b0, (short) 42));
    QuickBranchInstruction qb = new QuickBranchInstruction(start);
    ce.visitInstruction(qb);
    ce.visitEnd();
    Assert.assertEquals(qb.offset, -1);
  }

  @Test
  public void qbForward() {
    CodeEmitter ce = new CodeEmitter();
    Label end = new Label("end");
    QuickBranchInstruction qb = new QuickBranchInstruction(end);
    ce.visitInstruction(qb);
    ce.visitInstruction(new LdiInstruction(CommonRegisters.R1_b0, (short) 42));
    ce.visitLabel(end);
    ce.visitEnd();
    Assert.assertEquals(qb.offset, 2);
  }

  @Test
  public void jmpBackward() {
    CodeEmitter ce = new CodeEmitter();
    Label start = new Label("start");
    ce.visitLabel(start);
    ce.visitInstruction(new LdiInstruction(CommonRegisters.R1_b0, (short) 42));
    JumpInstruction jmp = new JumpInstruction(Format2Instruction.Operation.JMP, start, CommonRegisters.R1);
    ce.visitInstruction(jmp);
    ce.visitEnd();
    Assert.assertEquals(jmp.op2, 0);
  }

  @Test
  public void jmpForward() {
    CodeEmitter ce = new CodeEmitter();
    Label end = new Label("end");
    JumpInstruction jmp = new JumpInstruction(Format2Instruction.Operation.JMP, end, CommonRegisters.R1);
    ce.visitInstruction(jmp);
    ce.visitInstruction(new LdiInstruction(CommonRegisters.R1_b0, (short) 42));
    ce.visitLabel(end);
    ce.visitEnd();
    Assert.assertEquals(jmp.op2, 2);
  }
}
