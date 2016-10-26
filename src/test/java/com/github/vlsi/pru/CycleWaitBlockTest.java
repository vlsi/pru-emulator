package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.BinaryCode;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import st61131.pru.WAIT_TICK_DW_CodeGenerator;

import java.util.Iterator;
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

    Assert.assertTrue(codeStart.isInitialized(), "codeStart label should be initialized");

    BinaryCode code = ce.visitEnd();

    Pru pru = new Pru();
    pru.setCode(code);
    new WAIT_TICK_DW_CodeGenerator().setPruCycleLength(pru, cycleLength);

    int lastCycleStart = 0;
    int startOffset = codeStart.getAbsoluteOffset();
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

  private void generateCycle(CodeEmitter ce, int cycleLength, Consumer<CodeEmitter> body) {
    WAIT_TICK_DW_CodeGenerator waitTick = new WAIT_TICK_DW_CodeGenerator();

    Label startWhileBody0 = new Label("startWhileBody0");
    ce.visitLabel(startWhileBody0);
    waitTick.accept(ce);

    // Build PRU loop body
    body.accept(ce);

    ce.visitInstruction(new QuickBranchInstruction(startWhileBody0));
  }
}
