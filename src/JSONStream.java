import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cc.nnproject.json.JSONException;

public class JSONStream {

	private InputStream stream;
	private InputStreamReader reader;

	public JSONStream(InputStream is) throws IOException {
		init(is);
	}
	
	private void init(InputStream is) throws IOException {
		this.stream = is;
		this.reader = new InputStreamReader(new FilterStream(is), "UTF-8");
	}
	
	String nextString() throws IOException {
		if(reader.read() != '"') {
			throw new JSONException("nextString failed");
		}
		StringBuffer sb = new StringBuffer();
		char l = 0;
		while(true) {
			char c = (char) reader.read();
			if(c == '\\' && l != '\\') {
				l = c;
				continue;
			}
			if(c == 'u' && l == '\\') {
				sb.append(l = (char) Integer.parseInt("" + next() + next() + next() + next(), 16));
				continue;
			}
			if(c == 0 || (l != '\\' && c == '"')) break;
			sb.append(c);
			l = c;
		}
		return sb.toString();
	}
	
	void skipString() throws IOException {
		if(reader.read() != '"') {
			throw new JSONException("skipString failed");
		}
		char l = 0;
		while(true) {
			char c = (char) reader.read();
			if(c == 0 || (l != '\\' && c == '"')) break;
			l = c;
		}
	}
	
	void skip(int n) throws IOException {
		reader.skip(n);
	}

	char next() throws IOException {
		return (char) reader.read();
	}
	
	public void reset() throws IOException {
		stream.reset();
		reader.close();
		reader = new InputStreamReader(new FilterStream(stream), "UTF-8");
	}
	
	public void reset(InputStream is) throws IOException {
		try {
			close();
		} catch (IOException e) {}
		init(is);
	}
	
	public void close() throws IOException {
		reader.close();
		stream.close();
	}

}
