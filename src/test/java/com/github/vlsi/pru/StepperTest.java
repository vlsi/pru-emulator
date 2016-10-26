package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.BinaryCode;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.annotations.Test;
import st61131.pru.PRU_STEPPER_CodeGenerator;
import st61131.pru.PRU_STEP_CONTROL_CodeGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

public class StepperTest {

  @Test
  public void stepperBlock() throws IOException {
    PRU_STEPPER_CodeGenerator g = new PRU_STEPPER_CodeGenerator();
    CodeEmitter ce = new CodeEmitter();
    g.accept(ce);

    BinaryCode code = ce.visitEnd();


    Pru cpu = new Pru();
    cpu.setCode(code);

    cpu.setReg(new Register(8, RegisterField.b1), 1); // enable =>
    cpu.setReg(new Register(10, RegisterField.dw), 100); // accel_ramp =>
    cpu.setReg(new Register(11, RegisterField.dw), 50); // decel_ramp =>
    cpu.setReg(new Register(12, RegisterField.dw), 60); // max_speed =>
    int max = 10;//5 * 100000 + 100000 * 20 / 2 + 100000 * 15 / 2;
    cpu.setReg(new Register(9, RegisterField.dw),
        max); // quantity =>

    FileWriter fw = new FileWriter("target/pru_stop_accel.csv");
    BufferedWriter bw = new BufferedWriter(fw);
    bw.write("i,state,ct,dt\n");//,decel_start,decel_val,accel_count\n");
    for (int i = 0; i < max + 10; i++) {
      cpu.setPc(0);
      int time = cpu.runTillHalt(10000);
      int delay = cpu.getReg(new Register(6, RegisterField.dw));// step_delay =>
      int state = cpu.getReg(new Register(8, RegisterField.b0));// state =>
      int decel_start = cpu.getReg(new Register(7, RegisterField.dw));// decel_start =>
      int decel_val = cpu.getReg(new Register(3, RegisterField.dw));// decel_val =>
      int accel_count = cpu.getReg(new Register(2, RegisterField.dw));// accel_count =>
      bw.write(
          i + "," + state + "," + time + "," + delay);// + "," + decel_start + "," + decel_val + "," + accel_count);
      bw.write('\n');
      if (i == 1000) {
//        cpu.setReg(new Register(10, RegisterField.b1), 0);
      }

//      int rest = cpu.getReg(new Register(6, RegisterField.dw));// rest
//      System.out.println(state + "," + time + "," + delay + "," + Integer.toUnsignedString(rest));
    }
    bw.close();
  }


  private final static Register inReg = new Register(31, RegisterField.dw);
  private final static Register outReg = new Register(30, RegisterField.dw);

  private static boolean getOut(Pru cpu) {
    return ((cpu.getReg(outReg) >> 28) & 1) == 1;
  }

  private static boolean getReady(Pru cpu) {
    return readTransaction(cpu, ram -> ram.get(60) != 0);
  }

  private static boolean getReadyUnsync(Pru cpu) {
    return cpu.ram().get(60) != 0;
  }

  private static int getStateUnsync(Pru cpu) {
    return cpu.ram().get(64);
  }

  private static int getState(Pru cpu) {
    return readTransaction(cpu, ram -> ram.getInt(64));
  }

  private static int getQuantityLeft(Pru cpu) {
    return readTransaction(cpu, ram -> ram.getInt(68));
  }

  private static int getQuantityLeftUnsync(Pru cpu) {
    return cpu.ram().getInt(68);
  }

  private static void setIn1(Pru cpu, boolean value) {
    int reg = cpu.getReg(inReg);
    reg &= ~(1 << 21);
    if (value) {
      reg |= 1 << 21;
    }
    cpu.setReg(inReg, reg);
  }

  private static <T> T readTransaction(Pru cpu, Function<ByteBuffer, T> action) {
    ByteBuffer ram = cpu.ram();
    lockMemoryAccess(cpu, ram);
    ram.put(0, (byte) 1);
    lockMemoryAccess(cpu, ram);
    return action.apply(ram);
  }

  private static void writeTransaction(Pru cpu, Consumer<ByteBuffer> action) {
    ByteBuffer ram = cpu.ram();
    lockMemoryAccess(cpu, ram);
    action.accept(ram);
    ram.put(0, (byte) 1);
  }

  private static void lockMemoryAccess(Pru cpu, ByteBuffer ram) {
    int i;
    for (i = 0; i < 10000 && ram.get(0) != 0; i++) {
      cpu.tick();
    }
    if (i == 10000) {
      throw new IllegalStateException(
          "PRU should clear 0 byte in reasonable time. State: " + cpu.printState());
    }
  }


