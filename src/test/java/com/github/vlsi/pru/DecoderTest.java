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
    assertDecode(new int[]{0x00e1e1e1, 0x05018181},
        "ArithmeticInstruction{operation=ADD, dstRegister=R1, srcRegister=R1, op2=R1, op2IsRegister=true}\n" +
            "ArithmeticInstruction{operation=SUB, dstRegister=R1.w0, srcRegister=R1.w0, op2=1, op2IsRegister=false}");
  }

  private void assertDecode(int[] code, String expected) {
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
    assertDecode(new int[]{0x1700e1e1},
        "ArithmeticInstruction{operation=NOT, dstRegister=R1, srcRegister=R1, op2=0, op2IsRegister=false}");
  }

}
