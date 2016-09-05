package com.github.vlsi.pru.plc110;

public interface Jump {
  Label getTarget();
  void resolveTarget(int sourceOffset);
}
