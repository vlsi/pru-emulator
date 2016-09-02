package com.github.vlsi.pru.plc110;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
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
    throw new IllegalStateException("Unsupported instruction " + ins);
  }

  private void execArithmetic(ArithmeticInstruction ins) {
    int src = getReg(ins.srcRegister);
    int op2 = ins.op2;
    if (ins.op2IsRegister) {
      op2 = getReg(op2);
    }
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

  public void setInstructions(List<Instruction> instructions) {
    instructionStream.addAll(instructions);
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

}
