package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.Decoder;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;

public class CpuEmulatorTest {
  @Test
  public void sub() {
    Pru cpu = new Pru();
    ByteBuffer bb = ByteBuffer.allocate(8);
    ArithmeticInstruction subW0W0_1 = new ArithmeticInstruction(
        ArithmeticInstruction.Operation.SUB,
        1, RegisterField.w0, 1, RegisterField.w0, (byte) 1);
    bb.putInt(subW0W0_1.code);
    bb.putInt(subW0W0_1.code);
    bb.flip();
    cpu.setInstructions(new Decoder().decode(bb));

    cpu.setReg(1, RegisterField.w0, 42);
    cpu.tick();

    Assert.assertEquals(cpu.getReg(1), 41, "42 - 1");

    cpu.tick();
    Assert.assertEquals(cpu.getReg(1), 40, "42 - 1 - 1");
  }

  @Test
  public void ldi() {
    Pru cpu = new Pru();
    ByteBuffer bb = ByteBuffer.allocate(4);
    int value = 12345;
    bb.putInt(new LdiInstruction(1, RegisterField.w1, (short) value).code);
    bb.flip();
    cpu.setInstructions(new Decoder().decode(bb));

    cpu.tick();

    Assert.assertEquals(cpu.getReg(1, RegisterField.w1), value);
  }

  @Test
  public void addByteWithCarry() {
    Pru cpu = new Pru();
    ByteBuffer bb = ByteBuffer.allocate(4 * 4);

    // b0 = 189
    bb.putInt(
        new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
            1, RegisterField.b0, 1, RegisterField.b0, (byte) 189).code);
    // b1 = 190
    bb.putInt(
        new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
            1, RegisterField.b1, 1, RegisterField.b1, (byte) 190).code);
    // b2 = b1 + b0 -> should carry
    bb.putInt(
        new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
            1, RegisterField.b2, 1, RegisterField.b1, 1, RegisterField.b0).code);
    // b3 = b3 + 0 + carry
    bb.putInt(
        new ArithmeticInstruction(ArithmeticInstruction.Operation.ADC,
            1, RegisterField.b3, 1, RegisterField.b3, (byte) 0).code);
    bb.flip();
    cpu.setInstructions(new Decoder().decode(bb));

    for (int i = 0; i < 4; i++) {
      cpu.tick();
    }

    Assert.assertEquals(cpu.getReg(1, RegisterField.w2), 189 + 190, "189 + 190");
  }
}
