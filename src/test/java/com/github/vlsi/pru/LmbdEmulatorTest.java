package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.LeftMostBitDetectInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LmbdEmulatorTest {
  private final Pru cpu = new Pru();

  @DataProvider
  public Object[][] data() {
    return new Object[][]{
        {RegisterField.b0, 0, 1, 32},
        {RegisterField.b0, 1, 1, 0},
        {RegisterField.b0, 2, 1, 1},
        {RegisterField.b0, 3, 1, 1},
        {RegisterField.b0, 4, 1, 2},
        {RegisterField.b0, 5, 1, 2},
        {RegisterField.b0, 7, 1, 2},
        {RegisterField.b0, 8, 1, 3},
        {RegisterField.b0, 255, 1, 7},

        {RegisterField.b0, 128, 0, 6},
        {RegisterField.b0, 128+64, 0, 5},
        {RegisterField.b0, 128+64+32, 0, 4},
        {RegisterField.b0, 4, 0, 7},
        {RegisterField.b0, 5, 0, 7},
        {RegisterField.b0, 7, 0, 7},
        {RegisterField.b0, 8, 0, 7},
        {RegisterField.b0, 255, 0, 32},
    };
  }

  @Test(dataProvider = "data")
  public void run(RegisterField field, int value, int bit, int expected) {
    Register resultRegister = new Register(1, RegisterField.b0);
    Register srcRegister = new Register(0, field);

    cpu.setReg(srcRegister, value);
    cpu.setReg(resultRegister, 255);
    cpu.setInstructions(
        new LeftMostBitDetectInstruction(resultRegister, srcRegister, (byte) bit)
    );
    cpu.setPc(0);
    cpu.tick();
    Assert.assertEquals(cpu.getReg(resultRegister), expected);
  }
}
