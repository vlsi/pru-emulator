package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.HaltInstruction;
import org.testng.annotations.Test;
import st61131.pru.PRU_BLINK1_CodeGenerator;
import st61131.pru.PRU_BLINK2_CodeGenerator;
import st61131.pru.PRU_BLINK3_CodeGenerator;
import st61131.pru.PRU_BLINK4_CodeGenerator;

import java.io.IOException;

public class BlinkTest {

  @Test
  public void blink() throws IllegalAccessException, IOException, InstantiationException {
    CodeSaver.save(PRU_BLINK1_CodeGenerator.class, "blink_1.bin");
    CodeSaver.save(PRU_BLINK2_CodeGenerator.class, "blink_2.bin");
    CodeSaver.save(PRU_BLINK3_CodeGenerator.class, "blink_3.bin");
    CodeSaver.save(PRU_BLINK4_CodeGenerator.class, "blink_4.bin");
  }

  @Test
  public void halt() throws IOException {
    CodeEmitter ce = new CodeEmitter();
    ce.visitInstruction(new HaltInstruction());
    CodeSaver.save(ce.visitEnd(), "halt.bin");
  }

}
