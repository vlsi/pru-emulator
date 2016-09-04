package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.Register;
import com.github.vlsi.pru.plc110.RegisterField;

public class CommonRegisters {
  public final static Register R0 = new Register(0, RegisterField.dw);

  public final static Register R1_b0 = new Register(1, RegisterField.b0);
  public final static Register R1_b1 = new Register(1, RegisterField.b1);
  public final static Register R1_b2 = new Register(1, RegisterField.b2);
  public final static Register R1_b3 = new Register(1, RegisterField.b3);

  public final static Register R1_w0 = new Register(1, RegisterField.w0);
  public final static Register R1_w1 = new Register(1, RegisterField.w1);
  public final static Register R1_w2 = new Register(1, RegisterField.w2);

  public final static Register R1 = new Register(1, RegisterField.dw);
}
