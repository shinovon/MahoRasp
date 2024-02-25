import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import javax.microedition.lcdui.Font;
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
	private static final Command addBookmarkCmd = new Command("Добавить в закладки", Command.SCREEN, 3);
	private static final Command prevDayCmd = new Command("Пред. день", Command.SCREEN, 4);
	private static final Command nextDayCmd = new Command("След. день", Command.SCREEN, 5);
	private static final Command showGoneCmd = new Command("Показать ушедшие", Command.ITEM, 2);
	private static final Command itemCmd = new Command("Подробнее", Command.ITEM, 2);

	// команды формы выбора
	static final Command doneCmd = new Command("Готово", Command.OK, 1);
	private static final Command cancelCmd = new Command("Отмена", Command.CANCEL, 1);
	private static final Command gpsCmd = new Command("Ближайший город", Command.ITEM, 2);
//	private static final Command searchCmd = new Command("Поиск", Command.ITEM, 2);

	private static final Command removeBookmarkCmd = new Command("Удалить", Command.ITEM, 2);
	
	private static final Command hyperlinkCmd = new Command("Открыть", Command.ITEM, 2);
	
	private static final Font smallfont = Font.getFont(0, 0, Font.SIZE_SMALL);
	
	private static final int RUN_REQUEST = 1;
	private static final int RUN_ITEM_DETAILS = 2;
	private static final int RUN_SEARCH = 3;
	private static final int RUN_BOOKMARKS_SCREEN = 4;
	private static final int RUN_UPDATE_RESULT = 5;
	private static final int RUN_NEAREST_CITY = 6;
	
	private static final int BOOKMARK_CITIES = 1;
//	private static final int BOOKMARK_STATIONS = 2;
//	private static final int BOOKMARK_SEGMENT = 3;
	
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
	private Display display;
	
	private boolean started;
	
	// ui главной
	private Form mainForm;
	private ChoiceGroup transportChoice;
	private DateField dateField;
	private StringItem fromBtn;
	private StringItem toBtn;
	private StringItem submitBtn;
	private ChoiceGroup showTransfers;

//	private Form settingsForm;
//	private ChoiceGroup proxyChoice;
	
	private Form resultForm;

	private Image planeImg;
	private Image trainImg;
	private Image suburbanImg;
	private Image busImg;

	private int run;
	private boolean running;
	
	private String from;
	private String to;

	private JSONObject result;
	private Hashtable items;
	private int selectedItem;
	private String resultTitle;
	private boolean showGone;
	
	Form searchForm;
	TextField searchField;
	ChoiceGroup searchChoice;

	private int choosing;
	Vector searchIds;
	boolean searchCancel;
	private InputStream stream;
	private Search search;
	boolean searching;
	
	private JSONArray bookmarks;

	private Alert gpsDialog;
	private boolean gpsActive;
	static double gpslat;
	static double gpslon;
	
	// настройки
