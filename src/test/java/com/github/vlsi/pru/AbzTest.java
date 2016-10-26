package com.github.vlsi.pru;

import static org.testng.Assert.assertEquals;

import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Pru;
import org.testng.annotations.Test;
import st61131.pru.PRU_ABZ_ENCODER_CodeGenerator;

import java.util.concurrent.ThreadLocalRandom;

public class AbzTest {
  private void executeBlock(Pru cpu) {
    cpu.setPc(0);
    cpu.runTillHalt(100);
  }

  @Test
  public void ccw() {
    PRU_ABZ_ENCODER_CodeGenerator abz = new PRU_ABZ_ENCODER_CodeGenerator();

    Pru cpu = new Pru();
    CodeEmitter ce = new CodeEmitter();
    abz.accept(ce);
    cpu.setCode(ce.visitEnd());

    abz.setA(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 1, "counter");
    assertEquals(abz.getPosition(cpu), ((-1) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    executeBlock(cpu); // Если входы не менялись, выходы меняться не должны
    assertEquals(abz.getCounter(cpu), 1, "counter");
    assertEquals(abz.getPosition(cpu), ((-1) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setB(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 2, "counter");
    assertEquals(abz.getPosition(cpu), ((-2) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setA(cpu, 0);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 3, "counter");
    assertEquals(abz.getPosition(cpu), ((-3) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setB(cpu, 0);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 4, "counter");
    assertEquals(abz.getPosition(cpu), ((-4) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setA(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 5, "counter");
    assertEquals(abz.getPosition(cpu), ((-5) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setB(cpu, 1);
    abz.setZ(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 6, "counter");
    assertEquals(abz.getPosition(cpu), ((0) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 1, "zero detected");
  }

  @Test
  public void ccwCw() {
    PRU_ABZ_ENCODER_CodeGenerator abz = new PRU_ABZ_ENCODER_CodeGenerator();

    Pru cpu = new Pru();
    CodeEmitter ce = new CodeEmitter();
    abz.accept(ce);
    cpu.setCode(ce.visitEnd());

//     Приходит фаза A; B ещё нет
//    abz.setB(cpu, 1); executeBlock(cpu);
//    System.out.println("position = " + (short)(abz.getPosition(cpu)) + ", counter = " + abz.getCounter(cpu));
//
    // Реверс -- фаза A пропала, B тоже молчит
//    abz.setB(cpu, 0); executeBlock(cpu);
//    System.out.println("position = " + (short)(abz.getPosition(cpu)) + ", counter = " + abz.getCounter(cpu));


    abz.setA(cpu, 1); // A: 0 -> 1
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 1, "counter");
    assertEquals(abz.getPosition(cpu), ((-1) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    executeBlock(cpu); // Если входы не менялись, выходы меняться не должны
    assertEquals(abz.getCounter(cpu), 1, "counter");
    assertEquals(abz.getPosition(cpu), ((-1) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setB(cpu, 1); // B: 0 -> 1
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 2, "counter");
    assertEquals(abz.getPosition(cpu), ((-2) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setA(cpu, 0); // A: 1 -> 0
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 3, "counter");
    assertEquals(abz.getPosition(cpu), ((-3) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    // Теперь крутим обратно
    abz.setA(cpu, 1); // A: 0 -> 1
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 4, "counter");
    assertEquals(abz.getPosition(cpu), ((-2) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setB(cpu, 0); // B: 1 -> 0
    executeBlock(cpu); // Если входы не менялись, выходы меняться не должны
    assertEquals(abz.getCounter(cpu), 5, "counter");
    assertEquals(abz.getPosition(cpu), ((-1) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    // Теперь снова меняем направление

    abz.setB(cpu, 1); // B: 0 -> 1
    executeBlock(cpu); // Если входы не менялись, выходы меняться не должны
    assertEquals(abz.getCounter(cpu), 6, "counter");
    assertEquals(abz.getPosition(cpu), ((-2) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setA(cpu, 0); // A: 1 -> 0
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 7, "counter");
    assertEquals(abz.getPosition(cpu), ((-3) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    /*abz.setA(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 5, "counter");
    assertEquals(abz.getPosition(cpu), ((-5) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setB(cpu, 1);
    abz.setZ(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 6, "counter");
    assertEquals(abz.getPosition(cpu), ((0) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 1, "zero detected");*/
  }

  @Test
  public void cw() {
    PRU_ABZ_ENCODER_CodeGenerator abz = new PRU_ABZ_ENCODER_CodeGenerator();

    Pru cpu = new Pru();
    CodeEmitter ce = new CodeEmitter();
    abz.accept(ce);
    cpu.setCode(ce.visitEnd());

    abz.setB(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 1, "counter");
    assertEquals(abz.getPosition(cpu), ((1) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 1, "counter");
    assertEquals(abz.getPosition(cpu), ((1) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setA(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 2, "counter");
    assertEquals(abz.getPosition(cpu), ((2) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setB(cpu, 0);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 3, "counter");
    assertEquals(abz.getPosition(cpu), ((3) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setA(cpu, 0);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 4, "counter");
    assertEquals(abz.getPosition(cpu), ((4) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setB(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 5, "counter");
    assertEquals(abz.getPosition(cpu), ((5) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 0, "zero detected");

    abz.setA(cpu, 1);
    abz.setZ(cpu, 1);
    executeBlock(cpu);
    assertEquals(abz.getCounter(cpu), 6, "counter");
    assertEquals(abz.getPosition(cpu), ((0) & 0xffff), "position");
    assertEquals(abz.getZeroDetected(cpu), 1, "zero detected");
  }

  @Test
  public void drunkEncoder() {
    PRU_ABZ_ENCODER_CodeGenerator abz = new PRU_ABZ_ENCODER_CodeGenerator();

    Pru cpu = new Pru();
    CodeEmitter ce = new CodeEmitter();
    abz.accept(ce);
    cpu.setCode(ce.visitEnd());

    VirtualAbEncoder encoder = new VirtualAbEncoder();

    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < 10000000; i++) {
      encoder.step(random.nextBoolean());

      abz.setA(cpu, encoder.getA() ? 1 : 0);
      abz.setB(cpu, encoder.getB() ? 1 : 0);
      executeBlock(cpu);
      assertEquals(abz.getCounter(cpu), encoder.getCount() & 0xffff, "counter");
      assertEquals(abz.getPosition(cpu), encoder.getPosition() & 0xffff, "position");
    }
    System.out.println("cpu.getCycleCountNonReset() = " + cpu.getCycleCountNonReset());
  }

}
