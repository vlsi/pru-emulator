package com.github.vlsi.pru.plc110;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Pru {
  private final static int TOTAL_REGISTERS = 32;

  // Byte order is b0, b1, b2, b3, b0, ...
  private final ByteBuffer registers =
      ByteBuffer.allocate(TOTAL_REGISTERS * 4)
          .order(ByteOrder.LITTLE_ENDIAN);

  private final ByteBuffer ram =
      ByteBuffer.allocate(1024)
          .order(ByteOrder.LITTLE_ENDIAN);

  private List<Instruction> instructionStream = new ArrayList<>();

  private int carry;
  private int pc;

  public void tick() {
    Instruction ins = instructionStream.get(pc);
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
    throw new IllegalStateException("Unsupported instruction " + ins);
  }

  private void execLeftMostBitDetect(LeftMostBitDetectInstruction ins) {
    LeftMostBitDetectInstruction lmbd = ins;
    int op2 = getOp2(lmbd.op2, lmbd.op2IsRegister);
    int src = getReg(lmbd.srcRegister);
    if ((op2 & 1) == 0) {
      int srcMask = RegisterField.ofMask(lmbd.srcRegister >> 5).getBitMask();
      src ^= srcMask;
    }
    int res = 31 - Integer.numberOfLeadingZeros(src);
    setReg(lmbd.dstRegister, res < 0 ? 32 : res);
  }

  private int getOp2(int op2, boolean op2IsRegister) {
    if (op2IsRegister) {
      return getReg(op2);
    }
    return op2;
  }

  private void execArithmetic(ArithmeticInstruction ins) {
    int src = getReg(ins.srcRegister);
    int op2 = getOp2(ins.op2, ins.op2IsRegister);
    int res = 0;
    int resMask = ins.dstField.getBitMask();
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
        if ((ins.srcRegister & 31) == 31) {
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

  public boolean getCarry() {
    return carry != 0;
  }

  public Register getRegister(int index) {
    if (index < 0 || index > TOTAL_REGISTERS) {
      throw new IllegalArgumentException("Register index should be in range 0.."
          + TOTAL_REGISTERS + ", actual value is " + index);
    }
    return new Register(registers, index);
  }

  public int getReg(int register) {
    return getRegister(register & 31, register >> 5);
  }

  public void setReg(int register, int value) {
    setRegister(register & 31, register >> 5, value);
  }

  public int getReg(int register, RegisterField field) {
    return getRegister(register & 31, field.toMask());
  }

  public void setReg(int register, RegisterField field, int value) {
    setRegister(register & 31, field.toMask(), value);
  }

  private int getRegister(int register, int mask) {
    int offset = register * 4;
    if (mask < 4) {
      return registers.get(offset + mask) & 0xff;
    }
    if (mask < 7) {
      return registers.getShort(offset + mask - 4) & 0xffff;
    }
    return registers.getInt(offset);
  }

  private void setRegister(int register, int mask, int value) {
    int offset = register * 4;
    if (mask < 4) {
      registers.put(offset + mask, (byte) (value & 0xff));
      return;
    }
    if (mask < 7) {
      registers.putShort(offset + mask - 4, (short) (value & 0xffff));
      return;
    }
    registers.putInt(offset, value);
  }

  public String printState() {
    StringBuffer sb = new StringBuffer();
    sb.append("pc: ").append(pc).append('\n');
    sb.append("carry: ").append(carry).append('\n');

    sb.append("Instructions around pc\n");
    for (int i = Math.max(0, pc - 5); i < Math.min(instructionStream.size(), pc + 5); i++) {
      sb.append(i).append(": ").append(instructionStream.get(i));
      if (i == pc) {
        sb.append(" // <-- PC");
      }
      sb.append('\n');
    }

    for (int i = 0; i < 31; i++) {
      int reg = getReg(RegisterField.dw.fullMask(i));
      sb.append("R").append(i).append(": ");
      String hex = Integer.toUnsignedString(reg, 16);
      sb.append("0x");
      for (int j = hex.length(); j < 8; j++) {
        sb.append('0');
      }
      sb.append(hex);
      sb.append(" ").append(Integer.toString(reg));
      sb.append(", s: ").append(Short.toString((short) (reg >> 16)));
      sb.append(" ").append(Short.toString((short) (reg & 0xffff)));
      sb.append(", b: ").append(Byte.toString((byte) (reg >> 24)));
      sb.append(" ").append(Byte.toString((byte) (reg >> 16)));
      sb.append(" ").append(Byte.toString((byte) (reg >> 8)));
      sb.append(" ").append(Byte.toString((byte) (reg & 0xff)));
      sb.append('\n');
    }
    return sb.toString();
  }
}
