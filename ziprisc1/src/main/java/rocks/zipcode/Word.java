package rocks.zipcode;

// class Word is a tricky little bit of code.
// it mimics a 32 bit word that is also an array of 4 bytes.
// the tricky bit is the way the bytes get bitwise-and'd and or'd in and out
// of a single integer which the word can represent.
//
// make sure you DO NOT set the high bit - otherwise when you shift right, the two's complement
// bit (the negative bit) gets replicated...
// like I say, a bit tricky.
//
public class Word {
    protected int[] b = new int[4];

	public Word() {
		this.b[3] = 0;
		this.b[2] = 0;
		this.b[1] = 0;
		this.b[0] = 0;
	}
	
	public Word(int i) {
		this.b[3] = i & 0x000000FF;
		this.b[2] = (i & 0x0000FF00) >> 8;
		this.b[1] = (i & 0x00FF0000) >> 16;
		this.b[0] = (i & 0xFF000000) >> 24;
	}

	public Word(int i, int j, int k, int l) {
		this.b[0] = i & 0x000000FF;
		this.b[1] = j & 0x000000FF;
		this.b[2] = k & 0x000000FF;
		this.b[3] = l & 0x000000FF;
	}

	public void set(int i) {
		this.b[3] = i & 0x000000FF;
		this.b[2] = (i & 0x0000FF00) >> 8;
		this.b[1] = (i & 0x00FF0000) >> 16;
		this.b[0] = (i & 0xFF000000) >> 24;
	}
	
	public int getInt() {
		return ((b[0] & 0xFF) << 24) | 
		((b[1] & 0xFF) << 16) | 
		((b[2] & 0xFF) << 8 ) | 
		((b[3] & 0xFF) << 0 );
	}

	public int getByte(int i) {
		return b[i];
	}

	public String toString() {
		return String.format("%02X %02X %02X %02X", this.b[0], this.b[1], this.b[2], this.b[3]);
	}

	public boolean isZero() {
		for (int i : b) {
			if (i != 0) return false;
		}
		return true;
	}

	public int opcode() {
		return this.b[0];
	}
	public int arg1() {
		return this.b[1];
	}
	public int arg2() {
		return this.b[2];
	}
	public int arg3() {
		return this.b[3];
	}

}