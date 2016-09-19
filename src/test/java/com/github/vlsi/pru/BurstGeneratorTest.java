package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.CodeEmitter;

import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import com.github.vlsi.pru.plc110.MemoryTransferInstruction;
import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

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

  private static boolean getIn1(Pru cpu) {
    return ((cpu.getReg(inReg) >> 21) & 1) == 1;
  }

  private static boolean getOut(Pru cpu) {
    return ((cpu.getReg(outReg) >> 28) & 1) == 1;
  }

  private static boolean getReady(Pru cpu) {
    return ((cpu.getReg(outReg) >> 29) & 1) == 1;
  }

  private static void setIn1(Pru cpu, boolean value) {
    int reg = cpu.getReg(inReg);
    reg &= ~(1 << 21);
    if (value) {
      reg |= 1 << 21;
    }
    cpu.setReg(inReg, reg);
  }

  @Test
  public void test() {
    int cycleLength = 33;
    int numPulses = 2;
    final int edgeTolerance = 330;

    CodeEmitter ce = new CodeEmitter();
    generateCode(ce, cycleLength);

    Pru cpu = new Pru();
    cpu.setCode(ce.visitEnd());

    cpu.ram().putShort(2, (short) cycleLength);
    cpu.ram().putShort(0, (short) numPulses);

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
      boolean ready = getReady(cpu);
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
          setIn1(cpu, true);
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
            setIn1(cpu, false);
            cycleLength++;
            numPulses++;
            cpu.ram().putShort(2, (short) cycleLength);
            cpu.ram().putShort(0, (short) numPulses);
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

  private void generateCode(CodeEmitter ce, int cycleLength) {
    Label startWhileBody0 = new Label("startWhileBody0");
    Label if3 = new Label("if3");
    Label if5 = new Label("if5");
    Label elsIf6 = new Label("elsIf6");
    Label endIf4 = new Label("endIf4");
    Label endIf2 = new Label("endIf2");
    Label if8 = new Label("if8");
    Label endIf9 = new Label("endIf9");
    Label startWhileBody10 = new Label("startWhileBody10");
    Label endWhile11 = new Label("endWhile11");
    Label endIf7 = new Label("endIf7");
    Label if13 = new Label("if13");
    Label endIf12 = new Label("endIf12");
    Label if15 = new Label("if15");
    Label endIf14 = new Label("endIf14");
    Label endWhile1 = new Label("endWhile1");
    ce.visitLabel(startWhileBody0);
    // собственно полезная работа
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(2, RegisterField.w0)).setAddress(3).setOffset(0).setLength(2).encode());
    //
    // Call PRU_IN1
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSR,
        new Register(1, RegisterField.b3), new Register(31, RegisterField.dw), 21));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.AND,
        new Register(1, RegisterField.b3), new Register(1, RegisterField.b3), 1));

    //
    // End PRU_IN1
    // Call PRU_GENER_BURST
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, if3,
        new Register(1, RegisterField.b3), 0));
    // Выключаемся
    ce.visitInstruction(new LdiInstruction(new Register(1, RegisterField.w0), (short) 0));
    ce.visitInstruction(new LdiInstruction(new Register(1, RegisterField.b2), (short) 0));

    ce.visitInstruction(new QuickBranchInstruction(endIf2));
    ce.visitLabel(if3);
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if5,
        new Register(1, RegisterField.w0), 0));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, elsIf6,
        new Register(1, RegisterField.b2), 0));
    // Поступила команда на включение
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(1, RegisterField.w0), new Register(2, RegisterField.w0), 1));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(1, RegisterField.w0), new Register(1, RegisterField.w0), 1));

    ce.visitInstruction(new QuickBranchInstruction(endIf4));
    ce.visitLabel(if5);
    // Идёт генерация
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(1, RegisterField.w0), new Register(1, RegisterField.w0), 1));

    ce.visitInstruction(new QuickBranchInstruction(endIf4));
    ce.visitLabel(elsIf6);
    // Всё сгенерировали, ждём пока передёрнут enable для следующего включения

    ce.visitLabel(endIf4);

    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.MIN,
        new Register(1, RegisterField.b2), new Register(1, RegisterField.w0), 1));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(1, RegisterField.b2), new Register(1, RegisterField.b2), 1));


    ce.visitLabel(endIf2);

    // Если всё сделали, то out выключится. Если пачка ещё генерируется, то младший бит и есть меандр
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.AND,
        new Register(1, RegisterField.b3), new Register(1, RegisterField.w0), 1));
    //
    // End PRU_GENER_BURST
    //
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(2, RegisterField.w0)).setAddress(3).setOffset(2).setLength(2).encode());
    // Параметр pruCycleLength это "количество тактов"
    // Т.е. 200 это цикл в 1 мкс
    //
    // Call WAIT_TICK
    // 0x00007000..0x00007FFF -- PRU0 Control Registers, 0xC -- cycle count register
    ce.visitInstruction(new LdiInstruction(new Register(3, RegisterField.dw), (short) 28684));
    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.LOAD,
        new Register(2, RegisterField.w2)).setAddress(new Register(3, RegisterField.dw)).setOffset(
        0).setLength(2).encode());
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(2, RegisterField.w2), new Register(2, RegisterField.w2), 8));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, if8,
        new Register(2, RegisterField.w0), new Register(2, RegisterField.w2)));
    ce.visitInstruction(new LdiInstruction(new Register(4, RegisterField.w0), (short) 0));

    ce.visitInstruction(new QuickBranchInstruction(endIf7));
    ce.visitLabel(if8);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(4, RegisterField.w0), new Register(2, RegisterField.w0),
        new Register(2, RegisterField.w2)));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(4, RegisterField.w0), new Register(4, RegisterField.w0), 0));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.BC, endIf9,
        new Register(4, RegisterField.w0), 0));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.XOR,
        new Register(4, RegisterField.w0), new Register(4, RegisterField.w0), 1));
    ce.visitLabel(endIf9);

    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endWhile11,
        new Register(4, RegisterField.w0), 0));
    ce.visitLabel(startWhileBody10);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(4, RegisterField.w0), new Register(4, RegisterField.w0), 2));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, startWhileBody10,
            new Register(4, RegisterField.w0), 0));
    ce.visitLabel(endWhile11);


    ce.visitLabel(endIf7);

    ce.visitInstruction(new MemoryTransferInstruction(MemoryTransferInstruction.Operation.STORE,
        new Register(4, RegisterField.w0)).setAddress(new Register(3, RegisterField.dw)).setOffset(
        0).setLength(2).encode());
    //
    // End WAIT_TICK
    //
    // Call PRU_OUT1
    // Ага, ассемблерные вставки
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, if13,
        new Register(1, RegisterField.b3), 0));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.CLR,
        new Register(30, RegisterField.dw), new Register(30, RegisterField.dw), 28));

    ce.visitInstruction(new QuickBranchInstruction(endIf12));
    ce.visitLabel(if13);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SET,
        new Register(30, RegisterField.dw), new Register(30, RegisterField.dw), 28));

    ce.visitLabel(endIf12);

    // End PRU_OUT1
    // Call PRU_OUT2
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(1, RegisterField.b3), new Register(1, RegisterField.b2), 0));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, if15,
        new Register(1, RegisterField.b3), 0));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.CLR,
        new Register(30, RegisterField.dw), new Register(30, RegisterField.dw), 29));

    ce.visitInstruction(new QuickBranchInstruction(endIf14));
    ce.visitLabel(if15);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SET,
        new Register(30, RegisterField.dw), new Register(30, RegisterField.dw), 29));

    ce.visitLabel(endIf14);

    // End PRU_OUT2
    //
    // Тут будет передача данных из/в host

    ce.visitInstruction(new QuickBranchInstruction(startWhileBody0));
    ce.visitLabel(endWhile1);
  }
}
