package rocks.zipcode.zas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rocks.zipcode.CPU;
import rocks.zipcode.ISA;
import rocks.zipcode.Panic;
import rocks.zipcode.Word;

/*
 * the ZipRISC1 assembler.
 */

public class ZAS {
    public static final boolean DEBUG = true;
    //public static final int MAXREGS = ISA.MAXREGS;

    ArrayList<WordAt> instructions = new ArrayList<>();
    SymbolTable symbols = new SymbolTable();
    
    HashMap<String,Integer> registers = new HashMap<>();
    HashMap<String,Integer> opcodes = new HashMap<>();
    int currentLineNum = 0;
    String currentLine = "";

    public static void main(String[] args) {
        ZAS zas = new ZAS();
        System.err.println("//**** ZipRISC1 Assembler v1.3 ****");
        if (DEBUG) {
            System.err.print("// args ");
            for (String arg : args) {
                System.err.print(" ");
                System.err.print(arg);
            }
            System.err.println();    
        }
        if (args.length == 1) {
            try {
                zas.initializeTables();
                zas.parseFile(args[0]);
                zas.dumpSymbols();
                zas.outputResults();
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

    String directiveLine = "^\\.([A-Z][A-Z])"; // dot, TWO UPPERCASE chars, space, and the rest
    String labelLine = "^([a-z0-9]+):"; // start of line, all lowercase alphanum, colon.
    String codeLine = "[\\s+]([A-Z]+)[\\s]*([\\w]*)[,]*[\\s]*([\\w]*)[,]*[\\s]*(\\-?[\\w]*)";
    Pattern directive_pattern;
    Pattern label_pattern;
    Pattern code_pattern;
    Integer address = 0;
    
    public ZAS() {
        this.directive_pattern = Pattern.compile(directiveLine, Pattern.CASE_INSENSITIVE);
        this.label_pattern = Pattern.compile(labelLine, Pattern.CASE_INSENSITIVE);
        this.code_pattern = Pattern.compile(codeLine, Pattern.CASE_INSENSITIVE);
    }

    public void parseFile(String filename) {
        java.io.File tempFile = new java.io.File(filename);
        if (tempFile.exists() != true)
            throw new Panic("Panic: input assembly file not found");
        
        // open and load each line.
        java.io.BufferedReader reader;
		try {
			reader = new java.io.BufferedReader(new java.io.FileReader(
                filename));
            this.currentLine = reader.readLine();
            this.currentLineNum++;
			while (this.currentLine != null) {
                this.parseLine(this.currentLine);
                // read next line
				this.currentLine = reader.readLine();
                this.currentLineNum++;
			}
			reader.close();
		} catch (java.io.IOException e) {
            e.printStackTrace();
		}
    }


    private void parseLine(String line) {

        if (DEBUG) System.err.print("//> ");
        String lineNoComments = line;
        int index = line.indexOf("//");
        if (index >= 0) {
            lineNoComments= line.substring(0, index);
        }
        if (DEBUG) System.err.print(lineNoComments);

        Matcher lMatch = label_pattern.matcher(lineNoComments);
        Matcher dMatch = directive_pattern.matcher(lineNoComments);
        Matcher codeMatch = code_pattern.matcher(lineNoComments);
        
        if (codeMatch.find()) {
            appendWord(this.handleCode(lineNoComments, codeMatch.group(1)));
        } else if (dMatch.find()) {
            this.handleDirective(lineNoComments, dMatch.group(1));
        } else if (lMatch.find()) {
            this.handleLabel(lineNoComments, lMatch.group(1));
        }
        if (DEBUG) System.err.println();
    }

    private void handleDirective(String line, String dir) {
        String[] tokens = line.split("\\s");
        if (DEBUG) {
            System.err.print(" /"+dir+"/ ");
            System.err.print("["+line+"]");
            for (String token : tokens) {
                System.err.print(" >> "+token);
            }
        }

        if (dir.equals("WD")) {
            int wordSaved = Integer.parseInt(tokens[1]);
            this.instructions.add(new WordAt(currentAddressString(), 
                new Word(wordSaved)));
            this.address++;
        }
        if (dir.equals("OR")) {
            int newStart = Integer.decode(tokens[1]);
            this.address = newStart;
        }
        // if (dir.equals("EQ")) {
        //     if (symbols.containsKey(tokens[1])){
        //         throw new Error("Can't redefine an existing symbol: "+tokens[1]);
        //     }
        //     symbols.put(tokens[1], tokens[2]);
        // }
    }

    private void handleLabel(String line, String label) {
        if (DEBUG) System.err.print(" // "+label+" ");
        symbols.put(label, currentAddressString());
    }

    private String currentAddressString() {
        return String.format("0x%04X", this.address);
    }

/*
         * PSEUDOs
        - MOV rd, rs │ ADD rd, rs, x0 │ rd ← rs
        - CLR rd │ ADD rd, x0, x0 │ rd ← 0
        - DEC rd │ SUBI rd, rd, 1 │ rd ← rd - 1
        - INCR rd |ADD rd, rd, 1  | rd <- rd + 1
        - BRA aa │ BRZ x0, aa │ next instruction to read is at aa
        * the calling convention for subroutines/functions.
        - CALL aa | ADDI x1 xPC 1; BRA aa | ra <- PC + 1, jump to aa
        - RET | ADD xPC x1 x0 | pc <- ra (ra is "return address")
        - POP rd | LDR rd, SP; INCR SP | load rd with contents SP, sp <- sp + 1
        - PUSH rd | DECR SP; STR rd SP | sp <- sp - 1, store rd to contents of SP
        */

    private void tokensCheck(int needs, String[] tokens) {
        // System.out.printf("\n[ %d ? %d ]\n", tokens.length, needs);
        if (tokens.length < needs) {
            throw new Panic("not enough tokens for instruction at line: "+atLine()+"\nline: "+this.currentLine);
        }
        int t0 = 0;
        for (String t : tokens) {
            // System.out.println(t);
            if (t.equals("")) {
                throw new Panic("\nexpected token at position "+Integer.toString(t0)+" is empty at line: "+atLine()+"\nline: "+this.currentLine);
            }
            t0++;
        }
    }
    private WordAt handleCode(String line, String opcode) {
        // change to standard interface, and call create opcode
        line = line.trim().replace(",", "");
        String[] tokens = line.split("\\W");

        if (DEBUG) { 
            System.err.print(" // "+opcode);
            System.err.print("["+line+"]");
            for (String token : tokens) {
                System.err.print(" "+token);
            }
        }
        if (opcode.equals("ADD")) {
            tokensCheck(4, tokens);
            return new3argWord(opcode, tokens[1], tokens[2], tokens[3]);
        }
        if (opcode.equals("MOV")) {
            tokensCheck(3, tokens);
            return new3argWord("ADD", tokens[1], tokens[2], "x0");
        }
        // pseudo CLR rd │ ADD rd, x0, x0 │ rd ← 0
        if (opcode.equals("CLR")) {
            tokensCheck(2, tokens);
            return new3argWord("ADD", tokens[1], "x0", "x0");
        }
        // pseudo INCR rd |ADD rd, rd, 1  | rd <- rd + 1
        if (opcode.equals("INCR")) {
            tokensCheck(2, tokens);
            return newShiftWord(opcode, tokens[1], tokens[1], "1");
        }
        if (opcode.equals("LDR")) {
            tokensCheck(2, tokens);
            return newShiftWord(opcode, tokens[1], tokens[2], "0");
        }
        if (opcode.equals("STR")) {
            tokensCheck(2, tokens);
            return newShiftWord(opcode, tokens[1], tokens[2], "0");
        }
        if (opcode.equals("HLT")) {
            return newHaltWord();
        }
        if (opcode.equals("DUMP")) {
            return newDumpWord();
        }
        if (opcode.equals("LD")) {
            tokensCheck(3, tokens);
            return newMemoryWord(opcode, tokens[1], tokens[2]);
        }
        if (opcode.equals("LDI")) {
            tokensCheck(3, tokens);
            return newMemoryWord(opcode, tokens[1], tokens[2]);
        }
        if (opcode.equals("ST")) {
            tokensCheck(3, tokens);
            return newMemoryWord(opcode, tokens[1], tokens[2]);
        }
        // pseudo BRA  
        if (opcode.equals("BRA")) {
            tokensCheck(2, tokens);
            return newBRZWord("BRZ", "x0", tokens[1]);
        }
        if (opcode.equals("BRZ")) {
            tokensCheck(3, tokens);
            return newBRZWord("BRZ", tokens[1], tokens[2]);
        }
        if (opcode.equals("SUB")) {
            tokensCheck(4, tokens);
            return new3argWord(opcode, tokens[1], tokens[2], tokens[3]);
        }
        // newShiftWord doesnt resolve last arg, it treats it as integer
        if (opcode.equals("SUBI")) {
            tokensCheck(4, tokens);
            return newShiftWord(opcode, tokens[1], tokens[2], tokens[3]);
        }
        if (opcode.equals("ADDI")) {
            tokensCheck(4, tokens);
            return newShiftWord(opcode, tokens[1], tokens[2], tokens[3]);
        }
        if (opcode.equals("LSH")) {
            tokensCheck(4, tokens);
            return newShiftWord(opcode, tokens[1], tokens[2], tokens[3]);
        }
        if (opcode.equals("RSH")) {
            tokensCheck(4, tokens);
            return newShiftWord(opcode, tokens[1], tokens[2], tokens[3]);
        }
        // DEC rd │ SUBI rd, rd, 1 │ rd ← rd - 1
        if (opcode.equals("DEC")) {
            tokensCheck(2, tokens);
            return newShiftWord("SUBI", tokens[1], tokens[1], "1");
        }
        // IN rd | Ad00 | read in a number to rd
        if (opcode.equals("IN")) {
            tokensCheck(2, tokens);
            return newIOWord(opcode, tokens[1]);
        }
        // OUT rd | Bd00 | output a number from rd
        if (opcode.equals("OUT")) {
            tokensCheck(2, tokens);
            return newIOWord(opcode, tokens[1]);
        }
        // pseudo RET | ADD xPC x1 x0 | pc <- ra (ra is "return address")
        if (opcode.equals("RET")) {
            tokensCheck(1, tokens);
            return new3argWord("ADD", "xPC", "x1", "x0");
        }
        // pseudo CALL aa | ADDI x1 xPC 1; BRA aa | ra <- PC + 1, jump to aa
        if (opcode.equals("CALL")) {
            tokensCheck(2, tokens);
            this.appendWord(newShiftWord("ADDI", "x1", "xPC", "1"));
            return newBRZWord("BRZ", "x0", tokens[1]);
        }
        // pseudo PUSH rd | DECR SP; STR rd SP | sp <- sp - 1, store rd to contents of SP
        if (opcode.equals("PUSH")) {
            tokensCheck(1, tokens);
            this.appendWord(newShiftWord("SUBI", "xSP", "xSP", "1"));
            return newShiftWord("STR", tokens[1], "xSP", "0");
        }
        // pseudo POP rd | LDR rd, SP; INCR SP | load rd with contents SP, sp <- sp + 1
        if (opcode.equals("POP")) {
            tokensCheck(1, tokens);
            newShiftWord("LDR", tokens[1], "xSP", "0");
            return newShiftWord("ADDI", "xSP", "xSP", "1");
        }
        // otherwise output deadbeef
        return deadbeef();
    }

    private WordAt deadbeef() {
        return new WordAt(currentAddressString(), 0xDE, 0xAD, 0xBE, 0xEF);
    }

    private void appendWord(WordAt newWord) {
        this.instructions.add(newWord);
        this.address++;
    }
    
    private WordAt newIOWord(String opcode, String rd) {
        return new WordAt(currentAddressString(), resolve(opcode), resolve(rd), 0, 0);
    }

    private WordAt newHCFWord() {
        return new WordAt(currentAddressString(), 0, 0xFF, 0xFF, 0xFF);
    }

    // LSH, RSH, and SUBI and DECR and ADDI & INCR(!)
    private WordAt newShiftWord(String opcode,
        String a1,
        String a2,
        String a3) {
        int immed = Integer.parseInt(a3);
        return new WordAt(currentAddressString(),
            resolve(opcode), resolve(a1), resolve(a2), immed);
    }

    private WordAt newBRZWord(String opcode, String rd, String aa) {
        int addr = resolve(aa);
        WordAt newWord = new WordAt(currentAddressString(),
        resolve(opcode),
        resolve(rd),
        upperHalf(addr),
        lowerHalf(addr)
        );
        if (addr == -1) {
            // it's a forward ref.
            newWord.undefineForwardRef(aa);
        }
        return newWord;
    }

    // LD, LDI and ST
    private WordAt newMemoryWord(String opcode, String a1, String a2) {
        int addr = resolve(a2);
        WordAt newWord = new WordAt(currentAddressString(),
        resolve(opcode),
        resolve(a1),
        upperHalf(resolve(a2)),
        lowerHalf(resolve(a2))
        );
        if (addr == -1) {
            // it's a forward ref.
            newWord.undefineForwardRef(a2);
        }
        return newWord;
    }

    private WordAt newDumpWord() {
        return new WordAt(currentAddressString(), 0x0F, 0, 0, 0);
    }

    private WordAt newHaltWord() {
        return new WordAt(currentAddressString(), 0, 0, 0, 0);
    }

    // both ADD and SUB
    private WordAt new3argWord(String op,
            String a1,
            String a2,
            String a3) {
        return new WordAt(currentAddressString(),
        resolve(op), resolve(a1), 
        resolve(a2), resolve(a3));
    }

    private int lowerHalf(int addr) {
        int i = (addr & 0xFF);
        return i;
    }

    private int upperHalf(int addr) {
        int i = (addr & 0xFF00) >> 16;
        return i;
    }

    private int resolve(String token) {
        if (token.equals("") || token == null) {
            throw new Panic("expected token is empty on line "+atLine()+"\nline: "+this.currentLine);
        }
        // try to resolve as a symbol
        if (this.symbols.containsKey(token)) {
            return Integer.decode(this.symbols.get(token));
        }
        // or try as a register name
        if (this.registers.containsKey(token)) {
            return this.registers.get(token);
        }
        // otherwise try to interpret the token as address
        if (token.startsWith("0x")){
            int newAddress = decodeToInt(token);
            return newAddress;            
        }
        // assume at this point the token is a forward symbol reference.
        return -1;
    }

    private int decodeToInt(String token) {
        try {
            return Integer.decode(token);
        } catch (NumberFormatException e) {
            throw new Panic("error in reading an integer: "+atLine()+" ["+token+"]"+"\nline: "+this.currentLine);
        }
    }

    private int parseToInt(String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new Panic("error in parsing an integer: "+atLine()+" ["+token+"]"+"\nline: "+this.currentLine);
        }
    }

    private String atLine() {
        return Integer.toString(this.currentLineNum);
    }

    private void dumpSymbols() {
        System.err.print("// SYMBOLS\n");
        for (Map.Entry<String, String> entry : this.symbols.entrySet()) {
            System.err.println("// "+entry.getKey() + ":" + entry.getValue().toString());
       }
       System.err.print("// END SYMBOLS\n");
    }

    private void outputResults() {
        // resolve any unknown forward refs.
        // and output
        for (WordAt w : this.instructions) {
            if (w.isForwardReference()) {
                if (symbols.containsKey(w.getForwardref())) {
                    String a = symbols.get(w.getForwardref());
                    w.defineForwardReference(a);
                } else {
                    throw new Panic("unable to resolve symbol "+w.getForwardref());
                }
            }
            System.out.println(w.toString());
        }
    }

    private void initializeTables() {
        // load up the registers table
        registers.put("x0", 0); registers.put("x1", 1); 
        registers.put("x2", 2); registers.put("x3", 3); 
        registers.put("x4", 4); registers.put("x5", 5); 
        registers.put("x6", 6); registers.put("x7", 7); 
        registers.put("x8", 8); registers.put("x9", 9); 
        registers.put("xA", 10); registers.put("xB", 11); 
        registers.put("xC", 12); registers.put("xD", 13); 
        registers.put("xE", 14); registers.put("xF", 15); 

        registers.put("x10", 16); registers.put("x11", 17); 
        registers.put("x12", 18); registers.put("x13", 19); 
        registers.put("x14", 20); registers.put("x15", 21); 
        registers.put("x16", 22); registers.put("x17", 23); 
        registers.put("x18", 24); registers.put("x19", 25); 
        registers.put("x1A", 26); registers.put("x1B", 27); 
        registers.put("x1C", 28); registers.put("x1D", 29); 
        registers.put("x1E", 30); registers.put("x1F", 31);
        registers.put("xPC", CPU.PC);
        registers.put("xIR", CPU.IR);
        registers.put("xSP", CPU.SP);
        registers.put("xFP", CPU.FP);

        // load up opcodes
        registers.put("ADD", ISA.ADD.getOpcode());
        registers.put("INCR", ISA.ADD.getOpcode());
        registers.put("ADDI", ISA.ADDI.getOpcode());
        registers.put("MOV", ISA.ADD.getOpcode());
        registers.put("SUB", ISA.SUB.getOpcode());
        registers.put("SUBI", ISA.SUBI.getOpcode());
        registers.put("LSH", ISA.LSH.getOpcode());
        registers.put("RSH", ISA.RSH.getOpcode());
        registers.put("HLT", ISA.HLT.getOpcode());
        registers.put("DUMP", ISA.DUMP.getOpcode());
        registers.put("BRA", ISA.BRZ.getOpcode());
        registers.put("BRZ", ISA.BRZ.getOpcode());
        registers.put("BGT", ISA.BGT.getOpcode());
        registers.put("LD", ISA.LD.getOpcode());
        registers.put("LDI", ISA.LDI.getOpcode());
        registers.put("ST", ISA.ST.getOpcode());
        registers.put("LDR", ISA.LDR.getOpcode());
        registers.put("STR", ISA.STR.getOpcode());
        registers.put("IN", ISA.IN.getOpcode());
        registers.put("OUT", ISA.OUT.getOpcode());

        
    }

}
