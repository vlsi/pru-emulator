package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Pru;
import org.testng.Assert;
import org.testng.annotations.Test;
import st61131.pru.PRU_CUTTER_CodeGenerator;
import st61131.pru.PRU_MATERIAL_CUTTER_CodeGenerator;

import java.io.IOException;

public class CutterTest {
  @Test
  public void save() throws IllegalAccessException, IOException, InstantiationException {
    CodeSaver.save(PRU_MATERIAL_CUTTER_CodeGenerator.class, "material_cutter.bin");
  }

  @Test
  public void test() {
    PRU_CUTTER_CodeGenerator c = new PRU_CUTTER_CodeGenerator();

    Pru cpu = new Pru();
    CodeEmitter ce = new CodeEmitter();
    c.accept(ce);
    cpu.setCode(ce.visitEnd());

    int abzStart = 65530;
    int abz = abzStart;

    Assert.assertEquals(c.getState(cpu), 0, "state");

    executeBlock(cpu);
    Assert.assertEquals(c.getState(cpu), 0, "state");

    c.setRunLength(cpu, 20);
    c.setCntr(cpu, abz);
    executeBlock(cpu); // Указали только расстояние, но не запустили. Поехать не должно
    Assert.assertEquals(c.getState(cpu), 0, "state");
    Assert.assertEquals(c.getOut(cpu), 0, "out");
    Assert.assertEquals(c.getOffset(cpu), 0, "offset");

    c.setEnable(cpu, 1);
    executeBlock(cpu); // Вот теперь должно поехать. Но энкодер не сместился, поэтому offset=0
    Assert.assertEquals(c.getState(cpu), 1, "state");
    Assert.assertEquals(c.getOut(cpu), 1, "out");
    Assert.assertEquals(c.getOffset(cpu), 0, "offset");

    abz = (abz + 5) & 0xffff; // Делаем вид, что энкодер сместился на 5 импульсов
    c.setCntr(cpu, abz);
    executeBlock(cpu);
    Assert.assertEquals(c.getState(cpu), 1, "state");
    Assert.assertEquals(c.getOut(cpu), 1, "out");
    Assert.assertEquals(c.getOffset(cpu), 5, "offset");

    executeBlock(cpu);
    Assert.assertEquals(c.getState(cpu), 1, "state");
    Assert.assertEquals(c.getOut(cpu), 1, "out");
    Assert.assertEquals(c.getOffset(cpu), 5, "offset");

    abz = (abz + 14) & 0xffff; // Смещаем энкодер ещё на 14 импульсов, всего 19 = 5+14
    c.setCntr(cpu, abz);
    executeBlock(cpu);
    Assert.assertEquals(c.getState(cpu), 1, "state");
    Assert.assertEquals(c.getOut(cpu), 1, "out");
    Assert.assertEquals(c.getOffset(cpu), 19, "offset");

    abz = (abz + 1) & 0xffff; // Доследний импульс. Мотор должен выключиться.
    c.setCntr(cpu, abz);
    executeBlock(cpu);
    Assert.assertEquals(c.getState(cpu), 2, "state");
    Assert.assertEquals(c.getOut(cpu), 0, "out");
    Assert.assertEquals(c.getOffset(cpu), 20, "offset");

    executeBlock(cpu);
    Assert.assertEquals(c.getState(cpu), 2, "state");
    Assert.assertEquals(c.getOut(cpu), 0, "out");
    Assert.assertEquals(c.getOffset(cpu), 20, "offset");

    c.setEnable(cpu, 0);
    executeBlock(cpu);
    Assert.assertEquals(c.getState(cpu), 0, "state");
    Assert.assertEquals(c.getOut(cpu), 0, "out");
    Assert.assertEquals(c.getOffset(cpu), 0, "offset");
  }

  private void executeBlock(Pru cpu) {
    cpu.setPc(0);
    cpu.runTillHalt(100);
  }
}
