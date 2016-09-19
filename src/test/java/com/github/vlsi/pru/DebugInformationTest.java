package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.Pru;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DebugInformationTest {
  @Test
  public void singleVariable() {
    CodeEmitter ce = new CodeEmitter();
    Label start = new Label("start");
    Label mid = new Label("mid");
    Label end = new Label("end");
    ce.visitLabel(start);
    ce.visitInstruction(new LdiInstruction(CommonRegisters.R1_b3, (short) 42).setComment("var1 => R1.b3"));
    ce.visitLabel(mid);
    ce.visitInstruction(new LdiInstruction(CommonRegisters.R1_b2, (short) 43).setComment("var2 => R1.b2"));
    ce.visitLabel(end);
    ce.visitRegisterVariable("var1", "DWORD", start, mid, CommonRegisters.R1_b3);
    ce.visitRegisterVariable("var2", "DWORD", mid, end, CommonRegisters.R1_b2);
    ce.visitRegisterVariable("global", "DWORD", start, end, CommonRegisters.R1_b1);

    Pru cpu = new Pru();
    cpu.setCode(ce.visitEnd());

    Assert.assertEquals(cpu.printState(), "pc: 0\n" +
        "carry: 0\n" +
        "Instructions around pc\n" +
        "0: LDI R1.b3, 42 ; var1 => R1.b3 // <-- PC\n" +
        "1: LDI R1.b2, 43 ; var2 => R1.b2\n" +
        "\n" +
        "Variables\n" +
        "           Name |  Type |    Reg |    Decimal |        Hex\n" +
        "----------------+-------+--------+------------+------------\n" +
        "           var1 | DWORD |  R1.b3 |          0 |        0x0\n" +
        "         global | DWORD |  R1.b1 |          0 |        0x0\n");

    cpu.tick(); // executeLdi
    Assert.assertEquals(cpu.printState(), "pc: 1\n" +
        "carry: 0\n" +
        "Instructions around pc\n" +
        "0: LDI R1.b3, 42 ; var1 => R1.b3\n" +
        "1: LDI R1.b2, 43 ; var2 => R1.b2 // <-- PC\n" +
        "\n" +
        "Variables\n" +
        "           Name |  Type |    Reg |    Decimal |        Hex\n" +
        "----------------+-------+--------+------------+------------\n" +
        "           var2 | DWORD |  R1.b2 |          0 |        0x0\n" +
        "         global | DWORD |  R1.b1 |          0 |        0x0\n" +
        "\n" +
        "Registers\n" +
        "       Hex     |    Decimal ||    w2 |    w1 |    w0 ||  b3 |  b2 |  b1 |  b0\n" +
        "---------------+------------++-------+-------+-------++-----+-----+-----+-----\n" +
        "R1: 0x2a000000 |  704643072 || 10752 |     0 |     0 ||  42 |   0 |   0 |   0\n" +
        "...\n");

    cpu.tick(); // executeLdi
    Assert.assertEquals(cpu.printState(), "pc: 2\n" +
        "carry: 0\n" +
        "Instructions around pc\n" +
        "0: LDI R1.b3, 42 ; var1 => R1.b3\n" +
        "1: LDI R1.b2, 43 ; var2 => R1.b2\n" +
        "\n" +
        "Registers\n" +
        "       Hex     |    Decimal ||    w2 |    w1 |    w0 ||  b3 |  b2 |  b1 |  b0\n" +
        "---------------+------------++-------+-------+-------++-----+-----+-----+-----\n" +
        "R1: 0x2a2b0000 |  707461120 || 10795 | 11008 |     0 ||  42 |  43 |   0 |   0\n" +
        "...\n");
  }
}
