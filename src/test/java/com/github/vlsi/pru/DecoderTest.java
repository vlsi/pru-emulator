package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.Decoder;
import com.github.vlsi.pru.plc110.Instruction;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

public class DecoderTest {
  @Test
  public void subR1w0R1w01() {
    Instruction ins = Instruction.of(0x05018181);
    Assert.assertEquals(ins.getClass(), ArithmeticInstruction.class,
        "0x05018181 : SUB R1.w0, R1.w0, 1");
  }

  @Test
  public void decodeAddSub() {
    assertDecode("ADD R1, R1, R1\n" +
            "SUB R1.w0, R1.w0, 1",
        0x00e1e1e1, 0x05018181);
  }

  private void assertDecode(String expected, int... code) {
    int capacity = code.length * 4;
    ByteBuffer bb = ByteBuffer.allocate(capacity);
    bb.asIntBuffer().put(code);
    bb.limit(capacity);
    List<Instruction> ins = new Decoder().decode(bb);
    String actual = ins.stream().map(Object::toString).collect(Collectors.joining("\n"));
    Assert.assertEquals(actual,
        expected);
  }

  @Test
  public void decodeNot() {
    assertDecode("NOT R1, R1, 0",
        0x1700e1e1);
  }

  @Test
  public void decodeQBGT() {
    assertDecode("QBGT offset=3, R1, 0",
        0x6100e103);
  }

  @Test
  public void decodeQBA2() {
    assertDecode("QBA offset=2, R0.b0, 0",
        0x79000002);
  }

  @Test
  public void decodeLsl() {
    assertDecode("LSL R1, R1, 5",
        0x0905e1e1);
  }

  @Test
  public void decodeLmbd() {
    assertDecode("LMBD R1, R1, 1",
        0x2701e1e1);
  }

  @Test
  public void decodeLbbo() {
    assertDecode("LBBO R0.b2, R0.b0, 12, 1",
        0xf10c0040);
  }

  @Test
  public void decodeLbbo2() {
    assertDecode("LBBO R1.b2, R0.b0, 12, 2",
        0xf10c00c1);
  }

  @Test
  public void decodeSbbo() {
    assertDecode("SBBO R0.b2, R0.b0, 12, 1",
        0xe10c0040);
  }

  @Test
  public void decodeSbbo2() {
    assertDecode("SBBO R1.b2, R0.b0, 12, 2",
        0xe10c00c1);
  }

  @Test
  public void decodeLbco() {
    assertDecode("LBCO R0.b0, C3, 0, 1",
        0x91000300);
  }

  @Test
  public void decodeLbco2() {
    assertDecode("LBCO R0.b0, C3, 112, 4",
        0x91702380);
  }

  @Test
  public void decodeSbco() {
    assertDecode("SBCO R0.b0, C3, 116, 4",
        0x81742380);
  }

  @Test
  public void decodeSbco2() {
    assertDecode("SBCO R0.b0, C3, 0, 1",
        0x81000300);
  }
}
