package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.stream.IntStream;

public class GtByteEmulatorTest {
  private final Pru cpu = new Pru();
  private final int outReg = 1;

  @BeforeClass
  public void setup() {
    cpu.setInstructions(
        new QuickBranchInstruction(
            QuickBranchInstruction.Operation.GT,
            (short) 3, 1, RegisterField.b0,
            (byte) 42
        ),
        new ArithmeticInstruction(
            ArithmeticInstruction.Operation.ADD,
            outReg, RegisterField.b1,
            outReg, RegisterField.b1,
            (byte) 1),
        new QuickBranchInstruction((short) 2),
        new ArithmeticInstruction(
            ArithmeticInstruction.Operation.ADD,
            outReg, RegisterField.b1,
            outReg, RegisterField.b1,
            (byte) 2)
    );
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
    cpu.setReg(1, RegisterField.b1, 0);
    cpu.setReg(1, RegisterField.b0, val);
    run();
    Assert.assertEquals(cpu.getReg(1, RegisterField.b1),
        42 > (val & 0xff) ? 2 : 1, "val: " + Integer.toString(val));
  }
}
