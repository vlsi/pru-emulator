package com.github.vlsi.pru.plc110;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Pru {
  private final static int PRU0_CONTROL_REGISTERS = 0x7000;

  private final ByteBuffer ram =
      ByteBuffer.allocate(0x7bff)
          .order(ByteOrder.LITTLE_ENDIAN);

  private final static int TOTAL_REGISTERS = 32;

  // Byte order is b0, b1, b2, b3, b0, ...
  private final ByteBuffer registers =
      ByteBuffer.allocate(TOTAL_REGISTERS * 4)
          .order(ByteOrder.LITTLE_ENDIAN);

  private final ByteBuffer registersView = registers.duplicate().order(ByteOrder.LITTLE_ENDIAN);
  private final ByteBuffer ramView = ram.duplicate().order(ByteOrder.LITTLE_ENDIAN);

  private final ByteBuffer cycleCountReg
      = ((ByteBuffer) ram.duplicate()
      .position(PRU0_CONTROL_REGISTERS + 0xC)
      .limit(PRU0_CONTROL_REGISTERS + 0xC + 4))
      .slice().order(ByteOrder.LITTLE_ENDIAN);

  private List<Instruction> instructionStream = new ArrayList<>();

  private int carry;
  private int pc;

  private int cycleCount;
  private int memoryTransferCyclesLeft;

  public void tick() {
    Instruction ins = instructionStream.get(pc);
    cycleCount++;
    if (ins instanceof ArithmeticInstruction) {
      execArithmetic((ArithmeticInstruction) ins);
      pc++;
      return;
    }
    if (ins instanceof LdiInstruction) {
      LdiInstruction ldi = (LdiInstruction) ins;
      setReg(ldi.dstRegister, ldi.value);
      pc++;
      return;
    }
    if (ins instanceof JumpInstruction) {
      JumpInstruction jmp = (JumpInstruction) ins;
      if (jmp.op == Format2Instruction.Operation.JAL) {
        // Return address is stored for JAL only
        setReg(jmp.dstRegister, pc + 1);
      }
      pc = getOp2(jmp.op2, jmp.op2IsRegister);
    }
    if (ins instanceof LeftMostBitDetectInstruction) {
      execLeftMostBitDetect((LeftMostBitDetectInstruction) ins);
      pc++;
      return;
    }
    if (ins instanceof QuickBranchInstruction) {
      pc = execQuickBranch((QuickBranchInstruction) ins);
      return;
    }
    if (ins instanceof MemoryTransferInstruction) {
      if (memoryTransferCyclesLeft == 0) {
        prepareMemoryTransfer((MemoryTransferInstruction) ins);
        return;
      }
      memoryTransferCyclesLeft--;
      if (memoryTransferCyclesLeft == 0) {
        execMemoryTransfer((MemoryTransferInstruction) ins);
        pc++;
      }
      return;
    }
    throw new IllegalStateException("Unsupported instruction " + ins);
  }

  private void prepareMemoryTransfer(MemoryTransferInstruction ins) {
    int length;
    if (ins.lengthIsRegister()) {
      length = getReg(new Register(0, ins.getLengthField()));
    } else {
      length = ins.getLengthByte();
    }

    memoryTransferCyclesLeft = (length + 1) / 2;
  }

  private void execMemoryTransfer(MemoryTransferInstruction ins) {
    cycleCountReg.putInt(0, cycleCount);
    int address;

    if (ins.addressIsRegister()) {
      address = getReg(ins.getAddress());
    } else {
      byte addrConst = ins.getAddressEntry();
      switch (addrConst) {
        case 3: // PRU0/1 Local Data
          address = 0x00000000;
          break;
        default:
          throw new IllegalArgumentException(
              "Not implemented address entry " + addrConst + " for " + ins);
      }
    }

    int offset;
    if (ins.offsetIsRegister()) {
      offset = getReg(ins.getOffsetRegister());
    } else {
      offset = ins.getOffsetImm();
    }

    int length;
    if (ins.lengthIsRegister()) {
      length = getReg(new Register(0, ins.getLengthField()));
    } else {
      length = ins.getLengthByte();
    }

    try {
      int regOffs = getRegOffset(ins.srcDst);
      registersView.limit(regOffs + length);
      registersView.position(regOffs);

      int ramOffs = address + offset;
      ramView.limit(ramOffs + length);
      ramView.position(ramOffs);

      if (ins.op == MemoryTransferInstruction.Operation.LOAD) {
        registersView.put(ramView);
      } else {
        ramView.put(registersView);
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Invalid memory access at instruction " + ins + ", cpu state: " + printState(), e);
    }

    cycleCount = cycleCountReg.getInt(0);
  }

  private void execLeftMostBitDetect(LeftMostBitDetectInstruction ins) {
    LeftMostBitDetectInstruction lmbd = ins;
    int op2 = getOp2(lmbd.op2, lmbd.op2IsRegister);
    int src = getReg(lmbd.srcRegister);
    if ((op2 & 1) == 0) {
      int srcMask = lmbd.srcRegister.field().getBitMask();
      src ^= srcMask;
    }
    int res = 31 - Integer.numberOfLeadingZeros(src);
    setReg(lmbd.dstRegister, res < 0 ? 32 : res);
  }

  private int getOp2(int op2, boolean op2IsRegister) {
    if (op2IsRegister) {
      return getReg(Register.ofMask(op2));
    }
    return op2;
  }

  private void execArithmetic(ArithmeticInstruction ins) {
    int src = getReg(ins.srcRegister);
    int op2 = getOp2(ins.op2, ins.op2IsRegister);
    int res = 0;
    int resMask = ins.dstRegister.field().getBitMask();
    switch (ins.operation) {
      case ADD: {
        res = (src + op2) & resMask;
        // When res is less than op2, then overflow happened
        carry = Integer.compareUnsigned(res, op2) >>> 31;
        break;
      }
      case ADC: {
        res = (src + op2 + carry) & resMask;
        // When res is less than op2, or res is equal to op2 and was carry
        // then overflow happened
        carry = (Integer.compareUnsigned(res, op2) - carry) >>> 31;
        break;
      }
      case SUB: {
        long resLong = Integer.toUnsignedLong(src) - op2;
        res = (int) (resLong & resMask);
        carry = resLong < 0 ? 1 : 0;
        break;
      }
      case SUC: {
        long resLong = Integer.toUnsignedLong(src) - op2 - carry;
        res = (int) (resLong & resMask);
        carry = resLong < 0 ? 1 : 0;
        break;
      }
      case LSL:
        res = src << (op2 & 0x1f);
        break;
      case LSR:
        res = src >> (op2 & 0x1f); // TODO: >> or >>> ?
        break;
      case RSB: {
        long resLong = op2 - Integer.toUnsignedLong(src);
        res = (int) (resLong & resMask);
        carry = resLong < 0 ? 1 : 0;
        break;
      }
      case RSC: {
        long resLong = op2 - Integer.toUnsignedLong(src) - carry;
        res = (int) (resLong & resMask);
        carry = resLong < 0 ? 1 : 0;
        break;
      }
      case AND:
        res = src & op2;
        break;
      case OR:
        res = src | op2;
        break;
      case XOR:
        res = src ^ op2;
        break;
      case NOT:
        res = ~src;
        break;
      case MIN:
        res = Math.min(src, op2);
        break;
      case MAX:
        res = Math.max(src, op2);
        break;
      case CLR:
        res = src & ~(1 << (op2 & 0x1f));
        break;
      case SET:
        // NOTE: Whenever R31 is selected as the source operand to a SET, the resulting
        // source bits will be NULL, and not reflect the current input event flags that
        // are normally obtained by reading R31
        if (ins.srcRegister.index() == 31) {
          src = 0;
        }
        res = src | (1 << (op2 & 0x1f));
        break;
      default:
        throw new UnsupportedOperationException("Instruction " + ins + " is not implemented");
    }
    setReg(ins.dstRegister, res);
  }

  private int execQuickBranch(QuickBranchInstruction ins) {
    int op1 = getReg(ins.srcRegister);
    int op2 = getOp2(ins.op2, ins.op2IsRegister);
    if (ins.isBitTest()) {
      int bit = (1 << (op2 & 31));
      int bitValue = op1 & bit;
      if (!(ins.operation == QuickBranchInstruction.Operation.BC && bitValue == 0
          || ins.operation == QuickBranchInstruction.Operation.BS && bitValue != 0)) {
        return pc + 1;
      }
    } else {
      QuickBranchInstruction.Operation op = ins.operation;

      if (!(op == QuickBranchInstruction.Operation.LT && op2 < op1
          || op == QuickBranchInstruction.Operation.EQ && op2 == op1
          || op == QuickBranchInstruction.Operation.LE && op2 <= op1
          || op == QuickBranchInstruction.Operation.GT && op2 > op1
          || op == QuickBranchInstruction.Operation.NE && op2 != op1
          || op == QuickBranchInstruction.Operation.GE && op2 >= op1
          || op == QuickBranchInstruction.Operation.A)) {
        return pc + 1;
      }
    }
    return pc + ins.offset;
  }

  public void setInstructions(List<Instruction> instructions) {
    instructionStream.clear();
    instructionStream.addAll(instructions);
  }

  public void setInstructions(Instruction... instructions) {
    setInstructions(Arrays.asList(instructions));
  }

  public void setPc(int pc) {
    this.pc = pc;
  }

  public int getPc() {
    return pc;
  }

  public int getCycleCount() {
    return cycleCount;
  }

  public ByteBuffer ram() {
    return ram.duplicate().order(ByteOrder.LITTLE_ENDIAN);
  }

  public boolean getCarry() {
    return carry != 0;
  }

  public void setReg(Register reg, int value) {
    int offset = reg.index() * 4;
    RegisterField field = reg.field();
    int bits = field.getBitWidth();
    if (bits == 8) {
      registers.put(offset + field.toMask(), (byte) (value & 0xff));
      return;
    }
    if (bits == 16) {
      registers.putShort(offset + field.toMask() - 4, (short) (value & 0xffff));
      return;
    }
    registers.putInt(offset, value);
  }

  public int getReg(Register reg) {
    int offset = reg.index() * 4;
    RegisterField field = reg.field();
    int bits = field.getBitWidth();
    if (bits == 8) {
      return registers.get(offset + field.toMask()) & 0xff;
    }
    if (bits == 16) {
      return registers.getShort(offset + field.toMask() - 4) & 0xffff;
    }
    return registers.getInt(offset);
  }

  private int getRegOffset(Register reg) {
    int offset = reg.index() * 4;
    RegisterField field = reg.field();
    int bits = field.getBitWidth();
    if (bits == 8) {
      return offset + field.toMask();
    }
    if (bits == 16) {
      return offset + field.toMask() - 4;
    }
    return offset;
  }

  public String printState() {
    StringBuilder sb = new StringBuilder();
    sb.append("pc: ").append(pc).append('\n');
    sb.append("carry: ").append(carry).append('\n');

    sb.append("Instructions around pc\n");
    for (int i = Math.max(0, pc - 10); i < Math.min(instructionStream.size(), pc + 10); i++) {
      sb.append(i).append(": ").append(instructionStream.get(i));
      if (i == pc) {
        sb.append(" // <-- PC");
      }
      sb.append('\n');
    }

    for (int i = 0; i < 31; i++) {
      int reg = getReg(new Register(i, RegisterField.dw));
      if (i > 5 && reg == 0) {
        continue;
      }
      sb.append("R").append(i).append(": ");
      String hex = Integer.toUnsignedString(reg, 16);
      sb.append("0x");
      for (int j = hex.length(); j < 8; j++) {
        sb.append('0');
      }
      sb.append(hex);
      sb.append(" ");
      appendLpad(sb, reg, 10);
      sb.append(", w2: ");
      appendLpad(sb, (reg >> 16) & 0xffff, 5);
      sb.append(", w1: ");
      appendLpad(sb, (reg >> 8) & 0xffff, 5);
      sb.append(", w0: ");
      appendLpad(sb, reg & 0xffff, 5);
      sb.append(", b3: ");
      appendLpad(sb, (reg >>> 24) & 0xff, 3);
      sb.append(", b2: ");
      appendLpad(sb, (reg >>> 16) & 0xff, 3);
      sb.append(", b1: ");
      appendLpad(sb, (reg >>> 8) & 0xff, 3);
      sb.append(", b0: ");
      appendLpad(sb, reg & 0xff, 3);
      sb.append('\n');
    }
    return sb.toString();
  }

  private static void appendLpad(StringBuilder sb, int num, int width) {
    String str = Integer.toString(num);
    for (int i = str.length(); i < width; i++) {
      sb.append(' ');
    }
    sb.append(str);
  }
}
