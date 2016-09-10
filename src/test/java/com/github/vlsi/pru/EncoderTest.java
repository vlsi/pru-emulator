package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.MemoryTransferInstruction;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.Test;

public class EncoderTest {
  @Test
  public void lbbo() {
    MemoryTransferInstruction ins = new MemoryTransferInstruction(
        MemoryTransferInstruction.Operation.LOAD,
        new Register(8, RegisterField.b3))
        .setAddress(CommonRegisters.R1)
        .setAddress(CommonRegisters.R1)
        .setLength((byte) 42)
        .setOffset(new Register(6, RegisterField.b2))
        .encode();
    String actual = ins.toString();
    Assert.assertEquals(actual, "LBBO R8.b3, R1.b0, R6.b2, 42");
    Assert.assertEquals(actual, new MemoryTransferInstruction(ins.code).toString());
  }
}
