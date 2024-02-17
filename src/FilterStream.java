import java.io.IOException;
import java.io.InputStream;

public class FilterStream extends InputStream {
	
	private InputStream in;
	
	FilterStream(InputStream stream) {
		in = stream;
	}

	public int read() throws IOException {
		return in.read();
	}

	public int read(byte[] b) throws IOException {
		return in.read(b);
	}

	public int read(byte[] b, int offset, int length) throws IOException {
		return in.read(b, offset, length);
	}

	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	public int available() throws IOException {
		return in.available();
	}

}
