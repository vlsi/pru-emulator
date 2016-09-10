package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Instruction;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.MemoryTransferInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class CycleWaitBlockTest {

  @DataProvider()
  public Iterator<Object[]> data() {
    return IntStream.range(20, 100)
        .mapToObj(value -> value)
        .flatMap(i -> Stream.of(new Object[][]{
            {i, 0},
            {i, 1},
            {i, 2},
            {i, 3},
            {i, 4},
            {i, 5},
            {i, 70},
        }).filter(x -> (Integer) x[0] > (Integer) x[1] + 20)).iterator();
  }

  @Test(dataProvider = "data")
  public void testCycleLength(int cycleLength, int bodyLength) {
    final int tolerance = 1;

    CodeEmitter ce = new CodeEmitter();
    Label codeStart = new Label("codeStart");
    generateCycle(ce, cycleLength, codeEmitter -> {
      ce.visitLabel(codeStart);
      for (int i = 0; i < bodyLength; i++) {
        ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
            new Register(1, RegisterField.dw), new Register(1, RegisterField.dw), 0));
      }
    });

    Assert.assertTrue(codeStart.getOffset() > 0, "codeStart label should be initialized");

    List<Instruction> instructions = ce.visitEnd();

    Pru pru = new Pru();
    pru.setInstructions(instructions);

    int lastCycleStart = 0;
    int startOffset = codeStart.getOffset();
    for (int i = 0; i < cycleLength * 1000; i++) {
      pru.tick();
      if (lastCycleStart == 0 && i > cycleLength * 2 + 5) {
        Assert.fail(
            "At least one cycle expected, total ticks " + i + ", pru is stuck: " + pru.printState());
      }
      if (pru.getPc() != startOffset) {
        continue;
      }
      if (lastCycleStart != 0) {
        Assert.assertEquals(i - lastCycleStart, cycleLength,
            "Cycle length should be " + cycleLength + ", bodyLength = " + bodyLength);
      } else {
        Assert.assertTrue(Math.abs(i - cycleLength) <= tolerance,
            "The first cycle should be in ±" + tolerance + " ticks within the set cycleLength." +
                " Actual cycle length is " + i + ", expected: " + (cycleLength - tolerance) + ".." + (cycleLength + tolerance));
      }
      lastCycleStart = i;
    }
  }

  /**
   * The following code was generated out of
   * <pre>{@code FUNCTION_BLOCK WAIT_TICK
   *   variables:
   *   input pruCycleLength : WORD;
   *   cyclesLeft : WORD;
   *   currentCycles : WORD;
   *   controlRegisterAddress : DWORD;
   *
   * body:
   *   (*0x00007000..0x00007FFF -- PRU0 Control Registers, 0xC -- cycle count register*)
   *   controlRegisterAddress := 16#700C;
   *   ASM
   *     LBBO currentCycles, controlRegisterAddress, 0, 2 ;Load cycle count, 1+wdcnt*1==2 cycles
   *   END_ASM
   *   currentCycles := currentCycles + 8;
   *   IF pruCycleLength > currentCycles THEN
   *     cyclesLeft:=cyclesLeftXOR0;
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
   *     SBBO cyclesLeft, controlRegisterAddress, 0, 2; Load cycle count, 1+wdcnt*1==2 cycles
   *   END_ASM
   * }</pre>
   */
  private void generateCycle(CodeEmitter ce, int cycleLength, Consumer<CodeEmitter> body) {
    Label startWhileBody0 = new Label("startWhileBody0");
    Label if3 = new Label("if3");
    Label endIf4 = new Label("endIf4");
    Label startWhileBody5 = new Label("startWhileBody5");
    Label endWhile6 = new Label("endWhile6");
    Label endIf2 = new Label("endIf2");
    Label endWhile1 = new Label("endWhile1");
    ce.visitLabel(startWhileBody0);
    // Call WAIT_TICK
    ce.visitInstruction(new LdiInstruction(new Register(1, RegisterField.w0), (short) cycleLength));
    // 0x00007000..0x00007FFF -- PRU0 Control Registers, 0xC -- cycle count register
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.dw), (short) 28684));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(1, RegisterField.w2)).setAddress(new Register(2, RegisterField.dw)).setOffset(
        0).setLength(2).encode());
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(1, RegisterField.w2), new Register(1, RegisterField.w2), 8));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if3,
        new Register(1, RegisterField.w0), new Register(1, RegisterField.w2)));
    ce.visitInstruction(new LdiInstruction(new Register(3, RegisterField.w0), (short) 0));

    ce.visitInstruction(new QuickBranchInstruction(endIf2));
    ce.visitLabel(if3);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(3, RegisterField.w0), new Register(1, RegisterField.w0),
        new Register(1, RegisterField.w2)));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(3, RegisterField.w0), new Register(3, RegisterField.w0), 0));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.BC, endIf4,
        new Register(3, RegisterField.w0), 0));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(3, RegisterField.w0), new Register(3, RegisterField.w0), 1));
    ce.visitLabel(endIf4);

    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endWhile6,
        new Register(3, RegisterField.w0), 0));
    ce.visitLabel(startWhileBody5);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(3, RegisterField.w0), new Register(3, RegisterField.w0), 2));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, startWhileBody5,
            new Register(3, RegisterField.w0), 0));
    ce.visitLabel(endWhile6);


    ce.visitLabel(endIf2);

    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(3, RegisterField.w0)).setAddress(new Register(2, RegisterField.dw)).setOffset(
        0).setLength(2).encode());
    //
    // End WAIT_TICK

    // Build PRU loop body
    body.accept(ce);

    ce.visitInstruction(new QuickBranchInstruction(startWhileBody0));
    ce.visitLabel(endWhile1);
  }
}
