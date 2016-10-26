package com.github.vlsi.pru;

import org.testng.annotations.Test;
import st61131.pru.PRU_MM_CodeGenerator;

import java.io.IOException;

public class MemoryModelTest {
  @Test
  public void test() throws IllegalAccessException, IOException, InstantiationException {
    CodeSaver.save(PRU_MM_CodeGenerator.class, "memory.bin");
  }
}
