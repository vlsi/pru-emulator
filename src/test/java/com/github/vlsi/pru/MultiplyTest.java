package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.ArithmeticInstruction;
import com.github.vlsi.pru.plc110.BinaryCode;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Instruction;
import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.LdiInstruction;
import com.github.vlsi.pru.plc110.LeftMostBitDetectInstruction;
import com.github.vlsi.pru.plc110.Pru;
import com.github.vlsi.pru.plc110.QuickBranchInstruction;
import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class MultiplyTest {
  private static int lmbd(int x) {
    int res = 31 - Integer.numberOfLeadingZeros(x);
    return res < 0 ? 32 : res;
  }

  private int mul(int a, int b) {
    // 5 * 32 == 150 ticks
    int x = Math.min(a, b);
    int y = Math.max(a, b);
    int r = 0;
    for (; x != 0; ) {
      if ((x & 1) == 1) {
        r += y;
      }
      x >>>= 1;
      y <<= 1;
    }
//    for (; ; ) {
//      int bit = lmbd(x); // 1
//      if (bit == 32) { // 2
//        break;
//      }
//      int tmp = 1<<bit;
//      x -= tmp;
//      x = x & ~(1 << bit); // 3
//      r += y << bit; // 4, 5
//    }
    return r;
  }

  public int div(int a, int b) {
    int r = 0;
    if (a == 0) {
      return 0;
    }
    int bitA = lmbd(a);
    int bitB = lmbd(b);

    if (bitB > bitA) {
      return 0;
    }

    int mask = 1;
    if (bitB < bitA) {
      b <<= bitA - bitB;
      mask <<= bitA - bitB;
    }

    while (mask != 0) { // 1
      if (a >= b) { // 2
        r += mask; // 3
        a -= b; // 4
      }
      mask >>>= 1; // 5
      b >>>= 1; // 6
    }

    return r;
  }


  @Test
  public void testMul() {
    for (int i = -20; i < 20; i++) {
      for (int j = -20; j < 20; j++) {
        Assert.assertEquals(mul(i, j), (int) (i * j), i + " * " + j);
      }
    }
  }

  @Test
  public void testDiv() {
    for (int i = 0; i < 200; i++) {
      for (int j = 1; j < 400; j++) {
        i = Integer.MIN_VALUE | Integer.MAX_VALUE;
        j = 1;
        Assert.assertEquals(div(i, j), i / j, i + " / " + j);
        return;
      }
    }
  }

  @Test
  public void divmod() {
    CodeEmitter ce = new CodeEmitter();
    appendCode(ce);
    Pru cpu = new Pru();
    cpu.setCode(ce.visitEnd());
//    cpu.setInstructions(ce.visitEnd().getInstructions());

    cpu.setReg(new Register(2, RegisterField.dw), 16);
    cpu.setReg(new Register(3, RegisterField.dw), 2);
    int i = 0;
    try {
      for (; i < 1000; i++) {
        cpu.tick();
      }
    } catch (Exception e) {
      System.out.println("Exception at step " + i + ", " + cpu.printState());
    }
    System.out.println("div = " + cpu.getReg(new Register(1, RegisterField.dw)));
    System.out.println("mod = " + cpu.getReg(new Register(2, RegisterField.dw)));
  }

  @Test
  public void sqrtTest() {
    CodeEmitter ce = new CodeEmitter();
    sqrtCode(ce);
    int[] cnt = new int[1024];
    Pru cpu = new Pru() {
      @Override
      public void tick() {
        cnt[this.getPc()]++;
        super.tick();
      }
    };
    BinaryCode code = ce.visitEnd();
    cpu.setCode(code);

    cpu.setReg(new Register(1, RegisterField.dw), (int) 4294967294L);
    int time = cpu.runTillHalt(1000);
    System.out.println("time = " + time + ", " + cpu.getReg(new Register(2, RegisterField.dw)));
    List<Instruction> instructions = code.getInstructions();
    for (int i = 0; i < instructions.size(); i++) {
      System.out.println(cnt[i] + "   " + instructions.get(i));
    }
  }

  @Test
  public void nextDelay() {
    CodeEmitter ce = new CodeEmitter();
    nextDelay(ce);
    Pru cpu = new Pru();
    cpu.setCode(ce.visitEnd());
//    cpu.setInstructions(ce.visitEnd().getInstructions());

//    cpu.setReg(new Register(2, RegisterField.dw), Integer.MAX_VALUE | Integer.MIN_VALUE);
//    cpu.setReg(new Register(3, RegisterField.dw), 1014000);
//    cpu.setReg(new Register(4, RegisterField.dw), 1);
    cpu.setReg(new Register(3, RegisterField.dw), 473200);
    cpu.setReg(new Register(4, RegisterField.dw), 3);
    int i = 0;
    try {
      for (; i < 1000; i++) {
        cpu.tick();
      }
    } catch (Exception e) {
      System.out.println("Exception at step " + i + ", " + cpu.printState());
    }
//    System.out.println("div = " + cpu.getReg(new Register(1, RegisterField.dw)));
    System.out.println("next_delay = " + cpu.getReg(new Register(2, RegisterField.dw)));
  }


  public void appendCode(CodeEmitter ce) {
    Label if1 = new Label("if1");
    Label endIf0 = new Label("endIf0");
    Label startWhileBody2 = new Label("startWhileBody2");
    Label endIf4 = new Label("endIf4");
    Label endWhile3 = new Label("endWhile3");
    ce.visitInstruction(new LeftMostBitDetectInstruction(new Register(4, RegisterField.b0),
        new Register(2, RegisterField.dw), (byte) 1).setComment("bitX => R4.b0, x => R2"));
    ce.visitInstruction(new LeftMostBitDetectInstruction(new Register(4, RegisterField.b1),
        new Register(3, RegisterField.dw), (byte) 1).setComment("bitY => R4.b1, y => R3"));

    ce.visitInstruction(
        new LdiInstruction(new Register(5, RegisterField.dw), (short) 1).setComment("mask => R5"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.GT, if1,
        new Register(4, RegisterField.b1), new Register(4, RegisterField.b0)).setComment(
        "bitY => R4.b1, bitX => R4.b0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(6, RegisterField.dw), new Register(3, RegisterField.dw), 0).setComment(
        "b => R6, y => R3"));

    ce.visitInstruction(new QuickBranchInstruction(endIf0).setComment(""));
    ce.visitLabel(if1);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(4, RegisterField.b0), new Register(4, RegisterField.b0),
        new Register(4, RegisterField.b1)).setComment(
        "diff => R4.b0, bitX => R4.b0, bitY => R4.b1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(6, RegisterField.dw), new Register(3, RegisterField.dw),
        new Register(4, RegisterField.b0)).setComment("b => R6, y => R3, diff => R4.b0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(5, RegisterField.dw), new Register(5, RegisterField.dw),
        new Register(4, RegisterField.b0)).setComment("mask => R5, diff => R4.b0"));

    ce.visitLabel(endIf0);

    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endWhile3,
        new Register(5, RegisterField.dw), 0).setComment("mask => R5"));
    ce.visitLabel(startWhileBody2);
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.GT, endIf4,
        new Register(2, RegisterField.dw), new Register(6, RegisterField.dw)).setComment(
        "a => R2, b => R6"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(1, RegisterField.dw), new Register(1, RegisterField.dw),
        new Register(5, RegisterField.dw)).setComment("div => R1, mask => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(2, RegisterField.dw), new Register(2, RegisterField.dw),
        new Register(6, RegisterField.dw)).setComment("a => R2, b => R6"));

    ce.visitLabel(endIf4);

    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSR,
        new Register(5, RegisterField.dw), new Register(5, RegisterField.dw), 1).setComment(
        "mask => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSR,
        new Register(6, RegisterField.dw), new Register(6, RegisterField.dw), 1).setComment(
        "b => R6"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, startWhileBody2,
            new Register(5, RegisterField.dw), 0).setComment("mask => R5"));
    ce.visitLabel(endWhile3);
  }


  public void nextDelay(CodeEmitter ce) {
    Label if1 = new Label("if1");
    Label endIf0 = new Label("endIf0");
    Label startWhileBody2 = new Label("startWhileBody2");
    Label endIf4 = new Label("endIf4");
    Label endWhile3 = new Label("endWhile3");
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(5, RegisterField.dw), new Register(3, RegisterField.dw), 1).setComment(
        "step_delay2x => R5, step_delay => R3"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(5, RegisterField.dw), new Register(5, RegisterField.dw),
        new Register(1, RegisterField.dw)).setComment("step_delay2x => R5, rest => R1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(4, RegisterField.dw), new Register(4, RegisterField.dw), 2).setComment(
        "accel_count4x => R4, accel_count => R4"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.dw), new Register(4, RegisterField.dw), 1).setComment(
        "accel_count4x => R4"));
    // Call PRU_DIVMOD
    ce.visitInstruction(new LeftMostBitDetectInstruction(new Register(6, RegisterField.b0),
        new Register(5, RegisterField.dw), (byte) 1).setComment(
        "tmpPRU_DIVMOD_0_bitX => R6.b0, tmpPRU_DIVMOD_0_x => R5"));
    ce.visitInstruction(new LeftMostBitDetectInstruction(new Register(6, RegisterField.b1),
        new Register(4, RegisterField.dw), (byte) 1).setComment(
        "tmpPRU_DIVMOD_0_bitY => R6.b1, tmpPRU_DIVMOD_0_y => R4"));

    ce.visitInstruction(new LdiInstruction(new Register(7, RegisterField.dw), (short) 1).setComment(
        "tmpPRU_DIVMOD_0_mask => R7"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.GT, if1,
        new Register(6, RegisterField.b1), new Register(6, RegisterField.b0)).setComment(
        "tmpPRU_DIVMOD_0_bitY => R6.b1, tmpPRU_DIVMOD_0_bitX => R6.b0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(8, RegisterField.dw), new Register(4, RegisterField.dw), 0).setComment(
        "tmpPRU_DIVMOD_0_b => R8, tmpPRU_DIVMOD_0_y => R4"));

    ce.visitInstruction(new QuickBranchInstruction(endIf0).setComment(""));
    ce.visitLabel(if1);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(6, RegisterField.b0), new Register(6, RegisterField.b0),
        new Register(6, RegisterField.b1)).setComment(
        "tmpPRU_DIVMOD_0_diff => R6.b0, tmpPRU_DIVMOD_0_bitX => R6.b0, tmpPRU_DIVMOD_0_bitY => R6.b1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(8, RegisterField.dw), new Register(4, RegisterField.dw),
        new Register(6, RegisterField.b0)).setComment(
        "tmpPRU_DIVMOD_0_b => R8, tmpPRU_DIVMOD_0_y => R4, tmpPRU_DIVMOD_0_diff => R6.b0"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSL,
        new Register(7, RegisterField.dw), new Register(7, RegisterField.dw),
        new Register(6, RegisterField.b0)).setComment(
        "tmpPRU_DIVMOD_0_mask => R7, tmpPRU_DIVMOD_0_diff => R6.b0"));

    ce.visitLabel(endIf0);

    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.dw), new Register(5, RegisterField.dw), 0).setComment(
        "tmpPRU_DIVMOD_0_a => R4, tmpPRU_DIVMOD_0_x => R5"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.EQ, endWhile3,
        new Register(7, RegisterField.dw), 0).setComment("tmpPRU_DIVMOD_0_mask => R7"));
    ce.visitLabel(startWhileBody2);
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.GT, endIf4,
        new Register(4, RegisterField.dw), new Register(8, RegisterField.dw)).setComment(
        "tmpPRU_DIVMOD_0_a => R4, tmpPRU_DIVMOD_0_b => R8"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(2, RegisterField.dw), new Register(2, RegisterField.dw),
        new Register(7, RegisterField.dw)).setComment(
        "tmpPRU_DIVMOD_0_div => R2, tmpPRU_DIVMOD_0_mask => R7"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(4, RegisterField.dw), new Register(4, RegisterField.dw),
        new Register(8, RegisterField.dw)).setComment(
        "tmpPRU_DIVMOD_0_a => R4, tmpPRU_DIVMOD_0_b => R8"));

    ce.visitLabel(endIf4);

    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSR,
        new Register(7, RegisterField.dw), new Register(7, RegisterField.dw), 1).setComment(
        "tmpPRU_DIVMOD_0_mask => R7"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSR,
        new Register(8, RegisterField.dw), new Register(8, RegisterField.dw), 1).setComment(
        "tmpPRU_DIVMOD_0_b => R8"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, startWhileBody2,
            new Register(7, RegisterField.dw), 0).setComment("tmpPRU_DIVMOD_0_mask => R7"));
    ce.visitLabel(endWhile3);

    // End PRU_DIVMOD
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(1, RegisterField.dw), new Register(4, RegisterField.dw), 0).setComment(
        "rest => R1, tmpPRU_DIVMOD_0_mod => R4"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(2, RegisterField.dw), new Register(3, RegisterField.dw),
        new Register(2, RegisterField.dw)).setComment("next_delay => R2, step_delay => R3"));
  }


  public void sqrtCode(CodeEmitter ce) {
    Label startWhileBody0 = new Label("startWhileBody0");
    Label endWhile1 = new Label("endWhile1");
    Label startRepeat2 = new Label("startRepeat2");
    Label endIf3 = new Label("endIf3");
    Label endIf4 = new Label("endIf4");
    ce.visitInstruction(
        new LdiInstruction(new Register(2, RegisterField.dw), (short) 0).setComment("xr => R2"));
    ce.visitInstruction(
        new LdiInstruction(new Register(3, RegisterField.w0), (short) 0).setComment("q2 => R3"));
    ce.visitInstruction(
        new LdiInstruction(new Register(3, RegisterField.w2), (short) 16384).setComment(""));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(4, RegisterField.dw), new Register(1, RegisterField.dw), 0).setComment(
        "x => R4, in_x => R1"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.GE, endWhile1,
        new Register(3, RegisterField.dw), new Register(4, RegisterField.dw)).setComment(
        "q2 => R3, x => R4"));
    ce.visitLabel(startWhileBody0);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSR,
        new Register(3, RegisterField.dw), new Register(3, RegisterField.dw), 2).setComment(
        "q2 => R3"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.LT, startWhileBody0,
            new Register(3, RegisterField.dw), new Register(4, RegisterField.dw)).setComment(
            "q2 => R3, x => R4"));
    ce.visitLabel(endWhile1);

    ce.visitLabel(startRepeat2);
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(5, RegisterField.dw), new Register(2, RegisterField.dw),
        new Register(3, RegisterField.dw)).setComment("sum => R5, xr => R2, q2 => R3"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSR,
        new Register(2, RegisterField.dw), new Register(2, RegisterField.dw), 1).setComment(
        "xr => R2"));
    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.GT, endIf3,
        new Register(4, RegisterField.dw), new Register(5, RegisterField.dw)).setComment(
        "x => R4, sum => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.SUB,
        new Register(4, RegisterField.dw), new Register(4, RegisterField.dw),
        new Register(5, RegisterField.dw)).setComment("x => R4, sum => R5"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(2, RegisterField.dw), new Register(2, RegisterField.dw),
        new Register(3, RegisterField.dw)).setComment("xr => R2, q2 => R3"));

    ce.visitLabel(endIf3);

    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.LSR,
        new Register(3, RegisterField.dw), new Register(3, RegisterField.dw), 2).setComment(
        "q2 => R3"));

    ce.visitInstruction(
        new QuickBranchInstruction(QuickBranchInstruction.Operation.NE, startRepeat2,
            new Register(3, RegisterField.dw), 0).setComment("q2 => R3"));

    ce.visitInstruction(new QuickBranchInstruction(QuickBranchInstruction.Operation.LE, endIf4,
        new Register(2, RegisterField.dw), new Register(1, RegisterField.dw)).setComment(
        "xr => R2, in_x => R1"));
    ce.visitInstruction(new ArithmeticInstruction(ArithmeticInstruction.Operation.ADD,
        new Register(2, RegisterField.dw), new Register(2, RegisterField.dw), 1).setComment(
        "xr => R2"));
    ce.visitLabel(endIf4);
  }


}
