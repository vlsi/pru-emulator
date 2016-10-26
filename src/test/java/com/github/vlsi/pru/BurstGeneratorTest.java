package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.BinaryCode;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.MemoryTransferInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.Test;
import st61131.pru.PRU_PULSE_GENERATOR_CodeGenerator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

public class BurstGeneratorTest {

  enum BurstState {
    DONE,
    STARTED,
    GENERATING,
    WAIT_READY,
    GENERATED,
    DISABLING,
  }

  private final static Register inReg = new Register(31, RegisterField.dw);
  private final static Register outReg = new Register(30, RegisterField.dw);

  private static boolean getOut(Pru cpu) {
    return ((cpu.getReg(outReg) >> 28) & 1) == 1;
  }

  private static boolean getReady(Pru cpu) {
    return readTransaction(cpu, ram -> ram.get(112) != 0);
  }

  private static boolean getReadyUnsync(Pru cpu) {
    return cpu.ram().get(112) != 0;
  }

  private static int getQuantityLeft(Pru cpu) {
    return readTransaction(cpu, ram -> ram.getInt(116));
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
    writeTransaction(cpu, ram -> ram.putInt(100, quantity));
  }

  private static void setEnable(Pru cpu, boolean enable) {
    writeTransaction(cpu, ram -> ram.put(108, (byte) (enable ? 1 : 0)));
  }

  private static void setCycleLength(Pru cpu, int cycleLength) {
    writeTransaction(cpu, ram -> ram.putShort(104, (short) cycleLength));
  }

