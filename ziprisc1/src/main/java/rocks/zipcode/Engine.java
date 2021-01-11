package rocks.zipcode;

/* this Engine should be the 
 * "microcode" runner, the object that 
 * does all the actual work 
 * of the processor.
 * It implements all the BASE (or core) instructions,
 * using the "cpu" object provided
 * 
 * This is where you will do most of your work.
 */

public class Engine {

    private CPU cpu = null;
    private int instructionsRun = 0;

	public Engine(CPU cpu) {
        this.cpu = cpu;
    }
    
    public void runAt(int initial_address)  {
        if (initial_address >= CPU.MEMORY_SIZE) {
            throw new Panic("memory violation");
        }
        long startTime = System.nanoTime();

        // set program counter to initial address
        // it should be an instruction to run
        cpu.set(CPU.PC, initial_address);
        
        cpu.setRunnable(); // start cpu
        //int haltOpcode = ISA.HLT.getOpcode();

        while (cpu.isRunnable()) {
            // LOAD current instruction into IR
            cpu.wset(CPU.IR, cpu.fetch(cpu.get(CPU.PC)));
            // INCREMENT Program Counter
            cpu.set(CPU.PC, cpu.get(CPU.PC)+1);

            // execute current instruction
            this.decodeAndExecute(cpu.opcode(),
                cpu.arg1(), cpu.arg2(), cpu.arg3());
            this.instructionsRun += 1;
            // if (cpu.opcode().getOpcode() == haltOpcode) {
            //     System.err.println("FORCE HALT");
            //     break;
            // }
        }
        long endTime = System.nanoTime();
        // on Exit
        System.err.println("...final cpu state...");
        long duration = (endTime - startTime)/1000000;  //divide by 1000000 to get milliseconds.   
        System.err.printf("[%d] total instructions run in (%d)ms.\n", this.instructionsRun, duration);
        this.cpu.dumpState();
	}

    // this is one of those pieces of code that doesn't get more "clean"
    // when you break it up into smaller and smaller implementations.
    // In the case of a whole boatload of options of what the opcode could be,
    // keeping them all in one large method is really the only thing that works.
    // Unless, heh heh, you want to do a map of functions (lambdas).
    //
    // decodeAndExecute the instruction held currently in CPU.IR
    private void decodeAndExecute(ISA opcode, int arg1, int arg2, int arg3) {
        switch (opcode) {
            case HLT:
                System.err.printf("...halting at address: 0x%04X\n", cpu.get(CPU.PC)-1);
                cpu.haltCPU();
                break;
            case BRZ:
                branchOnZero(arg1, arg2, arg3);
                break;
            case ADD:
                add(arg1, arg2, arg3);
                break;
            default:
                // 
                System.err.println("...dumping cpu state due to Panic...");
                cpu.dumpState();
                String irname = String.format("0x%2X", CPU.IR);
                throw new Panic("Unimplemented instruction: look at contents of register "+irname);
        }
    }
    
    // Instruction Implementations.


    // ADD
    private void add(int arg1, int arg2, int arg3) {
        cpu.set(arg1, cpu.get(arg2) + cpu.get(arg3));
    }

    // BRZ
    private void branchOnZero(int arg1, int arg2, int arg3) {
        int address = makeAddress(arg2, arg3);
        if (cpu.get(arg1)==0) {
            cpu.set(CPU.PC, address);
        }
    }

    // make the two argument bytes into one 16bit address.
    private int makeAddress(int arg2, int arg3) {
        int addr = ((arg2 & 0x0000FF00) | (arg3 & 0x000000FF));
        if (addr < 0 || addr >= CPU.MEMORY_SIZE) {
            System.err.printf("Engine: Bad Address: %X \n", addr);
            throw new Panic("Engine: makeAddress: made incorrect address.");
        }
        return addr;
    }

}
