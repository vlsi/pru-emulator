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
    CodeEmitter ce = new CodeEmitter();
    Label codeStart = new Label("codeStart");
    generateCycle(ce, cycleLength - 2, codeEmitter -> {
      ce.visitLabel(codeStart);
      for (int i = 0; i < bodyLength; i++) {
        ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
            new Register(1, RegisterField.dw), new Register(1, RegisterField.dw), 0));
      }
    });

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
        int tolerance = 1;
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
   *   cyclesLeft := pruCycleLength - currentCycles;
   *   IF cyclesLeft.0 THEN
   *     cyclesLeft := cyclesLeft XOR 1;
   *   END_IF;
   *   REPEAT
   *     cyclesLeft := cyclesLeft - 2;
   *   UNTIL cyclesLeft > 0
   *   END_REPEAT;
   *   ASM
   *     SBBO cyclesLeft, controlRegisterAddress, 0, 2; Load cycle count, 1+wdcnt*1==2 cycles
   *   END_ASM
   * }</pre>
   */
  private void generateCycle(CodeEmitter ce, int cycleLength, Consumer<CodeEmitter> body) {
    Label startWhile0 = new Label("startWhile0");
    Label endIf2 = new Label("endIf2");
    Label startRepeat3 = new Label("startRepeat3");
    Label endWhile1 = new Label("endWhile1");
    ce.visitLabel(startWhile0);
    // Call WAIT_TICK
    ce.visitInstruction(new LdiInstruction(new Register(1, RegisterField.w0), (short) cycleLength));
    // 0x00007000..0x00007FFF -- PRU0 Control Registers, 0xC -- cycle count register
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.dw), (short) 28684));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(1, RegisterField.w2)).setAddress(new Register(2, RegisterField.dw)).setOffset(
        0).setLength(2).encode());
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(1, RegisterField.w0), new Register(1, RegisterField.w0),
        new Register(1, RegisterField.w2)));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.BC, endIf2,
        new Register(1, RegisterField.w0), 0));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(1, RegisterField.w0), new Register(1, RegisterField.w0), 1));
    ce.visitLabel(endIf2);

    ce.visitLabel(startRepeat3);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(1, RegisterField.w0), new Register(1, RegisterField.w0), 2));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, startRepeat3,
            new Register(1, RegisterField.w0), 0));

    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(1, RegisterField.w0)).setAddress(new Register(2, RegisterField.dw)).setOffset(
        0).setLength(2).encode());
    //
    // End WAIT_TICK

    // Build PRU loop body
    body.accept(ce);

    ce.visitInstruction(new QuickBranchInstruction(startWhile0));
    ce.visitLabel(endWhile1);
  }
}