  @Test
  public void test() throws IOException {
    int cycleLength = 35;
    int numPulses = 2;
    final int edgeTolerance = 330;

    CodeEmitter ce = new CodeEmitter();
    new PRU_PULSE_GENERATOR_CodeGenerator().accept(ce);

    BinaryCode code = ce.visitEnd();
    CodeSaver.save(code, "burst.bin");

    Pru cpu = new Pru();
    cpu.setCode(code);

    setCycleLength(cpu, cycleLength);
    setQuantity(cpu, numPulses);

    long seed = ThreadLocalRandom.current().nextLong();
    Random rnd = new Random(seed);
    // Burn PRU for a while
    for (int i = rnd.nextInt(100); i > 0; i--) {
      cpu.tick();
    }

    // enable == true;  wait N bursts;  wait stable;  enable == false; wait ready = false
    int lastStart = 0;
    int edgeStarted = 0;
    int disableStarted = 0;
    int quietStarted = 0;
    int disableEnded = 0;
    int pulesToGo = 0;
    boolean lastOut = false;
    BurstState state = BurstState.DONE;
    for (int i = 0; i < 10000; i++) {
      boolean out = getOut(cpu);
      int oldCycles = cpu.getCycleCountNonReset();
      boolean ready = getReadyUnsync(cpu);
      i += cpu.getCycleCountNonReset() - oldCycles;
      switch (state) {
        case DONE:
          if (out || ready) {
            Assert.assertEquals("out = " + out + ", ready = " + ready, "out = false, ready = false",
                "State is " + state);
          }
          int rndRange = (disableEnded + cycleLength) - i;
          if (rndRange > 0 && rnd.nextInt(rndRange) != 0) {
            break;
          }
          oldCycles = cpu.getCycleCountNonReset();
          setEnable(cpu, true);
          i += cpu.getCycleCountNonReset() - oldCycles;
          state = BurstState.STARTED;
          lastStart = i;
          pulesToGo = numPulses;
          break;
        case STARTED:
          if (!out) {
            int maxDelay = cycleLength * 2 + edgeTolerance;
            if (i - lastStart > maxDelay) {
              Assert.fail(
                  "Burst should be started in 0.." + maxDelay +
                      ", however output is still false. lastStart=" + lastStart + ", currentTick = " + i +
                      ", cpu state: " + cpu.printState());
            }
            break;
          }
          System.out.print(
              "cycle: " + cycleLength + ", quantity: " + numPulses + ". _" + (i - lastStart) + "_/‾");
          edgeStarted = i;
          state = BurstState.GENERATING;
          lastOut = true;
          break;
        case GENERATING:
          Assert.assertFalse(ready,
              "Expected " + pulesToGo + " more pulses to be generated, so ready should be FALSE");
          if (out == lastOut) {
            if (i - edgeStarted > cycleLength + edgeTolerance) {
              Assert.fail(
                  "out=" + out + " was longer than " + (cycleLength + edgeTolerance) +
                      ". Expecting output flip on each PRU program loop (each " + cycleLength + " ticks) cpu = " + cpu.printState());
            }
            break;
          }
          if (i - edgeStarted < cycleLength - edgeTolerance) {
            Assert.fail(
                "out switched from " + lastOut + " to " + out + " too soon. Ticks passed " + (i - edgeStarted) +
                    ", minimal expected flip duration " + (cycleLength - edgeTolerance) +
                    ". Expecting output flip on each PRU program loop (each " + cycleLength + " ticks)");
          }
          System.out.print((i - edgeStarted) + (lastOut ? "‾\\_" : "_/‾"));
          lastOut = out;
          edgeStarted = i;
          if (!out) {
            pulesToGo--;
          }
          if (pulesToGo == 0) {
            state = BurstState.WAIT_READY;
          }
          break;
        case WAIT_READY:
          Assert.assertEquals(false, out,
              "Output should not be activated while in WAIT_READY state");
          oldCycles = cpu.getCycleCountNonReset();
          ready = getReady(cpu);
          i += cpu.getCycleCountNonReset() - oldCycles;
          if (!ready) {
            if (i - edgeStarted > cycleLength + edgeTolerance) {
              Assert.fail(
                  "Unable to disable generator block within " + (cycleLength + edgeTolerance) +
                      " ticks. Expecting ready to become false since enabled is false");
            }
            break;
          }
          state = BurstState.GENERATED;
          quietStarted = i;
          break;
        case GENERATED:
          Assert.assertEquals(false, out,
              "Output should not be activated while in GENERATED state");
          oldCycles = cpu.getCycleCountNonReset();
          ready = getReady(cpu);
          i += cpu.getCycleCountNonReset() - oldCycles;
          if (!ready) {
            if (i - quietStarted > cycleLength + edgeTolerance) {
              Assert.fail(
                  "Unable to disable generator block within " + (cycleLength + edgeTolerance) +
                      " ticks. Expecting ready to become false since enabled is false");
            }
            break;
          }
          if (out || !ready) {
            Assert.assertEquals("out = " + out + ", ready = " + ready, "out = false, ready = true",
                "Everything was generated " + (i - quietStarted) + " ticks ago");
          }

          if (i - quietStarted > cycleLength * 5) {
            disableStarted = i;
            state = BurstState.DISABLING;
            oldCycles = cpu.getCycleCountNonReset();
            setEnable(cpu, false);
            i += cpu.getCycleCountNonReset() - oldCycles;
            cycleLength++;
            numPulses++;
            oldCycles = cpu.getCycleCountNonReset();
            setCycleLength(cpu, cycleLength);
            setQuantity(cpu, numPulses);
            i += cpu.getCycleCountNonReset() - oldCycles;
          }

          break;
        case DISABLING:
          Assert.assertEquals(false, out,
              "Output should not be activated while in DISABLING state");
          if (i - disableStarted > cycleLength + edgeTolerance) {
            Assert.fail(
                "Unable to disable generator block within " + (cycleLength + edgeTolerance) +
                    " ticks. Expecting ready to become false since enabled is false. CPU: " + cpu.printState());
          }
          Assert.assertFalse(out, "Enable == false, thus out should not be activated");
          if (!ready) {
            state = BurstState.DONE;
            disableEnded = i;
            System.out.println();
          }
          break;
        default:
          throw new IllegalStateException("Unexpected state " + state);
      }

      cpu.tick();
    }
  }

