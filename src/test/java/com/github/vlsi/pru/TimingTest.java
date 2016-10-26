package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.BinaryCode;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Instruction;
import com.github.vlsi.pru.plc110.Pru;
import org.testng.annotations.Test;
import st61131.pru.PRU_TIMING_TEST_CodeGenerator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class TimingTest {

  @Test
  public void test() throws IllegalAccessException, IOException, InstantiationException {
    CodeSaver.save(PRU_TIMING_TEST_CodeGenerator.class, "pru_timing.bin");

    CodeEmitter ce = new CodeEmitter();
    new PRU_TIMING_TEST_CodeGenerator().accept(ce);
    BinaryCode binaryCode = ce.visitEnd();

    int[] profile = new int[1000];
    int[] firstPc = new int[1000];
    int[] lastPc = new int[1000];
    Pru cpu = new Pru() {
      @Override
      public void tick() {
        int pc = getPc();
        profile[pc]++;
        if (firstPc[pc] == 0) {
          firstPc[pc] = getCycleCount();
        }
        super.tick();
        lastPc[pc] = getCycleCount();
      }
    };
    cpu.setCode(binaryCode);

    for (int i = 0; i < 1; i++) {
      cpu.tick();
      cpu.setPc(0);
    }
    for (int i = 0; i < 1000; i++) {
      cpu.tick();
    }

    List<Instruction> instructions = binaryCode.getInstructions();
    for (int i = 0; i < instructions.size(); i++) {
      Instruction instruction = instructions.get(i);
      System.out.println(
          profile[i] + "  :  " + firstPc[i] + " .. " + lastPc[i] + "  " + instruction);
    }

    int[] etalon = new int[]{
        /*1*/0, 7, 11, 15, // 4 diff
        /*5*/29, 33, 37, 41, // 4 diff
        /*9*/53, 2, 2, 2,
        /*13*/14, 20, 26, 32,
        /*17*/38, 44, 50, 56};

    ByteBuffer ram = cpu.ram();
    for (int i = 2; i <= 80 / 4; i++) {
      int vi = ram.getInt(i * 4);
      int ei = etalon[i - 1];
      int dvi = vi - ram.getInt((i - 1) * 4);
      int dei = ei - etalon[i - 2];
      System.out.println(i + ": " + vi + " (" + dvi
          + ")" + " vs " + ei + " (" + dei + ")" + (dvi == dei ? "" : " FAIL"));
    }

//    for (int i = 1; i <= 80/4; i++) {
//      System.out.println("v" + i + " : DWORD;");
//    }


//    for (int i = 1; i <= 80/4; i++) {
//      System.out.println("PRU_FB_GetParameter(pru_num:=0, index:=" + i + ", value := ADR(v" + i + "));");
//    }
  }

}
