# pru-emulator

[![Build Status](https://travis-ci.org/vlsi/pru-emulator.svg?branch=master)](https://travis-ci.org/vlsi/pru-emulator)
[![codecov](https://codecov.io/gh/vlsi/pru-emulator/branch/master/graph/badge.svg)](https://codecov.io/gh/vlsi/pru-emulator)

This is a java emulator for TI PRU CPU

The aim of the project is to implement basic assembler and emulator for TI PRU CPU.

http://processors.wiki.ti.com/index.php/Programmable_Realtime_Unit_Subsystem

Requirements: Java 8

License
-------

The library is distributed under terms of MIT license.

TODO
----

- Interrupts
- GPIO (explicit API to set/get IO values)
- Control registers, DDR
- Dual-PRU emulation (cross-PRU memory mapping)



Changelog
---------

v1.0.0-SNAPSHOT
- Supported instructions: arithmetic, LMBD, LDI, JAL, JMP, QB
- Not yet implemented: LBBO/SBBO, LBCO/SBCO, SCAN, HLT, SLP

Author
------
Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
