package com.github.vlsi.pru.plc110;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Decoder {
  public List<Instruction> decode(ByteBuffer bb) {
    return decode(bb, false);
  }

  public List<Instruction> decode(ByteBuffer bb, boolean ignoreInvalid) {
    List<Instruction> res = new ArrayList<>();
    while (bb.hasRemaining()) {
      int code = bb.getInt();
      Instruction ins = null;
      try {
        ins = Instruction.of(code);
      } catch (IllegalArgumentException e) {
        if (ignoreInvalid) {
          ins = new IllegalInstruction(code);
        } else {
          throw new IllegalArgumentException(
              "Instruction " + (bb.position() - 4) / 4 + " is invalid: " + e.getMessage(), e);
        }
      }
      res.add(ins);
    }
    return res;
  }
}
