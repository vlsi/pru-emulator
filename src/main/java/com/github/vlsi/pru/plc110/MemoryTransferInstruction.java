package com.github.vlsi.pru.plc110;

public class MemoryTransferInstruction extends Instruction {
  public enum Operation {
    LOAD,
    STORE
  }

  public final Operation op;
  public final Register srcDst;

  private boolean addrIsRegister;
  private byte addr;

  private boolean offsetIsRegister;
  private byte offset;

  private byte length;

  public MemoryTransferInstruction(int code) {
    super(code);
    int format = code >>> 29;
    assert format == 0b111 || format == 0b100 :
        "Format should be 0b111 or 0b100. Given " +
            Integer.toBinaryString(format) + ", ins: " + Integer.toHexString(code);
    this.op = (code & (1 << 28)) == 0 ? Operation.STORE : Operation.LOAD;
    this.addrIsRegister = format == 0b111;
    this.length =
        (byte) (((code >>> (25 - 4)) & 0b1110000)
            | ((code >>> (13 - 1)) & 0b0001110)
            | ((code >>> (7 - 0)) & 0b0000001));
    this.offsetIsRegister = (code & (1 << 24)) == 0;
    this.offset = (byte) ((code >>> 16) & 0xff);
    this.addr = (byte) ((code >> 8) & 31);
    this.srcDst = Register.ofMask(code & ((1 << 7) - 1));
  }

  public MemoryTransferInstruction(
      Operation op,
      Register srcDst) {
    super(0);
    this.op = op;
    if (srcDst.field().getBitWidth() != 8) {
      srcDst = new Register(srcDst.index(), RegisterField.b(srcDst.field().byteOffset()));
    }
    this.srcDst = srcDst;
  }

  public MemoryTransferInstruction encode() {
    code = ((addrIsRegister ? 0b111 : 0b100) << 29)
        | (op == Operation.LOAD ? 1 << 28 : 0)
        | ((length & 0b1110000) << (25 - 4))
        | ((length & 0b0001110) << (13 - 1))
        | ((length & 0b0000001) << (7 - 0))
        | (!offsetIsRegister ? 1 << 24 : 0) // IO
        | ((offset & 0xff) << 16) // Ro
        | ((addr & 31) << 8)
        | (srcDst.field().byteOffset() << 5)
        | srcDst.index();
    return this;
  }

  public MemoryTransferInstruction setAddress(int addr) {
    assert addr >= 0 && addr <= 31 : "LBCO/SBCO constant entry should be in 0..31 range. Given " + addr;
    this.addrIsRegister = false;
    this.addr = (byte) addr;
    return this;
  }

  public MemoryTransferInstruction setAddress(Register addr) {
    assert addr.index() >= 0 && addr.index() <= 31
        && addr.field() == RegisterField.dw : "LBBO/SBBO address register should be 31 bit one. Given " + addr;
    this.addrIsRegister = true;
    this.addr = (byte) addr.index();
    return this;
  }

  public boolean addressIsRegister() {
    return addrIsRegister;
  }

  public byte getAddressEntry() {
    assert !this.addrIsRegister : "Address entry should be asked for LBCO/SBCO instruction only. Current is " + this;
    return addr;
  }

  public Register getAddress() {
    assert this.addrIsRegister : "Address register should be asked for LBBO/SBBO instruction only. Current is " + this;
    return new Register(addr, RegisterField.dw);
  }

  public MemoryTransferInstruction setOffset(int offset) {
    this.offsetIsRegister = false;
    this.offset = (byte) offset;
    return this;
  }

  public MemoryTransferInstruction setOffset(Register offset) {
    this.offsetIsRegister = true;
    this.offset = (byte) offset.mask();
    return this;
  }

  public boolean offsetIsRegister() {
    return offsetIsRegister;
  }

  public int getOffsetImm() {
    return offset & 0xff;
  }

  public Register getOffsetRegister() {
    return Register.ofMask(offset);
  }

  public MemoryTransferInstruction setLength(int length) {
    assert length >= 1 && length <= 124 : "LB/SB immediate offset should be in 1..124 range. Given " + length;
    this.length = (byte) (length - 1);
    return this;
  }

  public MemoryTransferInstruction setLength(RegisterField offset) {
    assert offset.getBitWidth() == 8 : "LB/SB register offset should be 1 byte register of R0. Given " + offset;
    this.length = (byte) (124 + offset.toMask());
    return this;
  }

  public boolean lengthIsRegister() {
    return length > 123;
  }

  public int getLengthByte() {
    assert length < 124 : "Byte accessor should be used for 1..124 burst length only. Actual length is " + (length + 1);
    return (length & 0xff) + 1;
  }

  public RegisterField getLengthField() {
    assert length >= 124 : "Register accessor should be used for R0-coded burst length only. Actual length code is " + length;
    return RegisterField.ofMask(length - 124);
  }

  @Override
  public String toString() {
    return (op == Operation.LOAD ? "LB" : "SB") +
        (addrIsRegister ? "BO" : "CO") +
        " " +
        srcDst +
        ", " + (addrIsRegister ? Register.ofMask(addr).toString() : ("C" + addr)) +
        ", " + (offsetIsRegister ? Register.ofMask(offset & 0xff).toString() : offset & 0xff) +
        ", " + (lengthIsRegister() ? getLengthField() : getLengthByte()) +
        commentToString();
  }
}
