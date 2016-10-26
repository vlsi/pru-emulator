package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.MemoryTransferInstruction;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
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
    Assert.assertEquals(new MemoryTransferInstruction(ins.code).toString(), actual);
  }

  @Test
  public void lbco1() {
    MemoryTransferInstruction ins =
        new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
            new Register(2, RegisterField.w0))
            .setAddress(3)
            .setOffset(0)
            .setLength(2)
            .encode();
    String actual = ins.toString();
    Assert.assertEquals(actual, "LBCO R2.b0, C3, 0, 2");
    Assert.assertEquals(new MemoryTransferInstruction(ins.code).toString(), "LBCO R2.b0, C3, 0, 2");
  }

  @Test
  public void qba10() {
    Label l = new Label("+10").setRelativeOffset(10);
    QuickBranchInstruction qba = new QuickBranchInstruction(l);
    qba.resolveTarget(0);
    Assert.assertEquals(Integer.toHexString(qba.code), "7900000a");
  }
}
