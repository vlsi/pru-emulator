package com.github.vlsi.pru;

import static com.github.vlsi.pru.VirtualAbEncoder.EncoderState.S01;
import static com.github.vlsi.pru.VirtualAbEncoder.EncoderState.S10;
import static com.github.vlsi.pru.VirtualAbEncoder.EncoderState.S11;

public class VirtualAbEncoder {
  private final static EncoderState[] ENCODER_STATES = EncoderState.values();

  enum EncoderState {
    S00,
    S01,
    S11,
    S10,
  }

  private EncoderState state = EncoderState.S00;
  private int count;
  private int position;

  public boolean getA() {
    return state == S10 || state == S11;
  }

  public boolean getB() {
    return state == S01 || state == S11;
  }


  public void reset() {
    state = EncoderState.S00;
    count = 0;
    position = 0;
  }

  public void left() {
    step(true);
  }

  public void right() {
    step(false);
  }

  public void step(boolean left) {
    count++;
    int diff = left ? 1 : -1;
    position += diff;
    state = getStateByOrdinal(state.ordinal() + diff);
  }

  private EncoderState getStateByOrdinal(int i) {
    int length = ENCODER_STATES.length;
    return ENCODER_STATES[(i + length) % length];
  }

  public int getPosition() {
    return position;
  }

  public int getCount() {
    return count;
  }
}
