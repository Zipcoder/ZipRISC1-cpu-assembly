package rocks.zipcode;

import java.lang.System;
import java.util.Scanner;

// class handles the registers, the memory, i/o (ints), and the "status word"

public class CPU implements RISC1Core {
   //
    // convenience constants for some dedicated registers
    public static final int MAXREGS = 32;
    public static final int MEMORY_SIZE = 0x100;

    public static final int PC = MAXREGS-1; // Program Counter 1F
    public static final int IR = MAXREGS-2; // Instruction Decode Register 1E
    public static final int SP = MAXREGS-3; // Stack Pointer 1D
    public static final int FP = MAXREGS-4; // Frame Pointer 1C
    public static final int RA = 1; // use x1 for return address (see CALL and RET)

    private Integer[] registerFile = new Integer[MAXREGS];

    private Word[] memory = new Word[MEMORY_SIZE];
    private Word instruction = new Word(0);

    public static final int STACK_LIMIT = 8;
    private Integer[] opstack = new Integer[STACK_LIMIT];
    private Integer tos = -1; 

    private Scanner stdin;
    private int inputWord = 0;
    private int outputWord = 0;
    int statusWord = 0;

    public CPU() {
        this(0);
    }

    // initial all registers to a specific value
    // zero out memory
    public CPU(int rinit) {
        for (int i=0; i < MAXREGS; i++) {
            registerFile[i] = rinit;
        }
        for (int i=0; i < MEMORY_SIZE; i++) {
            memory[i] = new Word(0);
        }
        this.stdin = new Scanner(System.in);
    }

    public int memorysize() {
        return MEMORY_SIZE;
    }
    public int registerfilesize() {
        return MAXREGS;
    }
    // CPU state manipulation
    // primarily here for Engine's methods.
    public boolean isRunnable() {
        return (this.statusWord == 0);
    }
    public boolean setRunnable() {
        this.statusWord = 0;
        return true;
    }
    public boolean haltCPU() {
        this.statusWord = -1;
        // System.err.println("setting cpu status to -1");
        return true;
    }

    // Throws a Panic if register is out of range.
    private void checkRegister(int register) {
        if (register < 0 || register >= MAXREGS) {
            System.err.printf("bogus register %02X %d\n", register, register);
            this.dumpState();
            throw new Panic("illegal register value");
        }
    }

    // Throws a Panic if address is out of range.
    private void checkAddress(int address)  {
        if (address < 0 || address > MEMORY_SIZE) {
            System.err.printf("wrong address %04X %d\n", address, address);
            this.dumpState();
            throw new Panic("address out of range");
        }
    }

    // store a Word at memory address
    @Override
    public void store(int address, Word w)  {
        this.checkAddress(address);
        memory[address] = w;
    }

    // fetch a Word from memory address
    @Override
    public Word fetch(int address)  {
        this.checkAddress(address);
        return memory[address];
    }

    // set register to contents of Word
    @Override
    public void wset(int register, Word w) {
        int i = w.getInt();
        if (register == CPU.IR) {
            instruction.set(i);
            System.err.printf("// PC@ %04X : [%s]\n", this.get(CPU.PC), instruction.toString());
        }
        this.set(register, i);
    }

    // convenience methods for Instruction decode.

    public ISA opcode() {
        // map an int opcode to a ISA enum
        int opInt = instruction.opcode();
        ISA opcode = ISA.getISA(opInt);
        if (opcode == null) {
            return ISA.HCF;
        }
        return opcode;
    }

    public int arg1() {
        return instruction.arg1();
    }
    
    public int arg2() {
        return instruction.arg2();
    }
    
    public int arg3() {
        return instruction.arg3();
    }
    
    // get a Word from register
    @Override
    public Word wget(int register) {
        // TODO should this not create a new Word?
        return new Word(get(register));
    }

    // get int from register
    @Override
    public int get(int register) {
        checkRegister(register);
        if (register == 0) return 0; //always return zero from Reg 0.
        return this.registerFile[register];
    }

    // set register to int
    @Override
    public void set(int register, int i) {
        checkRegister(register);
        if (register == 0) return; // never allow reg 0 to be set.
        this.registerFile[register] = i;
    }

    @Override
    public void halt() {
        // just stop already.
        this.haltCPU();
    }

    // IO routines

    @Override
    public int inputInt()  {
        System.out.print("? ");
        this.inputWord = this.stdin.nextInt();
        this.stdin.nextLine();
        return inputWord;
    }

    @Override
    public void outputInt(int i) {
        this.outputWord = i;
        System.out.println("> " + this.outputWord);
    }

    public void dumpState() {
        // if the register is set to zero, do not DUMP
        System.err.println("==== Registers");
        int i = 0;
        for (Integer reg : this.registerFile) {
            if (reg != 0) {
                System.err.printf("%02X - 0x%08X - %d\n", i, reg, reg );
            }
            i++;
        }

        // if the contents of memory Word is set to zero, do not DUMP
        i = 0;
        boolean hole = false;
        System.err.println("==== Memory");
        for (Word w : memory) {
            if (w.isZero() == false) {
                hole = false;
                System.err.printf("0x%04X - %s\n", i, w.toString() );
            } else {
                if (hole == false) {
                    System.err.printf("0x%04X - %s\n", i, w.toString() );
                    System.err.printf(":\n");
                    hole=true;
                }
            }
            i++;
        }
        System.err.println("==== ");
    }

    private int ppop() {
        try {
            tos -= 1;
            return opstack[tos+1];    
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new Panic("internal cpu stack underflow "+tos);
        }
    }
    private void ppush(int i) {
        try {
            tos += 1;
            opstack[tos] = i;    
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new Panic("internal cpu stack overflow "+tos);
        }
    }

    @Override
    public void popi(int register) {
        checkRegister(register);
        this.set(register, ppop());
    }

    @Override
    public void pushi(int register) {
        checkRegister(register);
        ppush(this.get(register));
    }

    @Override
    public void op(ISA op) {
        throw new Panic("internal cpu stack OPS not yet implemented");
    }

}
