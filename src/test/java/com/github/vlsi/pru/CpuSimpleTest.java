package com.github.vlsi.pru;

import static com.github.vlsi.pru.CommonRegisters.R1;
import static com.github.vlsi.pru.CommonRegisters.R1_b0;
import static com.github.vlsi.pru.CommonRegisters.R1_b1;
import static com.github.vlsi.pru.CommonRegisters.R1_b2;
import static com.github.vlsi.pru.CommonRegisters.R1_w0;
import static com.github.vlsi.pru.CommonRegisters.R1_w1;

import com.github.vlsi.pru.plc110.Pru;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CpuSimpleTest {
  @Test
  public void bytesVsWords() {
    Pru pru = new Pru();

    pru.setReg(R1_b0, 1);
    pru.setReg(R1_b1, 2);
    pru.setReg(R1_b2, 3);

    Assert.assertEquals("0x" + Integer.toHexString(pru.getReg(R1_w0)), "0x201", "(2<<8) | 1");
    Assert.assertEquals("0x" + Integer.toHexString(pru.getReg(R1_w1)), "0x302", "(3<<8) | 2");
    Assert.assertEquals("0x" + Integer.toHexString(pru.getReg(R1)), "0x30201", "(3<<16) | (2<<8) | 1");
  }
}
