/*
Copyright (c) 2023 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/


import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

public class JSON extends InputStream {

	// JSONObject
	
	Hashtable table;

	public JSON(Hashtable table) {
		this.table = table == null ? new Hashtable() : table;
	}
	
	public Object get(String name) {
		try {
			if (has(name)) {
				Object o = table.get(name);
				if (o instanceof String[])
					table.put(name, o = MahoRaspApp2.parseJSON(((String[]) o)[0]));
				if (o == MahoRaspApp2.json_null)
					return null;
				return o;
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
		}
		throw new RuntimeException("JSON: No value for name: " + name);
	}
	
	public Object get(String name, Object def) {
		if (!has(name)) return def;
		try {
			return get(name);
		} catch (Exception e) {
			return def;
		}
	}
	
	public Object getNullable(String name) {
		return get(name, null);
	}
	
	public String getString(String name) {
		Object o = get(name);
		if (o == null || o instanceof String)
			return (String) o;
		return String.valueOf(o);
	}
	
	public String getString(String name, String def) {
		try {
			Object o = get(name, def);
			if (o == null || o instanceof String)
				return (String) o;
			return String.valueOf(o);
		} catch (Exception e) {
			return def;
		}
	}
	
	public String getNullableString(String name) {
		return getString(name, null);
	}
	
	public JSON getObject(String name) {
		try {
			JSON o = (JSON) get(name);
			if(o.table != null)
				return o;
		} catch (ClassCastException e) {
		}
		throw new RuntimeException("JSON: Not object: " + name);
	}
	public JSON getObject(String name, JSON def) {
		if (has(name)) {
			try {
				return (JSON) get(name);
			} catch (Exception e) {
			}
		}
		return def;
	}
	
	public JSON getNullableObject(String name) {
		return getObject(name, null);
	}
	
	public JSON getArray(String name) {
		try {
			JSON o = (JSON) get(name);
			if(o.elements != null)
				return o;
		} catch (ClassCastException e) {
		}
		throw new RuntimeException("JSON: Not array: " + name);
	}
	
	public JSON getArray(String name, JSON def) {
		if (has(name)) {
			try {
				return (JSON) get(name);
			} catch (Exception e) {
			}
		}
		return def;
	}
	
	
	public JSON getNullableArray(String name) {
		return getArray(name, null);
	}
	
	public int getInt(String name) {
		return MahoRaspApp2.getInt(get(name));
	}
	
	public int getInt(String name, int def) {
		if (!has(name)) return def;
		try {
			return getInt(name);
		} catch (Exception e) {
			return def;
		}
	}
	
	public boolean getBoolean(String name) {
		Object o = get(name);
		if (o == MahoRaspApp2.TRUE) return true;
		if (o == MahoRaspApp2.FALSE) return false;
		if (o instanceof Boolean) return ((Boolean) o).booleanValue();
		if (o instanceof String) {
			String s = (String) o;
			s = s.toLowerCase();
			if (s.equals("true")) return true;
			if (s.equals("false")) return false;
		}
		throw new RuntimeException("JSON: Not boolean: " + o);
	}

	public boolean getBoolean(String name, boolean def) {
		if (!has(name)) return def;
		try {
			return getBoolean(name);
		} catch (Exception e) {
			return def;
		}
	}
	
	public boolean isNull(String name) {
		if (!has(name))
			throw new RuntimeException("JSON: No value for name: " + name);
		return table.get(name) == MahoRaspApp2.json_null;
	}
	
	public void put(String name, Object obj) {
		table.put(name, obj);
	}
	
	public void put(String name, String s) {
		table.put(name, s);
	}

	public void put(String name, int i) {
		table.put(name, new Integer(i));
	}

	public void put(String name, long l) {
		table.put(name, new Long(l));
	}

	public void put(String name, double d) {
		table.put(name, new Double(d));
	}

	public void put(String name, boolean b) {
		table.put(name, new Boolean(b));
	}
	
	public boolean hasValue(Object object) {
		return table.contains(object);
	}
	
	// hasKey
	public boolean has(String name) {
		return table.containsKey(name);
	}
	
	public void clear() {
		if(table != null) {
			table.clear();
			return;
		}
		for (int i = 0; i < count; i++) elements[i] = null;
		count = 0;
	}
	
	public void remove(String name) {
		table.remove(name);
	}
	
	public int size() {
		return table != null ? table.size() : count;
	}
	
	public boolean isEmpty() {
		return table != null ? table.isEmpty() : count == 0;
	}
	
	public String toString() {
		return build();
	}
	
	public boolean equals(Object obj) {
		return this == obj || super.equals(obj);
	}

	public String build() {
		if(table != null) {
			if (size() == 0)
				return "{}";
			StringBuffer s = new StringBuffer("{");
			Enumeration keys = table.keys();
			while (true) {
				String k = (String) keys.nextElement();
				s.append('"').append(k).append("\":");
				Object v = table.get(k);
				if (v instanceof JSON) {
					s.append(((JSON) v).build());
				} else if (v instanceof JSON) {
					s.append(((JSON) v).build());
				} else if (v instanceof String) {
					s.append('"').append(escape_utf8((String) v)).append('"');
				} else if (v == MahoRaspApp2.json_null) {
					s.append((String) null);
				} else {
					s.append(v);
				}
				if (!keys.hasMoreElements()) {
					break;
				}
				s.append(",");
			}
			s.append("}");
			return s.toString();
		}
		int size = count;
		if (size == 0)
			return "[]";
		StringBuffer s = new StringBuffer("[");
		int i = 0;
		while (i < size) {
			Object v = elements[i];
			if (v instanceof JSON) {
				s.append(((JSON) v).build());
			} else if (v instanceof JSON) {
				s.append(((JSON) v).build());
			} else if (v instanceof String) {
				s.append('"').append(escape_utf8((String) v)).append('"');
			} else if (v == MahoRaspApp2.json_null) {
				s.append((String) null);
			} else {
				s.append(String.valueOf(v));
			}
			i++;
			if (i < size) {
				s.append(',');
			}
		}
		s.append(']');
		return s.toString();
	}

	public Enumeration keys() {
		return table.keys();
	}

	public JSON keysAsArray() {
		JSON array = new JSON(table.size());
		Enumeration keys = table.keys();
		while (keys.hasMoreElements()) {
			array.addElement(keys.nextElement());
		}
		return array;
	}
	
	// FilterStream buffered
	
	private InputStream in;
	private byte[] buf;
	private int pos;
	
	JSON(InputStream stream) {
		in = stream;
		buf = new byte[2048];
	}

	public int read() throws IOException {
//		return in.read();
		if (pos >= count && fillbuf() == -1) return -1;
		if (count - pos > 0) return buf[pos++] & 0xFF;
		return -1;
	}

	public int read(byte[] b) throws IOException {
//		return in.read(b);
		return read(b, 0, b.length);
	}

	public int read(byte[] buffer, int offset, int length) throws IOException {
//		return in.read(b, offset, length);
		if (length == 0) return 0;
		int required;
		if (pos < count) {
			int copylength = count - pos >= length ? length : count - pos;
			System.arraycopy(buf, pos, buffer, offset, copylength);
			pos += copylength;
			if (copylength == length || in.available() == 0)
				return copylength;
			offset += copylength;
			required = length - copylength;
		} else required = length;

		while (true) {
			int read;
			if (required >= buf.length) {
				read = in.read(buffer, offset, required);
				if (read == -1)
					return required == length ? -1 : length - required;
			} else {
				if (fillbuf() == -1)
					return required == length ? -1 : length - required;
				read = count - pos >= required ? required : count - pos;
				System.arraycopy(buf, pos, buffer, offset, read);
				pos += read;
			}
			required -= read;
			if (required == 0) return length;
			if (in.available() == 0) return length - required;
			offset += read;
		}
	}

	public long skip(long n) throws IOException {
//		return in.skip(n);
		if (n < 1) return 0;

		if (count - pos >= n) {
			pos += n;
			return n;
		}
		long read = count - pos;
		pos = count;
		return read + in.skip(n - read);
	}

	public int available() throws IOException {
//		return in.available();
		return count - pos + in.available();
	}
	
	public void close() {
//		in.close();
		buf = null;
	}
	
	private int fillbuf() throws IOException {
		int r = in.read(buf);
		pos = 0;
		count = r == -1 ? 0 : r;
		return r;
	}
	
	// JSONArray
	
	protected Object[] elements;
	protected int count;
	
	public JSON(int size) {
		elements = new Object[size];
	}

	public Object get(int index) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		try {
			Object o = elements[index];
			if (o instanceof String[])
				o = elements[index] = MahoRaspApp2.parseJSON(((String[]) o)[0]);
			if (o == MahoRaspApp2.json_null)
				return null;
			return o;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
		}
		throw new RuntimeException("JSON: No value at " + index);
	}
	
	// unused methods should be removed by proguard shrinking
	
	public Object get(int index, Object def) {
		try {
			return get(index);
		} catch (Exception e) {
			return def;
		}
	}
	
	public Object getNullable(int index) {
		return get(index, null);
	}
	
	public String getString(int index) {
		Object o = get(index);
		if (o == null || o instanceof String)
			return (String) o;
		return String.valueOf(o);
	}
	
	public String getString(int index, String def) {
		try {
			Object o = get(index);
			if (o == null || o instanceof String)
				return (String) o;
			return String.valueOf(o);
		} catch (Exception e) {
			return def;
		}
	}
	
	public String getNullableString(int index) {
		return getString(index, null);
	}
	
	public JSON getObject(int index) {
		try {
			return (JSON) get(index);
		} catch (ClassCastException e) {
			throw new RuntimeException("JSON: Not object at " + index);
		}
	}
	
	public JSON getObject(int index, JSON def) {
		try {
			return getObject(index);
		} catch (Exception e) {
		}
		return def;
	}
	
	public JSON getNullableObject(int index) {
		return getObject(index, null);
	}
	
	public JSON getArray(int index) {
		try {
			return (JSON) get(index);
		} catch (ClassCastException e) {
			throw new RuntimeException("JSON: Not array at " + index);
		}
	}
	
	public JSON getArray(int index, JSON def) {
		try {
			return getArray(index);
		} catch (Exception e) {
		}
		return def;
	}
	
	public JSON getNullableArray(int index) {
		return getArray(index, null);
	}
	
	public int getInt(int index) {
		return MahoRaspApp2.getInt(get(index));
	}
	
	public int getInt(int index, int def) {
		try {
			return getInt(index);
		} catch (Exception e) {
			return def;
		}
	}
	
	public boolean getBoolean(int index) {
		Object o = get(index);
		if (o == MahoRaspApp2.TRUE) return true;
		if (o == MahoRaspApp2.FALSE) return false;
		if (o instanceof Boolean) return ((Boolean) o).booleanValue();
		if (o instanceof String) {
			String s = (String) o;
			s = s.toLowerCase();
			if (s.equals("true")) return true;
			if (s.equals("false")) return false;
		}
		throw new RuntimeException("JSON: Not boolean: " + o + " (" + index + ")");
	}

	public boolean getBoolean(int index, boolean def) {
		try {
			return getBoolean(index);
		} catch (Exception e) {
			return def;
		}
	}
	
	public boolean isNull(int index) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		return elements[index] == MahoRaspApp2.json_null;
	}
	
	public void add(Object object) {
		addElement(object);
	}
	
	public void add(String s) {
		addElement(s);
	}
	
	public void add(int i) {
		addElement(new Integer(i));
	}

	public void add(long l) {
		addElement(new Long(l));
	}

	public void add(double d) {
		addElement(new Double(d));
	}
	
	public void add(boolean b) {
		addElement(new Boolean(b));
	}

	public void set(int index, Object object) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		elements[index] = object;
	}
	
	public void set(int index, String s) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		elements[index] = s;
	}
	
	public void set(int index, int i) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		elements[index] = new Integer(i);
	}

	public void set(int index, long l) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		elements[index] = new Long(l);
	}

	public void set(int index, double d) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		elements[index] = new Double(d);
	}
	
	public void set(int index, boolean b) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		elements[index] = new Boolean(b);
	}
	
	public void put(int index, Object object) {
		insertElementAt(object, index);
	}
	
	public void put(int index, String s) {
		insertElementAt(s, index);
	}
	
	public void put(int index, int i) {
		insertElementAt(new Integer(i), index);
	}

	public void put(int index, long l) {
		insertElementAt(new Long(l), index);
	}

	public void put(int index, double d) {
		insertElementAt(new Double(d), index);
	}

	public void put(int index, boolean b) {
		insertElementAt(new Boolean(b), index);
	}
	
	public boolean has(Object object) {
		return _indexOf(object, 0) != -1;
	}
	
	public boolean has(int i) {
		return _indexOf(new Integer(i), 0) != -1;
	}

	public boolean has(long l) {
		return _indexOf(new Long(l), 0) != -1;
	}

	public boolean has(double d) {
		return _indexOf(new Double(d), 0) != -1;
	}
	
	public boolean has(boolean b) {
		return _indexOf(new Boolean(b), 0) != -1;
	}
	
	public int indexOf(Object object) {
		return _indexOf(object, 0);
	}

	public int indexOf(Object object, int index) {
		return _indexOf(object, index);
	}
	
	public boolean remove(Object object) {
		int i = _indexOf(object, 0);
		if (i == -1) return false;
		remove(i);
		return true;
	}
	
	public void remove(int index) {
		if (index < 0 || index >= count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		count--;
		int size = count - index;
		if (size > 0)
			System.arraycopy(elements, index + 1, elements, index, size);
		elements[count] = null;
	}
	
	public void copyInto(Object[] arr) {
		copyInto(arr, 0, arr.length);
	}

	public void copyInto(Object[] arr, int offset, int length) {
		int i = offset;
		int j = 0;
		while(i < arr.length && j < length && j < size()) {
			arr[i++] = get(j++);
		}
	}

	void addElement(Object object) {
		if (count == elements.length) grow();
		elements[count++] = object;
	}
	
	private void insertElementAt(Object object, int index) {
		if (index < 0 || index > count) {
			throw new RuntimeException("JSON: Index out of bounds: " + index);
		}
		if (count == elements.length) grow();
		int size = count - index;
		if (size > 0)
			System.arraycopy(elements, index, elements, index + 1, size);
		elements[index] = object;
		count++;
	}
	
	private int _indexOf(Object object, int start) {
		for (int i = start; i < count; i++) {
			if (elements[i] instanceof String[])
				elements[i] = MahoRaspApp2.parseJSON(((String[]) elements[i])[0]);
			if (object.equals(elements[i])) return i;
		}
		return -1;
	}
	
	private void grow() {
		Object[] tmp = new Object[elements.length * 2];
		System.arraycopy(elements, 0, tmp, 0, count);
		elements = tmp;
	}

	// transforms string for exporting
	private static String escape_utf8(String s) {
		int len = s.length();
		StringBuffer sb = new StringBuffer();
		int i = 0;
		while (i < len) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
			case '\\':
				sb.append("\\").append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c < 32 || c > 1103) {
					String u = Integer.toHexString(c);
					sb.append("\\u");
					for (int z = u.length(); z < 4; z++) {
						sb.append('0');
					}
					sb.append(u);
				} else {
					sb.append(c);
				}
			}
			i++;
		}
		return sb.toString();
	}

}
