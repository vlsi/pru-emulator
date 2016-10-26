package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.annotations.Test;
import st61131.pru.PRU_DIV_CodeGenerator;
import st61131.pru.PRU_MULDIV_CodeGenerator;

public class MulDivTest {

  @Test
  public void init() {
    PRU_MULDIV_CodeGenerator g = new PRU_MULDIV_CodeGenerator();
    CodeEmitter ce = new CodeEmitter();
    g.accept(ce);

    Pru cpu = new Pru();
    cpu.setCode(ce.visitEnd());

    int delay = 1015232;
    int accel_count = 0;
//    int x = 1015232;
    int x = 1023456;
    cpu.setReg(new Register(1, RegisterField.dw), x); // x
    float f = 0.95f;
    int width = 256 * 256 * 256 * 16;
    int y = (int) (f * width);
    cpu.setReg(new Register(2, RegisterField.dw), y); // y
    int z = width;
    cpu.setReg(new Register(3, RegisterField.dw), z); // z
    int time = cpu.runTillHalt(10000);
    int q = cpu.getReg(new Register(4, RegisterField.dw));
    int r = cpu.getReg(new Register(5, RegisterField.dw));
    System.out.println("time = " + time + ", qn = " + q
        + ", rn = " + r + ", " + ((int) ((long) x * (long) y / (long) z)) + ", " + (x * (long) y % z));
//    cpu.setReg(new Register(13, RegisterField.dw), 20000); // min_delay
  }

  @Test
  public void div() {
    PRU_DIV_CodeGenerator g = new PRU_DIV_CodeGenerator();
    CodeEmitter ce = new CodeEmitter();
    g.accept(ce);

    Pru cpu = new Pru();
    cpu.setCode(ce.visitEnd());

//    int x = 1015232;
    int x = 256 * 128;
    cpu.setReg(new Register(1, RegisterField.dw), x); // x
    int y = 256 * 256;
    cpu.setReg(new Register(2, RegisterField.dw), y); // y
    int time = cpu.runTillHalt(10000);
    int q = cpu.getReg(new Register(6, RegisterField.dw));
    int r = cpu.getReg(new Register(3, RegisterField.dw));
    System.out.println("time = " + time + ", qn = " + q
        + ", rn = " + r + ", " + (x / y) + ", " + (x % y));
//    cpu.setReg(new Register(13, RegisterField.dw), 20000); // min_delay
  }
}
