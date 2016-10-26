package st61131.pru;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Format2Instruction;
import com.github.vlsi.pru.plc110.JumpInstruction;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.MemoryTransferInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;

import java.util.function.Consumer;

public class WAIT_TICK_DW_CodeGenerator implements Consumer<CodeEmitter> {
  public void setPruCycleLength(Pru cpu, int value) {
    cpu.setReg(new Register(1, RegisterField.dw), value);
  }


  /**
   * The following code was generated out of
   * <pre>{@code FUNCTION_BLOCK WAIT_TICK_DW
   * variables:
   *   input pruCycleLength : DWORD;
   *   cyclesLeft : DWORD;
   *   currentCycles : DWORD;
   *   controlRegisterAddress : DWORD;
   *
   * body:
   *   (* 0x00007000..0x00007FFF -- PRU0 Control Registers, 0xC -- cycle count register *)
   *   controlRegisterAddress := 16#700C;
   *   ASM
   *     LBBO currentCycles, controlRegisterAddress, 0, 4 ; Load cycle count, 1+wdcnt*2==3 cycles
   *   END_ASM
   *   currentCycles := currentCycles + 10;
   *   IF pruCycleLength > currentCycles THEN
   *     cyclesLeft := pruCycleLength - currentCycles;
   *     (*cyclesLeft := cyclesLeft XOR 0;*)
   *     IF cyclesLeft.0 THEN
   *       cyclesLeft := cyclesLeft XOR 1;
   *     END_IF;
   *     WHILE cyclesLeft <> 0 DO
   *       cyclesLeft := cyclesLeft - 2;
   *     END_WHILE;
   *   ELSE
   *     cyclesLeft := 0;
   *   END_IF;
   *   ASM
   *     SBBO cyclesLeft, controlRegisterAddress, 0, 4 ; Load cycle count, 1+wdcnt*2==3 cycles
   *   END_ASM
   *   }</pre>
   */
  @Override
  public void accept(CodeEmitter ce) {
    Label if1 = new Label("if1");
    Label endIf2 = new Label("endIf2");
    Label startWhile3 = new Label("startWhile3");
    Label startWhileBody4 = new Label("startWhileBody4");
    Label endWhile5 = new Label("endWhile5");
    Label endIf0 = new Label("endIf0");
    // 0x00007000..0x00007FFF -- PRU0 Control Registers, 0xC -- cycle count register
    ce.visitInstruction(
        new LdiInstruction(new Register(2, RegisterField.dw), (short) 28684).setComment(
            "controlRegisterAddress => R2"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(3, RegisterField.dw)).setAddress(new Register(2, RegisterField.dw)).setOffset(
        0).setLength(4).encode().setComment(
        "Load cycle count, 1+wdcnt*2==3 cycles, currentCycles => R3, controlRegisterAddress => R2"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(3, RegisterField.dw), new Register(3, RegisterField.dw), 10).setComment(
        "currentCycles => R3"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if1,
        new Register(1, RegisterField.dw), new Register(3, RegisterField.dw)).setComment(
        "pruCycleLength => R1, currentCycles => R3"));
    ce.visitInstruction(new LdiInstruction(new Register(4, RegisterField.dw), (short) 0).setComment(
        "cyclesLeft => R4"));

    ce.visitInstruction(new JumpInstruction(Format2Instruction.Operation.JMP, endIf0,
        new Register(1, RegisterField.dw)).setComment(""));
    ce.visitLabel(if1);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(4, RegisterField.dw), new Register(1, RegisterField.dw),
        new Register(3, RegisterField.dw)).setComment(
        "cyclesLeft => R4, pruCycleLength => R1, currentCycles => R3"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.BC, endIf2,
        new Register(4, RegisterField.dw), 0).setComment("cyclesLeft => R4"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(4, RegisterField.dw), new Register(4, RegisterField.dw), 1).setComment(
        "cyclesLeft => R4"));
    ce.visitLabel(endIf2);

    ce.visitLabel(startWhile3);
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endWhile5,
        new Register(4, RegisterField.dw), 0).setComment("cyclesLeft => R4"));
    ce.visitLabel(startWhileBody4);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(4, RegisterField.dw), new Register(4, RegisterField.dw), 2).setComment(
        "cyclesLeft => R4"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, startWhileBody4,
            new Register(4, RegisterField.dw), 0).setComment("cyclesLeft => R4"));
    ce.visitLabel(endWhile5);


    ce.visitLabel(endIf0);

    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(4, RegisterField.dw)).setAddress(new Register(2, RegisterField.dw)).setOffset(
        0).setLength(4).encode().setComment(
        "Load cycle count, 1+wdcnt*2==3 cycles, cyclesLeft => R4, controlRegisterAddress => R2"));
    //

  }
}