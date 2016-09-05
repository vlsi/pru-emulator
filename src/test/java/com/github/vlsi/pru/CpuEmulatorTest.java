package com.github.vlsi.pru;

import static com.github.vlsi.pru.CommonRegisters.R1;
import static com.github.vlsi.pru.CommonRegisters.R1_b0;
import static com.github.vlsi.pru.CommonRegisters.R1_b1;
import static com.github.vlsi.pru.CommonRegisters.R1_b2;
import static com.github.vlsi.pru.CommonRegisters.R1_b3;
import static com.github.vlsi.pru.CommonRegisters.R1_w0;
import static com.github.vlsi.pru.CommonRegisters.R1_w1;
import static com.github.vlsi.pru.CommonRegisters.R1_w2;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.Decoder;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
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
        R1_w0, R1_w0, (byte) 1);
    bb.putInt(subW0W0_1.code);
    bb.putInt(subW0W0_1.code);
    bb.flip();
    cpu.setInstructions(new Decoder().decode(bb));

    cpu.setReg(R1_w0, 42);
    cpu.tick();

    Assert.assertEquals(cpu.getReg(R1), 41, "42 - 1");

    cpu.tick();
    Assert.assertEquals(cpu.getReg(R1), 40, "42 - 1 - 1");
  }

  @Test
  public void ldi() {
    Pru cpu = new Pru();
    ByteBuffer bb = ByteBuffer.allocate(4);
    int value = 12345;
    bb.putInt(new LdiInstruction(R1_w1, (short) value).code);
    bb.flip();
    cpu.setInstructions(new Decoder().decode(bb));

    cpu.tick();

    Assert.assertEquals(cpu.getReg(R1_w1), value);
  }

  @Test
  public void qbgtNegativeOffset() {
    Pru cpu = new Pru();
    ByteBuffer bb = ByteBuffer.allocate(4);
    int offset = -43;
    bb.putInt(new QuickBranchInstruction(
        QuickBranchInstruction.Operation.GT, new Label("test", offset),
        R1_w1,
        (byte) 42).code);
    bb.flip();
    cpu.setInstructions(new Decoder().decode(bb));

    cpu.tick();

    Assert.assertEquals(cpu.getPc(), offset);
  }

  @Test
  public void addByteWithCarry() {
    Pru cpu = new Pru();
    ByteBuffer bb = ByteBuffer.allocate(4 * 4);

    // b0 = 189
    bb.putInt(
        new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
            R1_b0, R1_b0, (byte) 189).code);
    // b1 = 190
    bb.putInt(
        new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
            R1_b1, R1_b1, (byte) 190).code);
    // b2 = b1 + b0 -> should carry
    bb.putInt(
        new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
            R1_b2, R1_b1, R1_b0).code);
    // b3 = b3 + 0 + carry
    bb.putInt(
        new ArithmeticInstruction(ArithmeticInstruction.Operation.ADC,
            R1_b3, R1_b3, (byte) 0).code);
    bb.flip();
    cpu.setInstructions(new Decoder().decode(bb));

    for (int i = 0; i < 4; i++) {
      cpu.tick();
    }

    Assert.assertEquals(cpu.getReg(R1_w2), 189 + 190, "189 + 190");
  }
}
