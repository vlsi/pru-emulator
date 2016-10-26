package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.annotations.Test;
import st61131.pru.PRU_STEPPER_ACCEL_CALC_CodeGenerator;

public class AccelCalcTest {

  @Test
  public void init() {
    PRU_STEPPER_ACCEL_CALC_CodeGenerator g = new PRU_STEPPER_ACCEL_CALC_CodeGenerator();
    CodeEmitter ce = new CodeEmitter();
    g.appendCode(ce);

    Pru cpu = new Pru();
    cpu.setCode(ce.visitEnd());

    int delay = 1015232;
    int accel_count = 0;
    cpu.setReg(new Register(2, RegisterField.dw), 1015232); // delay
    cpu.setReg(new Register(1, RegisterField.dw), 0); // rest
    cpu.setReg(new Register(3, RegisterField.dw), 1); // accel_count
//    cpu.setReg(new Register(13, RegisterField.dw), 20000); // min_delay
    for (int i = 0; i < 10; i++) {
      cpu.setPc(0);
      accel_count++;
      cpu.setReg(new Register(2, RegisterField.dw), delay); // delay
      cpu.setReg(new Register(3, RegisterField.dw), accel_count); // accel_count
      int time = cpu.runTillHalt(10000);
      delay = cpu.getReg(new Register(3, RegisterField.dw));// next_step_delay
      int rest = cpu.getReg(new Register(1, RegisterField.dw));
      System.out.println(time + "," + delay + "," + rest);
    }
  }
}
