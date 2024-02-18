import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Vector;

public class Search implements Runnable {
	
	MahoRaspApp2 app;
	InputStreamReader reader;
	boolean cancel;
	
	int loadingIdx;

	public void run() {
		// TODO поиск по алфавиту
		app.searching = true;
		InputStreamReader r = null;
		s: {
			try {
				String query = app.searchField.getString().toLowerCase().trim();
				app.searchChoice.deleteAll();
				app.searchIds.removeAllElements();
				app.searchChoice.setLabel("Поиск...");
				Vector items = new Vector();
				search: {
					if(query.length() < 3) break search;
					r = reader = app.openCitiesStream();
					if(reader.read() != '[')
						throw new Exception("Cities database is corrupted");
					while(!cancel) {
						reader.skip(2);
						String regionName = nextString();
						reader.skip(2);
						for(;;) {
							reader.skip(1);
							String code = nextString();
							reader.skip(2);
							String cityName = nextString();
							if(cityName.toLowerCase().startsWith(query)) {
								app.searchChoice.append(cityName + ", " + regionName, null);
								app.searchIds.addElement(code);
							} else if(regionName.toLowerCase().startsWith(query)) {
								items.addElement(new String[] {cityName, regionName, code});
							}
							if(reader.read() != ',') {
								break;
							}
						}
						reader.skip(1);
						if(reader.read() != ',') {
							break;
						}
					}
				}
				if(app.searchForm == null || cancel) break s;
				app.searchChoice.setLabel("");
				for(Enumeration e = items.elements(); e.hasMoreElements(); ) {
					if(app.searchChoice.size() > 10) break;
					String[] s = (String[]) e.nextElement();
					app.searchChoice.append(s[0] + ", " + s[1], null);
					app.searchIds.addElement(s[2]);
				}
				// замена функции "отмена" на "готово"
				if(app.searchChoice.getSelectedIndex() != -1) {
					if(!app.searchCancel) break s;
	//				searchForm.removeCommand(cancelCmd);
					app.searchForm.addCommand(MahoRaspApp2.doneCmd);
					app.searchCancel = false;
					break s;
				}
				if(app.searchCancel) break s;
				app.searchForm.removeCommand(MahoRaspApp2.doneCmd);
	//			searchForm.addCommand(cancelCmd);
				app.searchCancel = true;
			} catch (Exception e) {
				if(cancel) break s;
				e.printStackTrace();
				app.display(app.warningAlert(e.toString()), app.searchForm);
			}
		}
		if(r != null) {
			try {
				r.close();
			} catch (Exception e) {}
		}
		app.searching = false;
		cancel = false;
		synchronized(this) {
			notifyAll();
		}
	}
	
	String nextString() throws IOException {
		StringBuffer sb = new StringBuffer();
		char l = 0;
		while(true) {
			char c = (char) reader.read();
			if(c == '\\' && l != '\\') {
				l = c;
				continue;
			}
			if(c == 'u' && l == '\\') {
				char[] chars = new char[4];
				reader.read(chars);
				sb.append(l = (char) Integer.parseInt(new String(chars), 16));
				continue;
			}
			if(c == 0 || (l != '\\' && c == '"')) break;
			sb.append(c);
			l = c;
		}
		return sb.toString();
	}
	
}
