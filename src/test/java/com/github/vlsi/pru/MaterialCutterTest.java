package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.annotations.Test;
import st61131.pru.PRU_MATERIAL_CUTTER_Program_CodeGenerator;

import java.nio.ByteBuffer;

public class MaterialCutterTest {
  private final static Register inReg = new Register(31, RegisterField.dw);
  private final static Register outReg = new Register(30, RegisterField.dw);
  private final static int outIndex[] = new int[]{28, 29};
  private final static int inIndex[] = new int[]{21, 22, 2, 3};

  private boolean enable;
  private int runLength = 326;

  private int counter;
  private int position;
  private boolean zeroDetected;
  private int offset;
  private int state;
  private boolean in4;
  private boolean in4raw;

  private static boolean getOut(Pru cpu, int index) {
    return ((cpu.getReg(outReg) >> outIndex[index]) & 1) == 1;
  }

  private static void setIn(Pru cpu, int index, boolean value) {
    int reg = cpu.getReg(inReg);
    int mask = 1 << inIndex[index];
    reg &= ~mask;
    if (value) {
      reg |= mask;
    }
    cpu.setReg(inReg, reg);
  }


  private void exchange(Pru cpu, PRU_MATERIAL_CUTTER_Program_CodeGenerator mc) {
    ByteBuffer ram = cpu.ram();
    waitPruProcessing(cpu, ram);

    mc.ramSetPRU_MATERIAL_CUTTER_cutter_enable(cpu, enable ? 1 : 0);
    mc.ramSetPRU_MATERIAL_CUTTER_cutter_runLength(cpu, runLength);

    ram.putInt(0, 1);
    waitPruProcessing(cpu, cpu.ram());

    counter = mc.ramGetPRU_MATERIAL_CUTTER_abz_counter(cpu);
    position = mc.ramGetPRU_MATERIAL_CUTTER_abz_position(cpu);
    zeroDetected = mc.ramGetPRU_MATERIAL_CUTTER_abz_zeroDetected(cpu) == 1;
    offset = mc.ramGetPRU_MATERIAL_CUTTER_cutter_offset(cpu);
    state = mc.ramGetPRU_MATERIAL_CUTTER_cutter_state(cpu);
    in4 = mc.ramGetSys_inputs_in4(cpu) == 1;
  }

  private void waitPruProcessing(Pru cpu, ByteBuffer ram) {
    int i;
    for (i = 0; i < 10000 && ram.get(0) != 0; i++) {
      cpu.tick();
    }
    if (i == 10000) {
      throw new IllegalStateException(
          "PRU should clear 0 byte in reasonable time. State: " + cpu.printState());
    }
  }

  @Test
  public void run() {
    PRU_MATERIAL_CUTTER_Program_CodeGenerator mc = new PRU_MATERIAL_CUTTER_Program_CodeGenerator();

    CodeEmitter ce = new CodeEmitter();
    mc.accept(ce);

    int plcCycle = 200000000 / 1000;
    long prevCycle = 0;

    boolean rawIn4 = false;
    int prevt = 0;
    Register timeReg = new Register(12, RegisterField.dw);
    int j = 0;
    long st = 0, en = 0;

    VirtualAbEncoder encoder = new VirtualAbEncoder();

    Pru cpu = new Pru() {
      int prevEncoderStep;

      @Override
      public void tick() {
        if (getCycleCountNonReset() / 2000 != prevEncoderStep && enable) {
          prevEncoderStep = getCycleCountNonReset() / 2000;
          encoder.left();
        }
        super.tick();
      }
    };

    cpu.setCode(ce.visitEnd());
    for (long i = 0; i < 10000000; i++) {
      long cc = cpu.getCycleCountNonReset();
      int newT = cpu.getReg(timeReg);
      if (cpu.getPc() == 150) {
//        System.out.println(cc+","+newT);
        prevt = newT;
//        if (j++ > 3000) {
//          break;
//        }
      }
//      if (cpu.getPc() == 77) {
//        st = cc;
//      }
//      if (cpu.getPc() == 126) {
//        System.out.println("cc = " + (cc - st));
//      }

      if (cc / plcCycle != prevCycle) {
        prevCycle = cc / plcCycle;
        exchange(cpu, mc);
        double time = cpu.getReg(timeReg) / 200e3;
        double trueTime = cc / 200e3;
        System.out.println(((int) trueTime) + " ms, " + toString()
            + ", fastOut4=" + (getOut(cpu, 0) ? "1" : "0")
            + ", encoder=" + encoder.getPosition());
        if (i < 2000000 || !rawIn4) {
          rawIn4 ^= true;
        }
        if (trueTime > 20) {
          enable = true;
        } else if (trueTime > 17) {
          enable = false;
        } else if (trueTime > 4) {
          enable = true;
        }
      }
      in4raw = rawIn4;
      setIn(cpu, 3, rawIn4);
      setIn(cpu, 0, encoder.getA());
      setIn(cpu, 1, encoder.getB());

      cpu.tick();
    }
  }

  @Override
  public String toString() {
    return "in.enable=" + (enable ? "1" : "0") +
        ", in.runLength=" + runLength +
        ", out.counter=" + counter +
        ", out.position=" + position +
        ", out.zeroDetected=" + (zeroDetected ? "1" : "0") +
        ", out.offset=" + offset +
        ", out.state=" + (state == 0 ? "STOP" : (state == 1 ? "MOVING" : "DONE")) +
        ", in4_raw=" + (in4raw ? "1" : "0") +
        ", out.in4_filtered=" + (in4 ? "1" : "0");
  }
}
