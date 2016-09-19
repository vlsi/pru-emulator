package com.github.vlsi.pru.plc110.debug;

import com.github.vlsi.pru.plc110.Label;
import com.github.vlsi.pru.plc110.Register;

public class RegisterVariableLocation {
  public final String name;
  public final String typeName;
  public final Label start;
  public final Label end;
  public final Register register;

  public RegisterVariableLocation(String name, String typeName, Label start, Label end,
                                  Register register) {
    this.name = name;
    this.typeName = typeName;
    this.start = start;
    this.end = end;
    this.register = register;
  }
}
