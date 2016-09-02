package com.github.vlsi.pru.plc110;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Decoder {
  public List<Instruction> decode(ByteBuffer bb) {
    List<Instruction> res = new ArrayList<>();
    while (bb.hasRemaining()) {
      int code = bb.getInt();
      Instruction ins = null;
      try {
        ins = Instruction.of(code);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Instruction " + (bb.position() - 4) / 4 + " is invalid: " + e.getMessage(), e);
      }
      res.add(ins);
    }
    return res;
  }
}
