package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CpuSimpleTest {
  @Test
  public void bytesVsWords() {
    Register reg = new Pru().getRegister(0);

    reg.setR8(0, (byte) 1);
    reg.setR8(1, (byte) 2);
    reg.setR8(2, (byte) 3);

    Assert.assertEquals("0x" + Integer.toHexString(reg.getR16(0)), "0x201", "(2<<8) | 1");
    Assert.assertEquals("0x" + Integer.toHexString(reg.getR16(1)), "0x302", "(3<<8) | 2");
    Assert.assertEquals("0x" + Integer.toHexString(reg.get()), "0x30201", "(3<<16) | (2<<8) | 1");
  }

  @Test
  public void bytesVsWordsCpu() {
    Pru cpu = new Pru();

    cpu.setReg(0, RegisterField.b0, (byte) 1);
    cpu.setReg(0, RegisterField.b1, (byte) 2);
    cpu.setReg(0, RegisterField.b2, (byte) 3);

    Assert.assertEquals("0x" + Integer.toHexString(cpu.getReg(0, RegisterField.w0)), "0x201",
        "(2<<8) | 1");
    Assert.assertEquals("0x" + Integer.toHexString(cpu.getReg(0, RegisterField.w1)), "0x302",
        "(3<<8) | 2");
    Assert.assertEquals("0x" + Integer.toHexString(cpu.getReg(0, RegisterField.dw)), "0x30201",
        "(3<<16) | (2<<8) | 1");
  }
}
