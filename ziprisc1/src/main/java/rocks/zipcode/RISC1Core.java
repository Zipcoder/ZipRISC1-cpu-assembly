package rocks.zipcode;

public interface RISC1Core {

    void store(int address, Word w) ;
    Word fetch(int address) ;

    void wset(int register, Word w) ;

    default Word wget() {
         return wget();
    }

    Word wget(int register) ;

    int get(int register) ;
    void set(int register, int i) ;

    void halt();

    int inputInt() ;
    void outputInt(int i);

    // for cpu integer stack
    // popi - pops top of integer_stack to register
    void popi(int register);
    // pushi - pushes register onto top of integer stack
    void pushi(int register);
    // op(ISA op) - if possible, performs op on top two stack elements,
    // leaving the result at top of stack
    void op(ISA op);

}
