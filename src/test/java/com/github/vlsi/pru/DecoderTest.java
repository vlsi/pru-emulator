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

}