  private static void setQuantity(Pru cpu, int quantity) {
    writeTransaction(cpu, ram -> ram.putInt(16, quantity));
  }

  private static void setMaxSpeed(Pru cpu, int maxSpeed) {
    writeTransaction(cpu, ram -> ram.putInt(20, maxSpeed));
  }

  private static void setMinSpeed(Pru cpu, int minSpeed) {
    writeTransaction(cpu, ram -> ram.putInt(36, minSpeed));
  }

  private static void setEnable(Pru cpu, boolean enable) {
    writeTransaction(cpu, ram -> ram.put(24, (byte) (enable ? 1 : 0)));
  }

  private static void setAccelRamp(Pru cpu, int accelRamp) {
    writeTransaction(cpu, ram -> ram.putInt(28, accelRamp));
  }

  private static void setDecelRamp(Pru cpu, int decelRamp) {
    writeTransaction(cpu, ram -> ram.putInt(32, decelRamp));
  }

  private static void setPwm(Pru cpu, float value) {
    writeTransaction(cpu, ram -> ram.putInt(44, (int) (value * (65536))));
  }

  private static void setOutBit(Pru cpu, int outBit) {
    writeTransaction(cpu, ram -> ram.putInt(40, outBit));
  }

  @Test(enabled = false)
  public void stepperProgram() throws IOException {
    PRU_STEP_CONTROL_CodeGenerator g = new PRU_STEP_CONTROL_CodeGenerator();
    CodeEmitter ce = new CodeEmitter();
    g.accept(ce);

    BinaryCode code = ce.visitEnd();
    CodeSaver.save(code, "pru_stepper.bin");

    Pru cpu = new Pru();
    cpu.setCode(code);
    for (int i = new Random().nextInt(1000000); i > 0; i--) {
      cpu.tick();
    }

    setOutBit(cpu, 28);
    setQuantity(cpu, 30000);
    setMaxSpeed(cpu, 10000);
    setMinSpeed(cpu, 0);
    setAccelRamp(cpu, 0);
    setDecelRamp(cpu, 0);
    setPwm(cpu, 0.7f);
//    setQuantity(cpu, 100);y
//    setAccelRamp(cpu, 50);
//    setDecelRamp(cpu, 50);
//    setMaxSpeed(cpu, 30);

//    long seed = ThreadLocalRandom.current().nextLong();
//    Random rnd = new Random(seed);
//    // Burn PRU for a while
//    for (int i = rnd.nextInt(100); i > 0; i--) {
//      cpu.tick();
//    }
//

    boolean ready = getReady(cpu);
    System.out.println("ready = " + ready);
    System.out.println("getQuantityLeftUnsync(cpu) = " + getQuantityLeftUnsync(cpu));

    setEnable(cpu, true);

    boolean lastOut = getOut(cpu);
    long last = 0;
    int k = 0;
    boolean stopFlag = false;
    Writer fw = new BufferedWriter(new FileWriter("target/stepperProgramOut.csv"));
    int plcCycle = 200000000 / 2000;
    int prevCycle = 0;
    int pulsesGenerated = 0;
    int state = getStateUnsync(cpu);
    for (long i = 0; i < 610010079; i++) {
      long cc = cpu.getCycleCountNonReset();

      if (i / plcCycle != prevCycle) {
        prevCycle = (int) (i / plcCycle);
        setQuantity(cpu, 30000);
        setMaxSpeed(cpu, 10000);
        setMinSpeed(cpu, 0);
        setAccelRamp(cpu, 0);
        setDecelRamp(cpu, 0);
        setEnable(cpu, true);//getState(cpu) != 4);
        ready = getReady(cpu);
        pulsesGenerated = getQuantityLeft(cpu);
        state = getState(cpu);
      }

      cc = cpu.getCycleCountNonReset() - cc;
      i += cc;
      if (stopFlag && state == 0) {
//        System.out.println("START: " + (last-i) + ", "+i + ", " + getStateUnsync(cpu));
//        setEnable(cpu, true);
        stopFlag = false;
        k = 0;
      }

      cpu.tick();
      boolean out = getOut(cpu);
      if (out == lastOut) {
        continue;
      }
      String msg = (out ? '1' : '0') + " " + (i - last) + " " + i + " " + getStateUnsync(
          cpu) + " " + ready + " " + pulsesGenerated;
      if ((k % 1000) == 0) {
        System.out.println(k + ")" + msg);
      }
      fw.append(msg);
      fw.append('\n');
      fw.flush();
      lastOut = out;
      last = i;
      k++;
      if (k == 30) {
//        System.out.println("STOP: " + (last-i) + ", "+i + ", " + getStateUnsync(cpu));
//        setEnable(cpu, false);
//        stopFlag=true;
      }
    }
    System.out.println("k = " + k);
    fw.close();
  }
}