//	private static boolean proxy;

	public MahoRaspApp2() {
		midlet = this;
		items = new Hashtable();
		searchIds = new Vector();
		mainForm = new Form("MahoRasp");
		mainForm.addCommand(exitCmd);
		mainForm.addCommand(bookmarksCmd);
//		mainForm.addCommand(settingsCmd);
		mainForm.addCommand(aboutCmd);
		mainForm.setCommandListener(this);
		transportChoice = new ChoiceGroup("Тип транспорта", Choice.POPUP, TRANSPORT_NAMES, null);
		mainForm.append(transportChoice);
		dateField = new DateField("Дата", DateField.DATE);
		dateField.setDate(new Date());
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
		showTransfers = new ChoiceGroup("", Choice.MULTIPLE, new String[] { "С пересадками" }, null);
		mainForm.append(showTransfers);
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
		display = Display.getDisplay(this);
		try {
			planeImg = Image.createImage("/plane.png");
			trainImg = Image.createImage("/train.png");
			suburbanImg = Image.createImage("/suburban.png");
			busImg = Image.createImage("/bus.png");
		} catch (Exception e) {
		}
//		try {
//			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, false);
//			JSONObject j = JSON.getObject(new String(r.getRecord(1), "UTF-8"));
//			r.closeRecordStore();
//			proxy = j.getBoolean("proxy", proxy);
//		} catch (Exception e) {
//		}
		display(mainForm);
	}

	public void commandAction(Command c, Item i) {
		if(c == itemCmd) {
			display(loadingAlert("Загрузка"), null);
			selectedItem = ((Integer) items.get(i)).intValue();
			run(2);
			return;
		}
		if(c == choosePointCmd) {
			choosing = i == fromBtn ? 1 : 2;
			// TODO выбор станции
			searchForm = new Form("Выбор города");
			searchCancel = true;
			searchField = new TextField("Поиск", "", 100, TextField.ANY);
			searchField.setItemCommandListener(this);
			searchForm.append(searchField);
			StringItem gpsBtn = new StringItem(null, "Ближайший город", Item.BUTTON);
			gpsBtn.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			gpsBtn.addCommand(gpsCmd);
			gpsBtn.setDefaultCommand(gpsCmd);
			gpsBtn.setItemCommandListener(this);
			searchForm.append(gpsBtn);
			searchChoice = new ChoiceGroup("", Choice.EXCLUSIVE);
			searchChoice.setFitPolicy(Choice.TEXT_WRAP_ON);
			searchForm.append(searchChoice);
			searchForm.addCommand(cancelCmd);
			searchForm.setCommandListener(this);
			searchForm.setItemStateListener(this);
			display(searchForm);
			return;
		}
		if(c == showGoneCmd) {
			if(running) return;
			showGone = true;
			display(loadingAlert("Загрузка"));
			run(RUN_UPDATE_RESULT);
			return;
		}
		if(c == hyperlinkCmd) {
			try {
				if(platformRequest("http://" + ((StringItem) i).getText()))
					notifyDestroyed();
			} catch (Exception e) {}
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
//			if(settingsForm == d) {
//				proxy = proxyChoice.isSelected(0);
//				try {
//					RecordStore.deleteRecordStore(SETTINGS_RECORDNAME);
//				} catch (Exception e) {
//				}
//				try {
//					JSONObject j = new JSONObject();
//					j.put("proxy", proxy);
//					byte[] b = j.toString().getBytes("UTF-8");
//					RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, true);
//					r.addRecord(b, 0, b.length);
//					r.closeRecordStore();
//				} catch (Exception e) {
//				}
//			} else
			if(resultForm == d) {
				items.clear();
				resultForm = null;
			} else if(resultForm != null && d instanceof Form) {
				display(resultForm);
				return;
			}
			display(mainForm);
			return;
		}
		if(c == submitCmd) {
			if(running) return;
			if(from == null || to == null) {
				display(warningAlert("Не выбран один из пунктов"), null);
				return;
			}
			showGone = false;
			display(loadingAlert("Загрузка"));
			run(RUN_REQUEST);
			return;
		}
//		if(c == settingsCmd) {
//			settingsForm = new Form("Настройки");
//			settingsForm.addCommand(backCmd);
//			settingsForm.setCommandListener(this);
//			proxyChoice = new ChoiceGroup(null, Choice.MULTIPLE, new String[] { "Проксировать запросы" }, null);
//			proxyChoice.setSelectedIndex(0, proxy);
//			settingsForm.append(proxyChoice);
//			display(settingsForm);
//			return;
//		}
		if(c == gpsCmd) {
			if(gpsActive) return;
			gpsActive = true;
			gpsDialog = new Alert("", "Ожидание геолокации", null, null);
			gpsDialog.setTimeout(Alert.FOREVER);
			gpsDialog.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
			gpsDialog.addCommand(cancelCmd);
			gpsDialog.setCommandListener(this);
			display(gpsDialog, searchForm);
			try {
				Class.forName("javax.microedition.location.LocationProvider");
				new Thread(new GPS()).start();
			} catch (Throwable e) {
				display(warningAlert(e.toString()), searchForm);
			}
			return;
		}
		if(c == cancelCmd) {
			if(d == gpsDialog) {
				gpsActive = false;
				try {
					GPS.reset();
				} catch (Throwable e) {}
				display(searchForm);
				return;
			}
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
			StringItem s;
			try {
				f.append(new ImageItem(null, Image.createImage("/icon.png"), Item.LAYOUT_LEFT, null));
				s = new StringItem(null, "MahoRasp v" + this.getAppProperty("MIDlet-Version"));
				s.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_VCENTER);
				f.append(s);
			} catch (IOException e) {
			}
			s = new StringItem(null, "J2ME клиент Яндекс.Расписаний\n\n");
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			s = new StringItem("Разработал", "shinovon");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			s = new StringItem("Помогали", "sym_ansel\nMuseCat");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			s = new StringItem("Сайт", "nnp.nnchan.ru", Item.HYPERLINK);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(hyperlinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			s = new StringItem("Чат", "t.me/nnmidletschat", Item.HYPERLINK);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(hyperlinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			s = new StringItem(null, "\n292 labs");
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
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
			display(loadingAlert("Загрузка"));
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
			if(resultTitle != null) bm.put("n", resultTitle);
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
			display(infoAlert("Закладка добавлена"), null);
			return;
		}
		if(c == removeBookmarkCmd) {
			if(bookmarks == null) return;
			int idx;
			((List)d).delete(idx = ((List)d).getSelectedIndex());
			bookmarks.remove(idx);
			display(infoAlert("Закладка удалена"), null);
			return;
		}
		if(c == List.SELECT_COMMAND) { // выбрана закладка
			if(bookmarks == null) return;
			JSONObject bm = bookmarks.getObject(((List)d).getSelectedIndex());
			switch(bm.getInt("t")) {
			case BOOKMARK_CITIES:
//			case BOOKMARK_STATIONS:
				from = bm.getString("a");
				to = bm.getString("b");
				fromBtn.setText(bm.getString("c"));
				toBtn.setText(bm.getString("d"));
				break;
//			case BOOKMARK_SEGMENT:
//				break;
			}
			display(mainForm);
//			commandAction(submitCmd, d);
			return;
		}
	}

	public void itemStateChanged(Item item) {
		if(item == searchField) { // выполнять поиск при изменениях в поле ввода
			if(running) return;
			run(RUN_SEARCH);
		}
		if(item == searchChoice) {
			if(searchChoice.getSelectedIndex() != -1) {
				if(!searchCancel) return;
				searchForm.addCommand(MahoRaspApp2.doneCmd);
				searchCancel = false;
				return;
			}
			if(searchCancel) return;
			searchForm.removeCommand(MahoRaspApp2.doneCmd);
			searchCancel = true;
			return;
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
			try {
				Calendar cal = Calendar.getInstance();
				cal.setTime(dateField.getDate());
				String searchDate = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
				String searchParams = "from=" + from + "&to=" + to;
				int transport = transportChoice.getSelectedIndex();
				result = api("search/?date=" +
						searchDate + "&" +
						searchParams +
						(showTransfers.isSelected(0) ? "&transfers=true" : "") +
						(transport > 0 ? ("&transport_types=" + TRANSPORT_TYPES[transport - 1]) : "")
						);
				parseResults();
			} catch (Exception e) {
				resultForm.append(e.toString());
			}
			display(resultForm);
			break;
		}
		case RUN_ITEM_DETAILS: { // показ подробнее
			Form f = new Form("");
			f.addCommand(backCmd);
			f.setCommandListener(this);
			JSONObject seg = result.getArray("segments").getObject(selectedItem);
			
			if(seg.getBoolean("has_transfers")) {
				JSONArray types = seg.getArray("transport_types");
				String title = seg.getObject("departure_from").getString("title") + " - " + seg.getObject("arrival_to").getString("title");
				f.setTitle(title);
				StringItem t = new StringItem(null, title + "\n");
				t.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
				f.append(t);
				if(seg.has("details")) {
					JSONArray details = seg.getArray("details");
					int i = 0;
					for(Enumeration e = details.elements(); e.hasMoreElements(); ) {
						JSONObject n = (JSONObject) e.nextElement();
						if(n.has("thread")) {
							JSONObject th = n.getObject("thread");
							f.append(new ImageItem(null, transportImg(types.getString(i)), Item.LAYOUT_LEFT, null));
							StringItem s2 = new StringItem(null, th.getString("number") + " " + th.getString("title") + "\n\n");
							s2.setFont(Font.getFont(0, 0, Font.SIZE_MEDIUM));
							f.append(s2);
							String m;
							String d = "";
							if(n.has("departure_platform") && (m = n.getString("departure_platform")) != null) {
								d = "Платформа: " + m + "\n";
							}
							if(n.has("departure_terminal") && (m = n.getString("departure_terminal")) != null) {
								d = "Терминал: " + m + "\n";
							}
							Calendar departure = parseDate(seg.getString("departure"));
							Calendar arrival = parseDate(seg.getString("arrival"));
							StringItem s = new StringItem(null,
									"Отправление: " + point(n.getObject("from")) + "\n" +
									shortDate(departure) + " " + time(departure) + "\n" + d + "\n" +
									"Прибытие: " + point(n.getObject("to")) + "\n" +
									shortDate(arrival) + " " + time(arrival) + "\n" +
									(n.has("duration") ? (duration(n.getInt("duration")) + "\n") : "") + "\n"
									);
							s.setFont(smallfont);
							f.append(s);
							i++;
						} else if(n.getBoolean("is_transfer", false)) {
							StringItem s2 = new StringItem(null,
									"Пересадка в " + point(n.getObject("transfer_point")) + "\n"
									);
							s2.setFont(Font.getFont(0, 0, Font.SIZE_MEDIUM));
							f.append(s2);
							StringItem s = new StringItem(null,
											"c: " + point(n.getObject("transfer_from")) + "\n" +
											"на: " + point(n.getObject("transfer_to")) + "\n\n"
									);
							s.setFont(smallfont);
							f.append(s);
						}
					}
				}
			} else {
				JSONObject thread = seg.getObject("thread");
				f.append(new ImageItem(null, transportImg(thread.getString("transport_type")), Item.LAYOUT_LEFT, null));
				String title = thread.getString("number") + " " + thread.getString("title");
				f.setTitle(title);
				StringItem t = new StringItem(null, title + "\n");
				t.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
				f.append(t);
				String m;
				String d = "";
				if((m = seg.getNullableString("departure_platform")) != null && m.length() > 0) {
					d = "Платформа: " + m + "\n";
				}
				if((m = seg.getNullableString("departure_terminal")) != null && m.length() > 0) {
					d = "Терминал: " + m + "\n";
				}
				Calendar departure = parseDate(seg.getString("departure"));
				Calendar arrival = parseDate(seg.getString("arrival"));
				StringItem s = new StringItem(null,
						"Отправление:\n" + point(seg.getObject("from")) + "\n" +
						shortDate(departure) + " " + time(departure) + "\n" + d + "\n" +
						"Прибытие:\n" + point(seg.getObject("to")) + "\n" +
						shortDate(arrival) + " " + time(arrival) + "\n" +
						(seg.has("duration") ? (duration(seg.getInt("duration")) + "\n") : "") + "\n"
						);
				s.setFont(smallfont);
				f.append(s);
				if((m = seg.getNullableString("stops")) != null && m.length() > 0) {
					s = new StringItem(null, "Остановки: " + m + "\n\n");
					s.setFont(smallfont);
					f.append(s);
				}
				JSONObject o;
				if((o = seg.getNullableObject("tickets_info")) != null) {
					String r = "Цена:\n";
					JSONArray places = o.getArray("places");
					if(places.size() > 0) {
						for(Enumeration e = places.elements(); e.hasMoreElements();) {
							JSONObject p = (JSONObject) e.nextElement();
							String name = p.getString("name");
							JSONObject price = p.getObject("price");
							if(name != null) {
								r += name + ": ";
							}
							r += price.getInt("whole") + "." + price.getInt("cents") + " " + p.getString("currency") + "\n";
						}
						s = new StringItem(null, r + "\n");
						s.setFont(smallfont);
						f.append(s);
					}
				}
				if((m = thread.getNullableString("vehicle")) != null && m.length() > 0) {
					s = new StringItem(null, m + "\n");
					s.setFont(smallfont);
					f.append(s);
				}
				if((o = thread.getNullableObject("transport_subtype")) != null) {
					m = o.getNullableString("title");
					if(m != null && m.length() > 0) {
						s = new StringItem(null, m + "\n");
						s.setFont(smallfont);
						f.append(s);
					}
				}
				if((o = thread.getNullableObject("carrier")) != null) {
					s = new StringItem(null, o.getString("title") + "\n");
					s.setFont(smallfont);
					f.append(s);
				}
			}
			
			display(f);
			break;
		}
		case RUN_SEARCH: {
			if(search == null) {
				search = new Search();
				search.app = this;
			} else if(searching) {
				search.reader = null;
				search.cancel = true;
				try {
					synchronized(search) {
						search.wait();
					}
				} catch (Exception e) {}
			}
			new Thread(search).start();
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
					l.append(bm.has("n") ? bm.getString("n") : bm.getString("c") + " - " + bm.getString("d"), null);
				}
			} catch (Exception e) {
			}
			display(l);
			break;
		}
		case RUN_UPDATE_RESULT: {
			if(result == null) break;
			items.clear();
			resultForm.deleteAll();
			parseResults();
			display(resultForm);
			break;
		}
		case RUN_NEAREST_CITY: {
			display(loadingAlert("Загрузка"), searchForm);
			try {
				JSONObject r = api("nearest_settlement/?lat=" + gpslat + "&lng=" + gpslon);
				if(r.has("title") && r.has("code")) {
					select(r.getString("code"), r.getString("title"));
					break;
				}
				display(warningAlert("В пределах 10 км не найдено ни одного населенного пункта"), searchForm);
			} catch (Exception e) {
				display(warningAlert(e.toString()), searchForm);
			}
			break;
		}
		}
		running = false;
		run = 0;
	}

	private String point(JSONObject o) {
		if("station".equals(o.getString("type"))) {
			return o.getString("title") + " (" + o.getString("station_type_name") + ")";
		}
		return o.getString("title");
	}

	private void parseResults() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateField.getDate());
		Calendar now = Calendar.getInstance();
		now.setTime(new Date());
		JSONObject search = result.getObject("search");
		String title = search.getObject("from").getString("title") + " - " + search.getObject("to").getString("title");
		resultTitle = title;
		StringItem titleItem = new StringItem(n(cal.get(Calendar.DAY_OF_MONTH)) + "." + n(cal.get(Calendar.MONTH) + 1) + "." + cal.get(Calendar.YEAR), title + "\n");
		titleItem.setLayout(Item.LAYOUT_CENTER);
		resultForm.append(titleItem);
		
		StringItem left = new StringItem(null, "");
		left.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		int idx = resultForm.append(left);
		
		JSONArray segments = result.getArray("segments");
		int size = segments.size();
		if(size == 0) {
			left.setText("Пусто!");
		}
		int goneCount = 0;
		for(int i = 0; i < size; i++) {
			JSONObject seg = segments.getObject(i);
			Calendar departure = parseDate(seg.getString("departure"));
			// TODO: учитывать таймзоны
			if(!showGone && oneDay(now, departure) && departure.before(now)) {
				goneCount++;
				continue;
			}
			Calendar arrival = parseDate(seg.getString("arrival"));
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
		
		if(goneCount > 0) {
			StringItem showGoneBtn = new StringItem(null, "Показать ушедшие (" + goneCount + ")", Item.BUTTON);
			showGoneBtn.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			showGoneBtn.addCommand(showGoneCmd);
			showGoneBtn.setDefaultCommand(showGoneCmd);
			showGoneBtn.setItemCommandListener(this);
			resultForm.insert(idx, showGoneBtn);
		}
	}

	void gpsDone() {
		if(!gpsActive) return;
		gpsActive = false;
		if(gpslat == 0 && gpslon == 0) {
			// поймать ванну не удалось
			display(warningAlert("Не удалось получить геолокацию"), searchForm);
			return;
		}
		run(RUN_NEAREST_CITY);
//		display(searchForm);
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
		searchIds.removeAllElements();
		display(mainForm);
	}
	
	private void cancelChoice() {
		display(mainForm);
		if(search != null) {
			search.reader = null;
			search.cancel = true;
		}
		searchForm = null;
		searchField = null;
		searchChoice = null;
		searchIds.removeAllElements();
	}
	
	InputStreamReader openCitiesStream() throws IOException {
		if(stream != null) {
			try {
				stream.reset();
				return new InputStreamReader(new FilterStream(stream), "UTF-8");
			} catch (Exception e) {
				try {
					stream.close();
				} catch (IOException e2) {}
				stream = getClass().getResourceAsStream("/cities");
				return new InputStreamReader(new FilterStream(stream), "UTF-8");
			}
		}
		stream = getClass().getResourceAsStream("/cities");
		return new InputStreamReader(new FilterStream(stream), "UTF-8");
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
	
	void display(Alert a, Displayable d) {
		if(d == null) {
			display.setCurrent(a);
			return;
		}
		display.setCurrent(a, d);
	}
	
	private void display(Displayable d) {
		if(d instanceof Alert) {
			display.setCurrent((Alert) d, mainForm);
			return;
		}
		display.setCurrent(d);
	}

	private Alert loadingAlert(String text) {
		Alert a = new Alert("");
		a.setString(text);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(30000);
		return a;
	}
	
	Alert warningAlert(String text) {
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
		a.setTimeout(1500);
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
		case 292:
			return "выф";
		default:
			return "";
		}
	}
	
	static String duration(int t) {
		t /= 60;
		if(t > 24 * 60) {
			int hours = t / 60;
			if(hours == 0)
				return (hours / 24) + " д.";
			return (hours / 24) + " д. " + (hours % 24) + " ч.";
		}
		if(t > 60) {
			if(t % 60 == 0)
				return (t / 60) + " ч.";
			return (t / 60) + " ч. " + (t % 60) + " мин";
		}
		return t + " мин";
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
	
	private static byte[] readBytes(InputStream inputStream, int initialSize, int bufferSize, int expandSize) throws IOException {
		if (initialSize <= 0) initialSize = bufferSize;
		byte[] buf = new byte[initialSize];
		int count = 0;
		byte[] readBuf = new byte[bufferSize];
		int readLen;
		while ((readLen = inputStream.read(readBuf)) != -1) {
			if(count + readLen > buf.length) {
				byte[] newbuf = new byte[count + expandSize];
				System.arraycopy(buf, 0, newbuf, 0, count);
				buf = newbuf;
			}
			System.arraycopy(readBuf, 0, buf, count, readLen);
			count += readLen;
		}
		if(buf.length == count) {
			return buf;
		}
		byte[] res = new byte[count];
		System.arraycopy(buf, 0, res, 0, count);
		return res;
	}
	
	static byte[] get(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = (HttpConnection) Connector.open(url);
			hc.setRequestMethod("GET");
			hc.getResponseCode();
			in = hc.openInputStream();
			return readBytes(in, (int) hc.getLength(), 1024, 2048);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {
			}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {
			}
		}
	}

	static String getUtf(String url) throws IOException {
		return new String(get(url), "UTF-8");
	}

	static JSONObject api(String url) throws Exception {
//		url = "http://api.rasp.yandex.net/v3.0/" + url + (!url.endsWith("?") ? "&" : "") + "apikey=" + APIKEY + "&format=json&lang=ru_RU";
//		if (proxy)
//			url = "http://nnp.nnchan.ru:80/hproxy.php?u=" + url(url);
		String r = getUtf("http://api.rasp.yandex.net/v3.0/" + url + (!url.endsWith("?") ? "&" : "") + "apikey=" + APIKEY + "&format=json&lang=ru_RU");
		JSONObject j = JSON.getObject(r);
		if(j.has("error")) {
			// выбрасывать эксепшн с текстом ошибки
			throw new Exception(j.getObject("error").getString("text"));
		}
		return j;
	}

}
