package com.github.vlsi.pru;

import static com.github.vlsi.pru.CommonRegisters.R1_b0;
import static com.github.vlsi.pru.CommonRegisters.R1_b1;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.stream.IntStream;

public class GtByteEmulatorTest {
  private final Pru cpu = new Pru();

  @BeforeClass
  public void setup() {
    CodeEmitter ce = new CodeEmitter();
    Label gt = new Label("test");
    ce.visitInstruction(
        new QuickBranchInstruction(
            QuickBranchInstruction.Operation.GT,
            gt, R1_b0,
            (byte) 42
        ));
    ce.visitInstruction(
        new ArithmeticInstruction(
            ArithmeticInstruction.Operation.ADD,
            R1_b1,
            R1_b1,
            (byte) 1));
    Label end = new Label("end");
    ce.visitInstruction(new QuickBranchInstruction(end));
    ce.visitLabel(gt);
    ce.visitInstruction(
        new ArithmeticInstruction(
            ArithmeticInstruction.Operation.ADD,
            R1_b1,
            R1_b1,
            (byte) 2));
    ce.visitLabel(end);
    cpu.setInstructions(ce.visitEnd().getInstructions());
  }

  private void run() {
    int i;
    for (i = 0; i < 10 && cpu.getPc() < 4; i++) {
      cpu.tick();
    }
    if (i == 10) {
      Assert.fail("Unable to execute instructions in " + i + " ticks, Cpu state: "
          + cpu.printState());
    }
  }

  @DataProvider
  public Iterator<Object[]> bytes() {
    return IntStream.range(Byte.MIN_VALUE, Byte.MAX_VALUE + 1)
        .mapToObj(i -> new Object[]{i})
        .iterator();
  }

  @Test(dataProvider = "bytes")
  public void compareByte(int val) {
    cpu.setPc(0);
    cpu.setReg(R1_b1, 0);
    cpu.setReg(R1_b0, val);
    run();
    Assert.assertEquals(cpu.getReg(R1_b1),
        42 > (val & 0xff) ? 2 : 1, "val: " + Integer.toString(val));
  }
}
