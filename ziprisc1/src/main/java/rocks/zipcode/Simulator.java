package rocks.zipcode;

import java.io.File;

/* this class is the main entry point for 
 * the Simulator of the ZipRISC1 processor 
 * */

public class Simulator {
    private CPU cpu;
    public static boolean DEBUG = false;

    public Simulator(CPU cpu) {
        this.cpu = cpu;
    }

    public static void main(String[] args) {
        Simulator sim = new Simulator(new CPU(0x0));
        System.err.println("**** ZipRISC1 simulation 1.3 ****\n");
        if (DEBUG) {
            System.err.print("args ");
            for (String arg : args) {
                System.out.print(" ");
                System.out.print(arg);
            }
            System.err.println();    
        }
        if (args.length == 1) {
            try {
                sim.loadZEXFile(args[0]); // load the machine code to memory
                sim.run(); // start the machine code
            } catch (Panic e) {
                e.printStackTrace();
                java.lang.System.exit(-1);
            }    
        } else {
            System.err.println("no input file.");
            java.lang.System.exit(-1);
        }
        java.lang.System.exit(0);
    }

    private void run() {
        Engine engine = new Engine(this.cpu);
        engine.runAt(0x0000);
    }

    private void loadZEXFile(String executable_filename)  {
        File tempFile = new File(executable_filename);
        if (tempFile.exists() != true)
            throw new Panic("Panic: input executable file not found");

        // open and load each line.
        java.io.BufferedReader reader;
		try {
			reader = new java.io.BufferedReader(new java.io.FileReader(
                executable_filename));
			String line = reader.readLine();
			while (line != null) {
                if (DEBUG) {
                    System.err.println(line);
                }

                this.loadMemoryLocation(line);

                // read next line
				line = reader.readLine();
			}
			reader.close();
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
    }

    private void loadMemoryLocation(String line)  {
        // line should be in this format:
        // hex_memory_address byte0 byte1 byte2 byte3 // comments
        
        String[] tokens = line.split("\\s+");
        if (tokens.length < 5)
            throw new Panic("corrupted line in file.");
        int aa = Integer.decode(tokens[0]);
        Word w = new Word(Integer.parseInt(tokens[1], 16),
        Integer.parseInt(tokens[2], 16),
        Integer.parseInt(tokens[3], 16),
        Integer.parseInt(tokens[4], 16));

        this.cpu.store(aa, w);
    }
}
