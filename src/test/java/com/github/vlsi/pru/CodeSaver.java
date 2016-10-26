package com.github.vlsi.pru;

import com.github.vlsi.pru.plc110.BinaryCode;
import com.github.vlsi.pru.plc110.CodeEmitter;
import com.github.vlsi.pru.plc110.Instruction;
import com.github.vlsi.pru.plc110.LdiInstruction;
import org.testng.Assert;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.function.Consumer;

public class CodeSaver {

  public static <T extends Consumer<CodeEmitter>> void save(Class<T> producer, String fileName)
      throws IllegalAccessException, InstantiationException, IOException {
    CodeEmitter ce = new CodeEmitter();
    T t = producer.newInstance();
    t.accept(ce);
    save(ce.visitEnd(), fileName);
  }

  public static void save(BinaryCode code, String fileName) throws IOException {
    List<Instruction> instructions = code.getInstructions();
    ByteBuffer bb = ByteBuffer.allocate(instructions.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (Instruction ins : instructions) {
      bb.putInt(ins.code);
      Instruction dc = Instruction.of(ins.code);
      ins.setComment(null);
      Assert.assertEquals(dc.toString(), ins.toString());
    }
    bb.flip();
    saveToFile(fileName, bb);

    bb.flip();
    for (Instruction ins : instructions) {
      if (ins instanceof LdiInstruction) {
        LdiInstruction ld = (LdiInstruction) ins;
        if ((ld.value & 0xffff) == 0x700c) {
          ins = new LdiInstruction(ld.dstRegister, (short) 0x780c);
        }
      }
      bb.putInt(ins.code);
      Instruction dc = Instruction.of(ins.code);
      ins.setComment(null);
      Assert.assertEquals(dc.toString(), ins.toString());
    }
    bb.flip();
    saveToFile(fileName.replace(".bin", "1.bin"), bb);


  }

  private static void saveToFile(String fileName, ByteBuffer bb) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(fileName)) {
      FileChannel channel = fos.getChannel();
      channel.write(bb);
      channel.close();
    }
  }
}
