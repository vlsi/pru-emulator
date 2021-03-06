package st61131.pru;

/*Generated by MPS */

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Format2Instruction;
import com.github.vlsi.pru.plc110.JumpInstruction;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;

import java.util.function.Consumer;

public class PRU_CUTTER_CodeGenerator implements Consumer<CodeEmitter> {
  public void setEnable(Pru cpu, int value) {
    cpu.setReg(new Register(3, RegisterField.b0), value);
  }

  public void setRunLength(Pru cpu, int value) {
    cpu.setReg(new Register(4, RegisterField.dw), value);
  }

  public void setCntr(Pru cpu, int value) {
    cpu.setReg(new Register(3, RegisterField.w1), value);
  }

  public int getState(Pru cpu) {
    return cpu.getReg(new Register(2, RegisterField.b3));
  }

  public int getOffset(Pru cpu) {
    return cpu.getReg(new Register(1, RegisterField.dw));
  }

  public int getOut(Pru cpu) {
    return cpu.getReg(new Register(2, RegisterField.b2));
  }

  @Override
  public void accept(CodeEmitter ce) {
    Label if1 = new Label("if1");
    Label endIf2 = new Label("endIf2");
    Label elsIf6 = new Label("elsIf6");
    Label orMatch5 = new Label("orMatch5");
    Label if4 = new Label("if4");
    Label endIf3 = new Label("endIf3");
    Label elsIf8 = new Label("elsIf8");
    Label endIf7 = new Label("endIf7");
    Label endIf0 = new Label("endIf0");
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, if1,
        new Register(2, RegisterField.b3), 2).setComment("state => R2.b3"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, elsIf6,
        new Register(2, RegisterField.b3), 0).setComment("state => R2.b3"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, elsIf8,
        new Register(2, RegisterField.b3), 1).setComment("state => R2.b3"));
    ce.visitInstruction(new JumpInstruction(Format2Instruction.Operation.JMP, endIf0,
        new Register(1, RegisterField.dw)).setComment(""));
    ce.visitLabel(if1);
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, endIf2,
        new Register(3, RegisterField.b0), 0).setComment("enable => R3.b0"));
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b3), (short) 0).setComment(
        "state => R2.b3"));
    ce.visitInstruction(new LdiInstruction(new Register(1, RegisterField.dw), (short) 0).setComment(
        "offset => R1"));

    ce.visitLabel(endIf2);

    //

    ce.visitInstruction(new JumpInstruction(Format2Instruction.Operation.JMP, endIf0,
        new Register(1, RegisterField.dw)).setComment(""));
    ce.visitLabel(elsIf6);
    ce.visitInstruction(new LdiInstruction(new Register(1, RegisterField.dw), (short) 0).setComment(
        "offset => R1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(2, RegisterField.w0), new Register(3, RegisterField.w1), 0).setComment(
        "prevCntr => R2.w0, cntr => R3.w1"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, orMatch5,
        new Register(3, RegisterField.b0), 0).setComment("enable => R3.b0"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if4,
        new Register(3, RegisterField.w1), 0).setComment("cntr => R3.w1"));
    ce.visitLabel(orMatch5);

    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b2), (short) 0).setComment(
        "out => R2.b2"));

    ce.visitInstruction(new JumpInstruction(Format2Instruction.Operation.JMP, endIf3,
        new Register(1, RegisterField.dw)).setComment(""));
    ce.visitLabel(if4);
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b3), (short) 1).setComment(
        "state => R2.b3"));
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b2), (short) 1).setComment(
        "out => R2.b2"));

    ce.visitLabel(endIf3);


    ce.visitInstruction(new JumpInstruction(Format2Instruction.Operation.JMP, endIf0,
        new Register(1, RegisterField.dw)).setComment(""));
    ce.visitLabel(elsIf8);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(5, RegisterField.w0), new Register(3, RegisterField.w1),
        new Register(2, RegisterField.w0)).setComment(
        "diff => R5.w0, cntr => R3.w1, prevCntr => R2.w0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(2, RegisterField.w0), new Register(3, RegisterField.w1), 0).setComment(
        "prevCntr => R2.w0, cntr => R3.w1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(1, RegisterField.dw), new Register(1, RegisterField.dw),
        new Register(5, RegisterField.w0)).setComment("offset => R1, diff => R5.w0"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.GT, endIf7,
        new Register(1, RegisterField.dw), new Register(4, RegisterField.dw)).setComment(
        "offset => R1, runLength => R4"));
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b2), (short) 0).setComment(
        "out => R2.b2"));
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b3), (short) 2).setComment(
        "state => R2.b3"));

    ce.visitLabel(endIf7);


    ce.visitLabel(endIf0);


  }
}
