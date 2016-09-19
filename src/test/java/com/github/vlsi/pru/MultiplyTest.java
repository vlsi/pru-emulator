package com.github.vlsi.pru;

import org.testng.Assert;
import org.testng.annotations.Test;

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
        Assert.assertEquals(div(i, j), i / j, i + " / " + j);
      }
    }
  }
}
