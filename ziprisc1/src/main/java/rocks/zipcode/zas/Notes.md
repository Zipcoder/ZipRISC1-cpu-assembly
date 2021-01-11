# ZAS - ZipRISC1 Assembler

`zas` takes a `.zas` file and translates it into ZipRISC1 machine code.

Because the layout of the instructions of the processor are always 32-bit, it makes the code generation pretty easy.
Each line in the output file represents one "word" (4 bytes, 32bits) of main memory in the processor.
Each line number is therefore the main memory address. In the output file, it is shown as a 4-digit hexadecimal number.
The assembler keeps track of this in the ZAS instance variable `address` which is incremented after each word is added to the instruction list.

The input is handled:

- read in the line
- matchit to one of three possible line patterns
- depending on the match
  - treat it as a directive
  - treat it as a label
    - putting the label into the symbol table
  - treat it as an instruction
    - switch across all the opcodes
    - decide on what code to generate
    - place code into instruction list
- after all input has been handled
  - step thru the instruction list
  - examine instruction for missing forward references
  - resolve all forward references to labels
  - output the instruction

The basic idea is that it reads in each line of the input file and attempts to sort the line into one of three patterns:

- label line
- directive line
- instruction line

When it decides, using regex patterns, which of the three line patterns it is, it dispatches the line into one of three handling routines.

The label line is easy. It takes the current global address tracking variable and inserts the symbols name into the `symbols` table, associating a String with a memory address (the current global address).

The directive line implements one of the few assembler directives we use.

The instruction (code) handler breaks the line into tokens and then based on the opcode (tokens[0]), produces a given `WordAt` object. This is then appended to the List kept of each Instruction.

At the end of the file, the instruction list is looped through, any forward symbol references are resolved, as the output is being turned into text for the output file.

`zas` uses System.err as its debugging and diagnostic output, System.out for the machine code production.
So, that means you could use the shell to catch each of the two streams and do something with them.
(See below).
Generally, it's best to use

```
./zas inputfile.zas > outputfile.zex
```

This will take `inputfile.zas` as source, and produce a file as output `outputfile.zex`.
The dianostic messages printed to Standard Error (StdErr) will appear under the command, but not be put into your output.

The machine code file (.zex) is utf-8 text, not a binary data file, which makes it easier to look at and debug.

Just in case you are puzzled over the difference between System.out (stdout) and System.err (stderr), this might get you started. Google more about shells and their three common files for every process.

### Shell, StdIn, StdOut, StdErr & You

```
echo test > afile.txt
```

redirects stdout to afile.txt. This is the same as doing

```
echo test 1> afile.txt
```

Notice that there is _no spaces_ between the number and the greater-than symbol.

To redirect stderr, you do:

```
echo test 2> afile.txt
```

>& is the syntax to redirect a stream to another file descriptor - 0 is stdin, 1 is stdout, and 2 is stderr.

You can redirect stdout to stderr by doing:

```
echo test 1>&2 
```

Or vice versa:

```
echo test 2>&1
```

So, in short... 2> redirects stderr to an (unspecified) file, appending &1 redirects stderr to stdout.
