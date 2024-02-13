import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cc.nnproject.json.JSONException;

public class JSONStream {

	private InputStream stream;
	private InputStreamReader reader;
	private boolean isObject;
	int index;
	private char prev;
	private boolean usePrev;

	public JSONStream(InputStream is) throws IOException {
		init(is);
	}
	
	private void init(InputStream is) throws IOException {
		this.stream = is;
		this.reader = new InputStreamReader(is, "UTF-8");
		char c = next();
		if(c != '{' && c != '[') throw new JSONException("Not json");
		isObject = c == '{';
		if(!isObject) throw new JSONException("Arrays streaming not implemented");
		usePrev = true;
	}
	
	String nextString() throws IOException {
		if(next() != '"') {
			back();
			throw new JSONException("nextString(): not string at " + index);
		}
		StringBuffer sb = new StringBuffer();
		char l = 0;
		while(true) {
			char c = next();
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
		if(next() != '"') {
			back();
			throw new JSONException("skipString(): not string at " + index);
		}
		char l = 0;
		while(true) {
			char c = next();
			if(c == 0 || (l != '\\' && c == '"')) break;
			l = c;
		}
	}
	
	void assertNext(String f, char c) throws IOException {
		char n;
		if((n = next()) != c) throw new JSONException(f + ": Assertion failed: \'" + n + "\', expected: " + c + " at " + (index-1));
	}
	
	void back() {
		if(usePrev || index <= 0) throw new JSONException("back");
		usePrev = true;
		index--;
	}

	char next() throws IOException {
		if(usePrev) {
			usePrev = false;
			index++;
			return prev;
		}
		int c = reader.read();
		if(c <= 0) {
			return 0;
		}
		prev = (char) c;
		index++;
		return prev;
	}
	
	public void reset() throws IOException {
		index = prev = 0;
		usePrev = false;
		reader.reset();
	}
	
	public void reset(InputStream is) throws IOException {
		try {
			close();
		} catch (IOException e) {}
		index = prev = 0;
		usePrev = false;
		init(is);
	}
	
	public void close() throws IOException {
		reader.close();
		stream.close();
	}

}