  private void generateCode1(CodeEmitter ce) {
    Label startWhileBody0 = new Label("startWhileBody0");
    Label if3 = new Label("if3");
    Label if5 = new Label("if5");
    Label elsIf6 = new Label("elsIf6");
    Label endIf4 = new Label("endIf4");
    Label endIf2 = new Label("endIf2");
    Label startRepeat7 = new Label("startRepeat7");
    Label endIf8 = new Label("endIf8");
    Label if10 = new Label("if10");
    Label endIf11 = new Label("endIf11");
    Label startWhileBody12 = new Label("startWhileBody12");
    Label endWhile13 = new Label("endWhile13");
    Label endIf9 = new Label("endIf9");
    Label if15 = new Label("if15");
    Label endIf14 = new Label("endIf14");
    Label endWhile1 = new Label("endWhile1");
    // безопасные значения
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b1), (short) 0).setComment(
        "enable => R2.b1"));
    ce.visitInstruction(
        new LdiInstruction(new Register(2, RegisterField.w2), (short) 100).setComment(
            "cycleLength => R2.w2"));
    ce.visitInstruction(new LdiInstruction(new Register(3, RegisterField.dw), (short) 0).setComment(
        "quantity => R3"));
    ce.visitLabel(startWhileBody0);
    // собственно полезная работа
    // Call PRU_GENER_BURST
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.b0), new Register(2, RegisterField.b1), 0).setComment(
        "burst_enable => R4.b0, enable => R2.b1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(5, RegisterField.dw), new Register(3, RegisterField.dw), 0).setComment(
        "burst_quantity => R5, quantity => R3"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, if3,
        new Register(4, RegisterField.b0), 0).setComment("burst_enable => R4.b0"));
    // Выключаемся
    ce.visitInstruction(new LdiInstruction(new Register(1, RegisterField.dw), (short) 0).setComment(
        "burst_qtyLeft => R1"));
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b0), (short) 0).setComment(
        "burst_ready => R2.b0"));

    ce.visitInstruction(new QuickBranchInstruction(endIf2).setComment(""));
    ce.visitLabel(if3);
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if5,
        new Register(1, RegisterField.dw), 0).setComment("burst_qtyLeft => R1"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, elsIf6,
        new Register(2, RegisterField.b0), 0).setComment("burst_ready => R2.b0"));
    // Поступила команда на включение
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(1, RegisterField.dw), new Register(5, RegisterField.dw), 1).setComment(
        "burst_qtyLeft => R1, burst_quantity => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(1, RegisterField.dw), new Register(1, RegisterField.dw), 1).setComment(
        "burst_qtyLeft => R1"));

    ce.visitInstruction(new QuickBranchInstruction(endIf4).setComment(""));
    ce.visitLabel(if5);
    // Идёт генерация
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(1, RegisterField.dw), new Register(1, RegisterField.dw), 1).setComment(
        "burst_qtyLeft => R1"));

    ce.visitInstruction(new QuickBranchInstruction(endIf4).setComment(""));
    ce.visitLabel(elsIf6);
    // Всё сгенерировали, ждём пока передёрнут enable для следующего включения

    ce.visitLabel(endIf4);

    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.MIN,
        new Register(2, RegisterField.b0), new Register(1, RegisterField.dw), 1).setComment(
        "burst_ready => R2.b0, burst_qtyLeft => R1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(2, RegisterField.b0), new Register(2, RegisterField.b0), 1).setComment(
        "burst_ready => R2.b0"));


    ce.visitLabel(endIf2);

    // Если всё сделали, то out выключится. Если пачка ещё генерируется, то младший бит и есть меандр
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.AND,
        new Register(4, RegisterField.b0), new Register(1, RegisterField.dw), 1).setComment(
        "burst_out => R4.b0, burst_qtyLeft => R1"));
    //
    // End PRU_GENER_BURST
    //
    ce.visitInstruction(
        new LdiInstruction(new Register(5, RegisterField.dw), (short) 30732).setComment(
            "controlRegisterAddress => R5"));
    //
    ce.visitLabel(startRepeat7);
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(4, RegisterField.b1)).setAddress(3).setOffset(0).setLength(
        1).encode().setComment("dataReady => R4.b1"));
    //
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endIf8,
        new Register(4, RegisterField.b1), 0).setComment("dataReady => R4.b1"));
    // Загружаем параметры
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(3, RegisterField.dw)).setAddress(3).setOffset(100).setLength(
        4).encode().setComment("quantity => R3"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(2, RegisterField.w2)).setAddress(3).setOffset(104).setLength(
        2).encode().setComment("cycleLength => R2.w2"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(2, RegisterField.b1)).setAddress(3).setOffset(108).setLength(
        1).encode().setComment("enable => R2.b1"));

    //
    // Выводим выгружаем состояние в ПЛК
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.b2), new Register(2, RegisterField.b0), 0).setComment(
        "ready => R4.b2, burst_ready => R2.b0"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(4, RegisterField.b2)).setAddress(3).setOffset(112).setLength(
        1).encode().setComment("ready => R4.b2"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(6, RegisterField.dw), new Register(1, RegisterField.dw), 0).setComment(
        "qtyLeft => R6, burst_qtyLeft => R1"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(6, RegisterField.dw)).setAddress(3).setOffset(116).setLength(
        4).encode().setComment("qtyLeft => R6"));
    ce.visitInstruction(new LdiInstruction(new Register(4, RegisterField.b1), (short) 0).setComment(
        "dataReady => R4.b1"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(4, RegisterField.b1)).setAddress(3).setOffset(0).setLength(
        1).encode().setComment("dataReady => R4.b1"));

    ce.visitLabel(endIf8);

    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(4, RegisterField.w2)).setAddress(new Register(5, RegisterField.dw)).setOffset(
        0).setLength(2).encode().setComment(
        "currentCycles => R4.w2, controlRegisterAddress => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.w2), new Register(4, RegisterField.w2), 40).setComment(
        "currentCycles => R4.w2"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.GT, startRepeat7,
            new Register(4, RegisterField.w2), new Register(2, RegisterField.w2)).setComment(
            "currentCycles => R4.w2, cycleLength => R2.w2"));

    //
    // Call WAIT_TICK
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.w2), new Register(2, RegisterField.w2), 0).setComment(
        "tmpWAIT_TICK_0_pruCycleLength => R4.w2, cycleLength => R2.w2"));
    // 0x00007000..0x00007FFF -- PRU0 Control Registers, 0xC -- cycle count register
    ce.visitInstruction(
        new LdiInstruction(new Register(5, RegisterField.dw), (short) 30732).setComment(
            "tmpWAIT_TICK_0_controlRegisterAddress => R5"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(6, RegisterField.w0)).setAddress(new Register(5, RegisterField.dw)).setOffset(
        0).setLength(2).encode().setComment(
        "Load cycle count, 1+wdcnt*1==2 cycles, tmpWAIT_TICK_0_currentCycles => R6.w0, tmpWAIT_TICK_0_controlRegisterAddress => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(6, RegisterField.w0), new Register(6, RegisterField.w0), 8).setComment(
        "tmpWAIT_TICK_0_currentCycles => R6.w0"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if10,
        new Register(4, RegisterField.w2), new Register(6, RegisterField.w0)).setComment(
        "tmpWAIT_TICK_0_pruCycleLength => R4.w2, tmpWAIT_TICK_0_currentCycles => R6.w0"));
    ce.visitInstruction(new LdiInstruction(new Register(6, RegisterField.w2), (short) 0).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2"));

    ce.visitInstruction(new QuickBranchInstruction(endIf9).setComment(""));
    ce.visitLabel(if10);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(6, RegisterField.w2), new Register(4, RegisterField.w2),
        new Register(6, RegisterField.w0)).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2, tmpWAIT_TICK_0_pruCycleLength => R4.w2, tmpWAIT_TICK_0_currentCycles => R6.w0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(6, RegisterField.w2), new Register(6, RegisterField.w2), 0).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.BC, endIf11,
        new Register(6, RegisterField.w2), 0).setComment("tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(6, RegisterField.w2), new Register(6, RegisterField.w2), 1).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitLabel(endIf11);

    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endWhile13,
        new Register(6, RegisterField.w2), 0).setComment("tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitLabel(startWhileBody12);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(6, RegisterField.w2), new Register(6, RegisterField.w2), 2).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, startWhileBody12,
            new Register(6, RegisterField.w2), 0).setComment("tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitLabel(endWhile13);


    ce.visitLabel(endIf9);

    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(6, RegisterField.w2)).setAddress(new Register(5, RegisterField.dw)).setOffset(
        0).setLength(2).encode().setComment(
        "Load cycle count, 1+wdcnt*1==2 cycles, tmpWAIT_TICK_0_cyclesLeft => R6.w2, tmpWAIT_TICK_0_controlRegisterAddress => R5"));
    //
    // End WAIT_TICK
    //
    // Call PRU_OUT1
    // Ага, ассемблерные вставки
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, if15,
        new Register(4, RegisterField.b0), 0).setComment("tmpPRU_OUT1_0_Q => R4.b0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.CLR,
        new Register(30, RegisterField.dw), new Register(30, RegisterField.dw), 28).setComment(""));

    ce.visitInstruction(new QuickBranchInstruction(endIf14).setComment(""));
    ce.visitLabel(if15);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SET,
        new Register(30, RegisterField.dw), new Register(30, RegisterField.dw), 28).setComment(""));

    ce.visitLabel(endIf14);

    // End PRU_OUT1

    ce.visitInstruction(new QuickBranchInstruction(startWhileBody0).setComment(""));
    ce.visitLabel(endWhile1);
  }

  private void generateCode(CodeEmitter ce) {
    Label startWhileBody0 = new Label("startWhileBody0");
    Label if3 = new Label("if3");
    Label if5 = new Label("if5");
    Label elsIf6 = new Label("elsIf6");
    Label endIf4 = new Label("endIf4");
    Label endIf2 = new Label("endIf2");
    Label startRepeat7 = new Label("startRepeat7");
    Label endIf8 = new Label("endIf8");
    Label if10 = new Label("if10");
    Label endIf11 = new Label("endIf11");
    Label startWhileBody12 = new Label("startWhileBody12");
    Label endWhile13 = new Label("endWhile13");
    Label endIf9 = new Label("endIf9");
    Label if15 = new Label("if15");
    Label endIf14 = new Label("endIf14");
    Label endWhile1 = new Label("endWhile1");
    // безопасные значения
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b1), (short) 0).setComment(
        "enable => R2.b1"));
    ce.visitInstruction(
        new LdiInstruction(new Register(2, RegisterField.w2), (short) 100).setComment(
            "cycleLength => R2.w2"));
    ce.visitInstruction(new LdiInstruction(new Register(3, RegisterField.dw), (short) 0).setComment(
        "quantity => R3"));
    ce.visitLabel(startWhileBody0);
    // собственно полезная работа
    // Call PRU_GENER_BURST
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.b0), new Register(2, RegisterField.b1), 0).setComment(
        "burst_enable => R4.b0, enable => R2.b1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(5, RegisterField.dw), new Register(3, RegisterField.dw), 0).setComment(
        "burst_quantity => R5, quantity => R3"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, if3,
        new Register(4, RegisterField.b0), 0).setComment("burst_enable => R4.b0"));
    // Выключаемся
    ce.visitInstruction(new LdiInstruction(new Register(1, RegisterField.dw), (short) 0).setComment(
        "burst_qtyLeft => R1"));
    ce.visitInstruction(new LdiInstruction(new Register(2, RegisterField.b0), (short) 0).setComment(
        "burst_ready => R2.b0"));

    ce.visitInstruction(new QuickBranchInstruction(endIf2).setComment(""));
    ce.visitLabel(if3);
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if5,
        new Register(1, RegisterField.dw), 0).setComment("burst_qtyLeft => R1"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, elsIf6,
        new Register(2, RegisterField.b0), 0).setComment("burst_ready => R2.b0"));
    // Поступила команда на включение
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(1, RegisterField.dw), new Register(5, RegisterField.dw), 1).setComment(
        "burst_qtyLeft => R1, burst_quantity => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(1, RegisterField.dw), new Register(1, RegisterField.dw), 1).setComment(
        "burst_qtyLeft => R1"));

    ce.visitInstruction(new QuickBranchInstruction(endIf4).setComment(""));
    ce.visitLabel(if5);
    // Идёт генерация
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(1, RegisterField.dw), new Register(1, RegisterField.dw), 1).setComment(
        "burst_qtyLeft => R1"));

    ce.visitInstruction(new QuickBranchInstruction(endIf4).setComment(""));
    ce.visitLabel(elsIf6);
    // Всё сгенерировали, ждём пока передёрнут enable для следующего включения

    ce.visitLabel(endIf4);

    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.MIN,
        new Register(2, RegisterField.b0), new Register(1, RegisterField.dw), 1).setComment(
        "burst_ready => R2.b0, burst_qtyLeft => R1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(2, RegisterField.b0), new Register(2, RegisterField.b0), 1).setComment(
        "burst_ready => R2.b0"));


    ce.visitLabel(endIf2);

    // Если всё сделали, то out выключится. Если пачка ещё генерируется, то младший бит и есть меандр
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.AND,
        new Register(4, RegisterField.b0), new Register(1, RegisterField.dw), 1).setComment(
        "burst_out => R4.b0, burst_qtyLeft => R1"));
    //
    // End PRU_GENER_BURST
    //
    ce.visitInstruction(
        new LdiInstruction(new Register(5, RegisterField.dw), (short) 28684).setComment(
            "controlRegisterAddress => R5"));
    ce.visitLabel(startRepeat7);
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(4, RegisterField.b1)).setAddress(3).setOffset(0).setLength(
        1).encode().setComment("dataReady => R4.b1"));
    //
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endIf8,
        new Register(4, RegisterField.b1), 0).setComment("dataReady => R4.b1"));
    // Загружаем параметры
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(3, RegisterField.dw)).setAddress(3).setOffset(100).setLength(
        4).encode().setComment("quantity => R3"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(2, RegisterField.w2)).setAddress(3).setOffset(104).setLength(
        2).encode().setComment("cycleLength => R2.w2"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(2, RegisterField.b1)).setAddress(3).setOffset(108).setLength(
        1).encode().setComment("enable => R2.b1"));

    //
    // Выводим выгружаем состояние в ПЛК
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.b2), new Register(2, RegisterField.b0), 0).setComment(
        "ready => R4.b2, burst_ready => R2.b0"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(4, RegisterField.b2)).setAddress(3).setOffset(112).setLength(
        1).encode().setComment("ready => R4.b2"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(6, RegisterField.dw), new Register(1, RegisterField.dw), 0).setComment(
        "qtyLeft => R6, burst_qtyLeft => R1"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(6, RegisterField.dw)).setAddress(3).setOffset(116).setLength(
        4).encode().setComment("qtyLeft => R6"));
    //
    ce.visitInstruction(new LdiInstruction(new Register(4, RegisterField.b1), (short) 0).setComment(
        "dataReady => R4.b1"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(4, RegisterField.b1)).setAddress(3).setOffset(0).setLength(
        1).encode().setComment("dataReady => R4.b1"));

    ce.visitLabel(endIf8);

    //
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(4, RegisterField.w2)).setAddress(new Register(5, RegisterField.dw)).setOffset(
        0).setLength(2).encode().setComment(
        "Load cycle count, 1+wdcnt*1==2 cycles, currentCycles => R4.w2, controlRegisterAddress => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.w2), new Register(4, RegisterField.w2), 40).setComment(
        "currentCycles => R4.w2"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.GT, startRepeat7,
            new Register(4, RegisterField.w2), new Register(2, RegisterField.w2)).setComment(
            "currentCycles => R4.w2, cycleLength => R2.w2"));

    //
    // Call WAIT_TICK
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.w2), new Register(2, RegisterField.w2), 0).setComment(
        "tmpWAIT_TICK_0_pruCycleLength => R4.w2, cycleLength => R2.w2"));
    // 0x00007000..0x00007FFF -- PRU0 Control Registers, 0xC -- cycle count register
    ce.visitInstruction(
        new LdiInstruction(new Register(5, RegisterField.dw), (short) 28684).setComment(
            "tmpWAIT_TICK_0_controlRegisterAddress => R5"));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(6, RegisterField.w0)).setAddress(new Register(5, RegisterField.dw)).setOffset(
        0).setLength(2).encode().setComment(
        "Load cycle count, 1+wdcnt*1==2 cycles, tmpWAIT_TICK_0_currentCycles => R6.w0, tmpWAIT_TICK_0_controlRegisterAddress => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(6, RegisterField.w0), new Register(6, RegisterField.w0), 8).setComment(
        "tmpWAIT_TICK_0_currentCycles => R6.w0"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if10,
        new Register(4, RegisterField.w2), new Register(6, RegisterField.w0)).setComment(
        "tmpWAIT_TICK_0_pruCycleLength => R4.w2, tmpWAIT_TICK_0_currentCycles => R6.w0"));
    ce.visitInstruction(new LdiInstruction(new Register(6, RegisterField.w2), (short) 0).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2"));

    ce.visitInstruction(new QuickBranchInstruction(endIf9).setComment(""));
    ce.visitLabel(if10);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(6, RegisterField.w2), new Register(4, RegisterField.w2),
        new Register(6, RegisterField.w0)).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2, tmpWAIT_TICK_0_pruCycleLength => R4.w2, tmpWAIT_TICK_0_currentCycles => R6.w0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(6, RegisterField.w2), new Register(6, RegisterField.w2), 0).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.BC, endIf11,
        new Register(6, RegisterField.w2), 0).setComment("tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(6, RegisterField.w2), new Register(6, RegisterField.w2), 1).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitLabel(endIf11);

    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endWhile13,
        new Register(6, RegisterField.w2), 0).setComment("tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitLabel(startWhileBody12);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(6, RegisterField.w2), new Register(6, RegisterField.w2), 2).setComment(
        "tmpWAIT_TICK_0_cyclesLeft => R6.w2"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, startWhileBody12,
            new Register(6, RegisterField.w2), 0).setComment("tmpWAIT_TICK_0_cyclesLeft => R6.w2"));
    ce.visitLabel(endWhile13);


    ce.visitLabel(endIf9);

    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(6, RegisterField.w2)).setAddress(new Register(5, RegisterField.dw)).setOffset(
        0).setLength(2).encode().setComment(
        "Load cycle count, 1+wdcnt*1==2 cycles, tmpWAIT_TICK_0_cyclesLeft => R6.w2, tmpWAIT_TICK_0_controlRegisterAddress => R5"));
    //
    // End WAIT_TICK
    //
    // Call PRU_OUT1
    // Ага, ассемблерные вставки
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, if15,
        new Register(4, RegisterField.b0), 0).setComment("tmpPRU_OUT1_0_Q => R4.b0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.CLR,
        new Register(30, RegisterField.dw), new Register(30, RegisterField.dw), 28).setComment(""));

    ce.visitInstruction(new QuickBranchInstruction(endIf14).setComment(""));
    ce.visitLabel(if15);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SET,
        new Register(30, RegisterField.dw), new Register(30, RegisterField.dw), 28).setComment(""));

    ce.visitLabel(endIf14);

    // End PRU_OUT1

    ce.visitInstruction(new QuickBranchInstruction(startWhileBody0).setComment(""));
    ce.visitLabel(endWhile1);
  }
}
