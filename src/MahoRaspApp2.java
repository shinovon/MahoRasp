import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.DateField;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class MahoRaspApp2 extends MIDlet implements CommandListener, ItemCommandListener, Runnable, ItemStateListener {

	// команды главной формы
	private static final Command exitCmd = new Command("Выход", Command.EXIT, 1);
	private static final Command backCmd = new Command("Назад", Command.BACK, 1);
	private static final Command bookmarksCmd = new Command("Закладки", Command.SCREEN, 3);
//	private static final Command settingsCmd = new Command("Настройки", Command.SCREEN, 4);
	private static final Command aboutCmd = new Command("О программе", Command.SCREEN, 5);
	
	private static final Command submitCmd = new Command("Искать", Command.ITEM, 2);
	private static final Command choosePointCmd = new Command("Выбрать", Command.ITEM, 1);

	// команды формы результатов
	private static final Command prevDayCmd = new Command("Пред. день", Command.SCREEN, 3);
	private static final Command nextDayCmd = new Command("След. день", Command.SCREEN, 4);
	private static final Command addBookmarkCmd = new Command("Добавить в закладки", Command.SCREEN, 5);
	
	private static final Command itemCmd = new Command("Подробнее", Command.ITEM, 2);

	// команды формы выбора
	private static final Command doneCmd = new Command("Готово", Command.OK, 1);
	private static final Command cancelCmd = new Command("Отмена", Command.CANCEL, 1);
//	private static final Command searchCmd = new Command("Поиск", Command.ITEM, 2);
	
	private static final Command removeBookmarkCmd = new Command("Удалить", Command.ITEM, 2);
	
	private static final int RUN_REQUEST = 1;
	private static final int RUN_ITEM_DETAILS = 2;
	private static final int RUN_SEARCH = 3;
	private static final int RUN_BOOKMARKS_SCREEN = 4;
	
	private static final int BOOKMARK_CITIES = 1;
	private static final int BOOKMARK_STATIONS = 2;
	private static final int BOOKMARK_ROUTE = 3;
	
	// апи ключ
	private static final String APIKEY = "20e7cb3e-6b05-4774-bcbb-4b0fb74a58b0";

	private static final String[] TRANSPORT_TYPES = new String[] {
		"plane", "train", "suburban", "bus", "water", "helicopter"
	};
	private static final String[] TRANSPORT_NAMES = new String[] {
		"Любой", "Самолет", "Поезд", "Электричка", "Автобус",
		//"Морской транспорт", "Вертолет"
	};
	
	// Константы названий RecordStore
//	private static final String SETTINGS_RECORDNAME = "mahoRsets";
	private static final String BOOKMARKS_RECORDNAME = "mahoRbm";

	public static MahoRaspApp2 midlet;
	
	private boolean started;
	
	// ui главной
	private Form mainForm;
	private ChoiceGroup transportChoice;
	private DateField dateField;
	private StringItem fromBtn;
	private StringItem toBtn;
	private StringItem submitBtn;
//	private ChoiceGroup showTransfers;
	
	private Form resultForm;

	private Image planeImg;
	private Image trainImg;
	private Image suburbanImg;
	private Image busImg;

	private int run;
	private boolean running;
	
	private String from;
	private String to;
	private String searchDate;
	private String searchParams;

	private JSONObject result;
	private Hashtable items;
	private int selectedItem;
	
	private Form searchForm;
	private TextField searchField;
	private ChoiceGroup searchChoice;
	
	private int choosing;
	private Vector searchIds = new Vector();
	private boolean searchCancel;
	private JSONStream citiesStream;
	
	private JSONArray bookmarks;

	public MahoRaspApp2() {
		items = new Hashtable();
		midlet = this;
		mainForm = new Form("MahoRasp");
		mainForm.addCommand(exitCmd);
		mainForm.addCommand(bookmarksCmd);
//		mainForm.addCommand(settingsCmd);
		mainForm.addCommand(aboutCmd);
		mainForm.setCommandListener(this);
		transportChoice = new ChoiceGroup("Тип транспорта", Choice.POPUP, TRANSPORT_NAMES, null);
		mainForm.append(transportChoice);
		dateField = new DateField("Дата", DateField.DATE);
		dateField.setDate(new Date(System.currentTimeMillis()));
		mainForm.append(dateField);
		fromBtn = new StringItem("Откуда", "Не выбрано", Item.BUTTON);
		fromBtn.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		fromBtn.addCommand(choosePointCmd);
		fromBtn.setDefaultCommand(choosePointCmd);
		fromBtn.setItemCommandListener(this);
		mainForm.append(fromBtn);
		toBtn = new StringItem("Куда", "Не выбрано", Item.BUTTON);
		toBtn.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		toBtn.addCommand(choosePointCmd);
		toBtn.setDefaultCommand(choosePointCmd);
		toBtn.setItemCommandListener(this);
		mainForm.append(toBtn);
//		showTransfers = new ChoiceGroup("", Choice.EXCLUSIVE, new String[] { "Показывать пересадки" }, null);
//		mainForm.append(showTransfers);
		submitBtn = new StringItem(null, "Найти", StringItem.BUTTON);
		submitBtn.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		submitBtn.addCommand(submitCmd);
		submitBtn.setDefaultCommand(submitCmd);
		submitBtn.setItemCommandListener(this);
		mainForm.append(submitBtn);
	}

	protected void destroyApp(boolean unconditional) {

	}

	protected void pauseApp() {

	}

	protected void startApp() {
		if(started) return;
		started = true;
		try {
			planeImg = Image.createImage("/plane.png");
			trainImg = Image.createImage("/train.png");
			suburbanImg = Image.createImage("/suburban.png");
			busImg = Image.createImage("/bus.png");
		} catch (Exception e) {
		}
		display(mainForm);
	}

	public void commandAction(Command c, Item i) {
		if(c == itemCmd) {
			display(loadingAlert("Загрузка"));
			selectedItem = ((Integer) items.get(i)).intValue();
			run(2);
			return;
		}
		if(c == choosePointCmd) {
			choosing = i == fromBtn ? 1 : 2;
			// TODO выбор станции
			searchForm = new Form("Выбор города");
			searchField = new TextField("Поиск", "", 100, TextField.ANY);
			searchField.setItemCommandListener(this);
			searchForm.append(searchField);
			// TODO показ ближайших городов по гпс
			searchChoice = new ChoiceGroup("", Choice.EXCLUSIVE);
			searchChoice.setFitPolicy(Choice.TEXT_WRAP_ON);
			searchForm.append(searchChoice);
			searchForm.addCommand(cancelCmd);
			searchForm.setCommandListener(this);
			searchForm.setItemStateListener(this);
			display(searchForm);
			return;
		}
		commandAction(c, mainForm);
	}

	public void commandAction(Command c, Displayable d) {
		if(c == exitCmd) {
			notifyDestroyed();
			return;
		}
		if(c == backCmd) {
			display(mainForm);
			return;
		}
		if(c == submitCmd) {
			if(running) return;
			if(from == null || to == null) {
				display(warningAlert("Не выбран один из пунктов"));
				return;
			}
			display(loadingAlert("Загрузка"));
			run(RUN_REQUEST);
			return;
		}
//		if(c == searchCmd) {
//			if(running) return;
//			run(RUN_SEARCH);
//			return;
//		}
		if(c == cancelCmd) {
			cancelChoice();
			return;
		}
		if(c == doneCmd) {
			int i = searchChoice.getSelectedIndex();
			if(i == -1) { // если список пустой то отмена
				cancelChoice();
				return;
			}
			select((String) searchIds.elementAt(i), searchChoice.getString(i));
			return;
		}
		if(c == aboutCmd) {
			Form f = new Form("О программе");
			f.addCommand(backCmd);
			f.setCommandListener(this);
			f.append(new StringItem("MahoRasp v" + this.getAppProperty("MIDlet-Version"),
					"Клиент Яндекс Расписаний.\n\nРазработал, дизайнил, придумал, украл апи ключ: shinovon\n\n292 labs"));
			display(f);
			return;
		}
		if(c == prevDayCmd) {
			if(running) return;
			Date date = dateField.getDate();
			date.setTime(date.getTime()-24*60*60*1000);
			dateField.setDate(date);
			display(loadingAlert("Загрузка"));
			run(RUN_REQUEST);
			return;
		}
		if(c == nextDayCmd) {
			if(running) return;
			Date date = dateField.getDate();
			date.setTime(date.getTime()+24*60*60*1000);
			dateField.setDate(date);
			display(loadingAlert("Загрузка"));
			run(RUN_REQUEST);
			return;
		}
		if(c == bookmarksCmd) {
			if(running) return;
			loadingAlert("Загрузка");
			run(RUN_BOOKMARKS_SCREEN);
			return;
		}
		if(c == addBookmarkCmd) {
			if(from == null || to == null) {
				display(warningAlert("Не выбран один из пунктов"));
				return;
			}
			String fn = fromBtn.getText();
			String tn = toBtn.getText();
			
			if(bookmarks == null) {
				try {
					RecordStore r = RecordStore.openRecordStore(BOOKMARKS_RECORDNAME, false);
					bookmarks = JSON.getArray(new String(r.getRecord(1), "UTF-8"));
					r.closeRecordStore();
				} catch (Exception e) {
					bookmarks = new JSONArray();
				}
			} else {
				// есть ли уже такая закладка
				int l = bookmarks.size();
				for(int i = 0; i < l; i++) {
					JSONObject j = bookmarks.getObject(i);
					if(j.getInt("t") == BOOKMARK_CITIES && fn.equals(j.getString("c")) && tn.equals(j.getString("d"))) return;  
				}
			}
			
			JSONObject bm = new JSONObject();
			bm.put("t", BOOKMARK_CITIES);
			bm.put("a", from);
			bm.put("b", to);
			bm.put("c", fn);
			bm.put("d", tn);
			bookmarks.add(bm);
			
			try {
				RecordStore.deleteRecordStore(BOOKMARKS_RECORDNAME);
			} catch (Exception e) {
			}
			try {
				RecordStore r = RecordStore.openRecordStore(BOOKMARKS_RECORDNAME, true);
				byte[] b = bookmarks.toString().getBytes("UTF-8");
				r.addRecord(b, 0, b.length);
				r.closeRecordStore();
			} catch (Exception e) {
			}
			display(infoAlert("Закладка добавлена"));
			return;
		}
		if(c == removeBookmarkCmd) {
			if(bookmarks == null) return;
			int idx;
			((List)d).delete(idx = ((List)d).getSelectedIndex());
			bookmarks.remove(idx);
			return;
		}
		if(c == List.SELECT_COMMAND) { // выбрана закладка
			if(bookmarks == null) return;
			JSONObject bm = bookmarks.getObject(((List)d).getSelectedIndex());
			switch(bm.getInt("t")) {
			case BOOKMARK_CITIES:
			case BOOKMARK_STATIONS:
				from = bm.getString("a");
				to = bm.getString("b");
				fromBtn.setText(bm.getString("c"));
				toBtn.setText(bm.getString("d"));
				break;
			case BOOKMARK_ROUTE:
				// TODO
				break;
			}
			display(mainForm);
			commandAction(submitCmd, d);
			return;
		}
	}

	public void itemStateChanged(Item item) {
		if(item == searchField) { // выполнять поиск при изменениях в поле ввода
			if(running) return;
			run(RUN_SEARCH);
		}
	}
	
	public void run() {
		running = true;
		switch(run) {
		case RUN_REQUEST: { // запрос
			items.clear();
			resultForm = new Form("Результаты поиска");
			resultForm.addCommand(backCmd);
			resultForm.addCommand(prevDayCmd);
			resultForm.addCommand(nextDayCmd);
			resultForm.addCommand(addBookmarkCmd);
			resultForm.setCommandListener(this);
			// TODO скрытие уехавших
			try {
				Calendar cal = Calendar.getInstance();
				cal.setTime(dateField.getDate());
				searchDate = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
				searchParams = "from=" + from + "&to=" + to;
				int transport = transportChoice.getSelectedIndex();
				result = api("search/?date=" + searchDate + "&" + searchParams + "&transfers=true" + (transport > 0 ? ("&transport_types=" + TRANSPORT_TYPES[transport - 1]) : ""));
				
				JSONObject search = result.getObject("search");
				StringItem titleItem = new StringItem(searchDate, search.getObject("from").getString("title") + " - " + search.getObject("to").getString("title") + "\n");
				titleItem.setLayout(Item.LAYOUT_CENTER);
				resultForm.append(titleItem);
				
				StringItem left = new StringItem(null, "");
				left.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
				resultForm.append(left);
				
				JSONArray segments = result.getArray("segments");
				int size = segments.size();
				if(size == 0) {
					left.setText("Пусто!");
				}
				for(int i = 0; i < size; i++) {
					JSONObject seg = segments.getObject(i);
					String r = "";
					if(seg.getBoolean("has_transfers")) {
						JSONArray types = seg.getArray("transport_types");
						for(int j = 0; j < types.size(); j++) {
							resultForm.append(new ImageItem(null, transportImg(types.getString(j)), Item.LAYOUT_LEFT, null));
						}
						JSONObject from = seg.getObject("departure_from");
						JSONObject to = seg.getObject("arrival_to");
						r += from.getString("title") + " - " + to.getString("title") + "\n";
						r += "с пересадками\n";
					} else {
						JSONObject thread = seg.getObject("thread");
						r += thread.getString("number") + " " + thread.getString("title") + "\n";
						resultForm.append(new ImageItem(null, transportImg(thread.getString("transport_type")), Item.LAYOUT_LEFT, null));
					}
					Calendar departure = parseDate(seg.getString("departure"));
					Calendar arrival = parseDate(seg.getString("arrival"));
					r += (oneDay(cal, departure) ? time(departure) : shortDate(departure) + " " + time(departure));
					r += " - " + (oneDay(cal, arrival) ? time(arrival) : shortDate(arrival) + " " + time(arrival)) + "\n";
					StringItem s = new StringItem("", r + "\n");
					s.addCommand(itemCmd);
					s.setDefaultCommand(itemCmd);
					s.setItemCommandListener(this);
					s.setLayout(Item.LAYOUT_LEFT);
					items.put(s, new Integer(i));
					resultForm.append(s);
				}
			} catch (Exception e) {
				resultForm.append(e.toString());
				e.printStackTrace();
			}
			display(resultForm);
			break;
		}
		case RUN_ITEM_DETAILS: { // показ подробнее
			Form f = new Form("");
			f.addCommand(backCmd);
			f.setCommandListener(this);
			//TODO
			JSONObject j = result.getArray("segments").getObject(selectedItem);
			
			f.append(j.format());
			
			display(f);
			break;
		}
		case RUN_SEARCH: { // поиск точки
			JSONStream stream = null;
			try {
				String query = searchField.getString().toLowerCase().trim();
				searchChoice.deleteAll();
				searchIds.removeAllElements();
				search: {
					if(query.length() < 3) break search;
					stream = getCitiesStream();
					stream.assertNext('{');
					while(true) {
						stream.skipString();
						stream.assertNext(':');
						stream.assertNext('[');
						/*String countryName = */stream.nextString();
						stream.assertNext(',');
						while(true) {
							stream.assertNext('[');
							String regionName = stream.nextString();
							stream.assertNext(',');
							stream.assertNext('{');
							while(true) {
								String code = stream.nextString();
								stream.assertNext(':');
								String cityName = stream.nextString();
								if(cityName.toLowerCase().startsWith(query) || regionName.toLowerCase().startsWith(query)) {
									searchChoice.append(cityName + ", " + regionName/* + ", " + countryName*/, null);
									searchIds.addElement(code);
									if(searchChoice.size() > 10) break search;
								}
								if(stream.next() != ',') {
									break;
								}
							}
							stream.assertNext(']');
							if(stream.next() != ',') {
								stream.back();
								break;
							}
						}
						stream.assertNext(']');
						if(stream.next() != ',') {
							break;
						}
					}
				}
				// замена функции "отмена" на "готово"
				if(searchChoice.getSelectedIndex() != -1) {
					if(!searchCancel) break;
//					searchForm.removeCommand(cancelCmd);
					searchForm.addCommand(doneCmd);
					searchCancel = false;
					break;
				}
				if(searchCancel) break;
				searchForm.removeCommand(doneCmd);
//				searchForm.addCommand(cancelCmd);
				searchCancel = true;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(stream != null)
					try {
						stream.close();
					} catch (IOException e) {
					}
			}
			break;
		}
		case RUN_BOOKMARKS_SCREEN: {
			List l = new List("Закладки", List.IMPLICIT);
			l.setFitPolicy(Choice.TEXT_WRAP_ON);
			l.addCommand(backCmd);
			l.addCommand(List.SELECT_COMMAND);
			l.setCommandListener(this);
			try {
				if(bookmarks == null) {
					RecordStore r = RecordStore.openRecordStore(BOOKMARKS_RECORDNAME, false);
					bookmarks = JSON.getArray(new String(r.getRecord(1), "UTF-8"));
					r.closeRecordStore();
				}
				l.addCommand(removeBookmarkCmd);
				for(Enumeration e = bookmarks.elements(); e.hasMoreElements();) {
					JSONObject bm = (JSONObject) e.nextElement();
					l.append(bm.getString("c") + " - " + bm.getString("d"), null);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			display(l);
			break;
		}
		}
		running = false;
		run = 0;
	}

	private void run(int run) {
		this.run = run;
		new Thread(this).start();
	}
	
	private void select(String code, String title) {
		searchForm = null;
		searchField = null;
		searchChoice = null;
		if(choosing == 1) {
			from = code;
			fromBtn.setText(title);
		} else {
			to = code;
			toBtn.setText(title);
		}
		display(mainForm);
	}
	
	private void cancelChoice() {
		display(mainForm);
		searchForm = null;
		searchField = null;
		searchChoice = null;
	}
	
	private JSONStream getCitiesStream() throws IOException {
		if(citiesStream != null) {
			citiesStream.close();
		}
		citiesStream = new JSONStream(getClass().getResourceAsStream("/cities.json"));
		return citiesStream;
	}
	
	private Image transportImg(String transport) {
		if("plane".equals(transport)) {
			return planeImg;
		}
		if("train".equals(transport)) {
			return trainImg;
		}
		if("suburban".equals(transport)) {
			return suburbanImg;
		}
		if("bus".equals(transport)) {
			return busImg;
		}
		throw new RuntimeException("Unknown transport_type: " + transport);
	}
	
	private void display(Displayable d) {
		if(d instanceof Alert) {
			Display.getDisplay(this).setCurrent((Alert) d, mainForm);
			return;
		}
		Display.getDisplay(this).setCurrent(d);
	}

	private Alert loadingAlert(String text) {
		Alert a = new Alert("");
		a.setString(text);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(30000);
		return a;
	}
	
	private Alert warningAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.ERROR);
		a.setString(text);
		a.setTimeout(2000);
		return a;
	}
	
	private Alert infoAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.INFO);
		a.setString(text);
		a.setTimeout(2000);
		return a;
	}
	
	static String n(int n) {
		if(n < 10) {
			return "0".concat(i(n));
		} else return i(n);
	}
	
	static String i(int n) {
		return Integer.toString(n);
	}
	
	// парсер даты ISO 8601 без учета часового пояса
	static Calendar parseDate(String date) {
		Calendar c = Calendar.getInstance();
		if(date.indexOf('T') != -1) {
			String[] dateSplit = split(date.substring(0, date.indexOf('T')), '-');
			String[] timeSplit = split(date.substring(date.indexOf('T')+1), ':');
			String second = split(timeSplit[2], '.')[0];
			int i = second.indexOf('+');
			if(i == -1) {
				i = second.indexOf('-');
			}
			if(i != -1) {
				second = second.substring(0, i);
			}
			c.set(Calendar.YEAR, Integer.parseInt(dateSplit[0]));
			c.set(Calendar.MONTH, Integer.parseInt(dateSplit[1])-1);
			c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateSplit[2]));
			c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeSplit[0]));
			c.set(Calendar.MINUTE, Integer.parseInt(timeSplit[1]));
			c.set(Calendar.SECOND, Integer.parseInt(second));
		} else {
			String[] dateSplit = split(date, '-');
			c.set(Calendar.YEAR, Integer.parseInt(dateSplit[0]));
			c.set(Calendar.MONTH, Integer.parseInt(dateSplit[1])-1);
			c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateSplit[2]));
		}
		return c;
	}

	static String shortDate(Calendar c) {
		if(c == null) return "";
		return c.get(Calendar.DAY_OF_MONTH) + " " + localizeMonthWithCase(c.get(Calendar.MONTH));
	}
	
	static String time(Calendar c) {
		if(c == null) return "";
		//return c.get(Calendar.DAY_OF_MONTH) + " " + localizeMonthWithCase(c.get(Calendar.MONTH)) + " " +
		return n(c.get(Calendar.HOUR_OF_DAY)) + ":" + n(c.get(Calendar.MINUTE));
	}
	
	static long timestamp(String date) {
		return parseDate(date).getTime().getTime();
	}
	
	static boolean oneDay(Calendar a, Calendar b) {
		return a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH) &&
				a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
				a.get(Calendar.YEAR) == b.get(Calendar.YEAR);
	}
	
	static String localizeMonthWithCase(int month) {
		switch(month) {
		case Calendar.JANUARY:
			return "января";
		case Calendar.FEBRUARY:
			return "февраля";
		case Calendar.MARCH:
			return "марта";
		case Calendar.APRIL:
			return "апреля";
		case Calendar.MAY:
			return "мая";
		case Calendar.JUNE:
			return "июня";
		case Calendar.JULY:
			return "июля";
		case Calendar.AUGUST:
			return "августа";
		case Calendar.SEPTEMBER:
			return "сентября";
		case Calendar.OCTOBER:
			return "октября";
		case Calendar.NOVEMBER:
			return "ноября";
		case Calendar.DECEMBER:
			return "декабря";
		default:
			return "";
		}
	}
	
	static String duration(int minutes) {
		if(minutes > 24 * 60) {
			int hours = minutes / 60;
			return (hours / 24) + "д " + (hours % 24) + " ч";
		}
		if(minutes > 60) {
			return (minutes / 60) + " ч " + (minutes % 60) + " мин";
		}
		return minutes + " мин";
	}
	
	static String[] split(String str, char d) {
		int i = str.indexOf(d);
		if(i == -1)
			return new String[] {str};
		Vector v = new Vector();
		v.addElement(str.substring(0, i));
		while(i != -1) {
			str = str.substring(i + 1);
			if((i = str.indexOf(d)) != -1)
				v.addElement(str.substring(0, i));
			i = str.indexOf(d);
		}
		v.addElement(str);
		String[] r = new String[v.size()];
		v.copyInto(r);
		return r;
	}
	
	static String url(String url) {
		StringBuffer sb = new StringBuffer();
		char[] chars = url.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int c = chars[i];
			if (65 <= c && c <= 90) {
				sb.append((char) c);
			} else if (97 <= c && c <= 122) {
				sb.append((char) c);
			} else if (48 <= c && c <= 57) {
				sb.append((char) c);
			} else if (c == 32) {
				sb.append("%20");
			} else if (c == 45 || c == 95 || c == 46 || c == 33 || c == 126 || c == 42 || c == 39 || c == 40
					|| c == 41) {
				sb.append((char) c);
			} else if (c <= 127) {
				sb.append(hex(c));
			} else if (c <= 2047) {
				sb.append(hex(0xC0 | c >> 6));
				sb.append(hex(0x80 | c & 0x3F));
			} else {
				sb.append(hex(0xE0 | c >> 12));
				sb.append(hex(0x80 | c >> 6 & 0x3F));
				sb.append(hex(0x80 | c & 0x3F));
			}
		}
		return sb.toString();
	}

	private static String hex(int i) {
		String s = Integer.toHexString(i);
		return "%".concat(s.length() < 2 ? "0" : "").concat(s);
	}
	
	static byte[] get(String url) throws IOException {
		ByteArrayOutputStream o = null;
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = (HttpConnection) Connector.open(url);
			hc.setRequestMethod("GET");
			hc.getResponseCode();
			in = hc.openInputStream();
			int read;
			o = new ByteArrayOutputStream();
			byte[] b = new byte[512];
			while((read = in.read(b)) != -1) {
				o.write(b, 0, read);
			}
			return o.toByteArray();
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {
			}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {
			}
			try {
				if (o != null) o.close();
			} catch (IOException e) {
			}
		}
	}
	
	static String getUtf(String url) throws IOException {
		return new String(get(url), "UTF-8");
	}
	
	static JSONObject api(String url) throws Exception {
		String r = getUtf("http://api.rasp.yandex.net/v3.0/" + url + (!url.endsWith("?") ? "&" : "") + "apikey=" + APIKEY + "&format=json&lang=ru_RU");
		JSONObject j = JSON.getObject(r);
		if(j.has("error")) {
			// выбрасывать эксепшн с текстом ошибки
			throw new Exception(j.getObject("error").getString("text"));
		}
		return j;
	}

}
