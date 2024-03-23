import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
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
import javax.microedition.location.Location;
import javax.microedition.location.LocationProvider;
import javax.microedition.location.QualifiedCoordinates;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

public class MahoRaspApp2 extends MIDlet implements CommandListener, ItemCommandListener, Runnable, ItemStateListener {

	// команды главной формы
	private static final Command exitCmd = new Command("Выход", Command.EXIT, 1);
	private static final Command backCmd = new Command("Назад", Command.BACK, 1);
	private static final Command bookmarksCmd = new Command("Закладки", Command.SCREEN, 4);
	private static final Command settingsCmd = new Command("Настройки", Command.SCREEN, 5);
	private static final Command aboutCmd = new Command("О программе", Command.SCREEN, 6);
	private static final Command submitCmd = new Command("Искать", Command.ITEM, 2);
	private static final Command choosePointCmd = new Command("Выбрать", Command.ITEM, 1);
	private static final Command reverseCmd = new Command("Развернуть", Command.SCREEN, 3);

	// команды формы результатов
	private static final Command addBookmarkCmd = new Command("Добавить в закладки", Command.SCREEN, 3);
	private static final Command prevDayCmd = new Command("Пред. день", Command.SCREEN, 4);
	private static final Command nextDayCmd = new Command("След. день", Command.SCREEN, 5);
	private static final Command showGoneCmd = new Command("Показать ушедшие", Command.ITEM, 2);
	private static final Command itemCmd = new Command("Подробнее", Command.ITEM, 2);

	// команды формы выбора
	private static final Command doneCmd = new Command("Готово", Command.OK, 1);
	private static final Command cancelCmd = new Command("Отмена", Command.CANCEL, 1);
	private static final Command gpsCmd = new Command("Ближайший город", Command.ITEM, 2);
//	private static final Command searchCmd = new Command("Поиск", Command.ITEM, 2);

	// закладки
	private static final Command removeBookmarkCmd = new Command("Удалить", Command.ITEM, 2);
	private static final Command moveBookmarkCmd = new Command("Переместить", Command.ITEM, 3);
	
	private static final Command hyperlinkCmd = new Command("Открыть", Command.ITEM, 2);
	
	private static final Font smallfont = Font.getFont(0, 0, Font.SIZE_SMALL);
	
	private static final int RUN_REQUEST = 1;
	private static final int RUN_ITEM_DETAILS = 2;
	private static final int RUN_SEARCH = 3;
	private static final int RUN_BOOKMARKS_SCREEN = 4;
	private static final int RUN_UPDATE_RESULT = 5;
	private static final int RUN_NEAREST_CITY = 6;
	private static final int RUN_LOCATION = 7;
	
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
	private static final String SETTINGS_RECORDNAME = "mahoRsets";
	private static final String BOOKMARKS_RECORDNAME = "mahoRbm";

	public static MahoRaspApp2 midlet;
	private static Display display;
	
	private static boolean started;
	
	// ui главной
	private static Form mainForm;
	private static ChoiceGroup transportChoice;
	private static DateField dateField;
	private static StringItem fromBtn;
	private static StringItem toBtn;
	private static StringItem submitBtn;
	private static ChoiceGroup showTransfers;

	private static Form settingsForm;
	private static TextField timezoneField;
	private static ChoiceGroup timezoneChoice;
//	private ChoiceGroup proxyChoice;
	
	private static Form resultForm;

	private static Image planeImg;
	private static Image trainImg;
	private static Image suburbanImg;
	private static Image busImg;

	private int run;
	private static boolean running;
	
	private static String from;
	private static String to;

	private static JSON result;
	private static Hashtable items = new Hashtable();
	private static int selectedItem;
	private static String resultTitle;
	private static boolean showGone;
	
	private static Form searchForm;
	private static TextField searchField;
	private static ChoiceGroup searchChoice;

	private static int choosing;
	private static Vector searchIds = new Vector();
	private static boolean searchDoneCmdAdded;
	private static InputStream stream;
	private static Object searchLock = new Object();
	private static boolean searching;
	
	private static InputStreamReader searchReader;
	private static boolean searchCancel;
	
	private static JSON bookmarks;
	private static int movingBookmark = -1;

	private static Object locationProvider;
	private static Alert gpsDialog;
	private static boolean gpsActive;
	private static double gpslat;
	private static double gpslon;
	
	// настройки
	private static String timezone;
	private static int timezoneMode;
//	private static boolean proxy;

	public MahoRaspApp2() {
		midlet = this;
		mainForm = new Form("MahoRasp");
		mainForm.addCommand(exitCmd);
		mainForm.addCommand(bookmarksCmd);
		mainForm.addCommand(settingsCmd);
		mainForm.addCommand(aboutCmd);
		mainForm.addCommand(reverseCmd);
		mainForm.setCommandListener(this);
		transportChoice = new ChoiceGroup("Тип транспорта", Choice.POPUP, TRANSPORT_NAMES, null);
		mainForm.append(transportChoice);
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
		dateField = new DateField("Дата", DateField.DATE);
		dateField.setDate(new Date());
		mainForm.append(dateField);
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
			// загрузка иконок
			planeImg = Image.createImage("/plane.png");
			trainImg = Image.createImage("/train.png");
			suburbanImg = Image.createImage("/suburban.png");
			busImg = Image.createImage("/bus.png");
		} catch (Exception e) {}
		try {
			// определение таймзоны системы
			int i = TimeZone.getDefault().getRawOffset() / 60000;
			timezone = (i < 0 ? '-' : '+') + n(Math.abs(i / 60)) + ':' + n(Math.abs(i % 60));
		} catch (Exception e) {}
		try {
			// загрузка настроек
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, false);
			JSON j = getObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			timezone = j.getString("tz", timezone);
			timezoneMode = j.getInt("tzm", timezoneMode);
//			proxy = j.getBoolean("proxy", proxy);
		} catch (Exception e) {}
		display(mainForm);
	}

	public void commandAction(Command c, Item i) {
		if(c == itemCmd) { // выбран рейс, показать его детали
			display(loadingAlert("Загрузка"), null);
			selectedItem = ((Integer) items.get(i)).intValue();
			start(2);
			return;
		}
		if(c == choosePointCmd) { // начать выбор точки
			choosing = i == fromBtn ? 1 : 2;
			// TODO выбор станции
			searchForm = new Form("Выбор города");
			searchDoneCmdAdded = true;
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
		if(c == showGoneCmd) { // показать ушедшие
			if(running) return;
			showGone = true;
			display(loadingAlert("Загрузка"));
			start(RUN_UPDATE_RESULT);
			return;
		}
		if(c == hyperlinkCmd) { // открыть гиперссылку из формы "о программе"
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
			if(settingsForm == d) {
				// сохранить настройки
				timezone = timezoneField.getString();
				timezoneMode = timezoneChoice.getSelectedIndex();
//				proxy = proxyChoice.isSelected(0);
				try {
					RecordStore.deleteRecordStore(SETTINGS_RECORDNAME);
				} catch (Exception e) {
				}
				try {
					JSON j = new JSON((Hashtable) null);
					j.put("tz", timezone);
					j.put("tzm", timezoneMode);
//					j.put("proxy", proxy);
					byte[] b = j.toString().getBytes("UTF-8");
					RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, true);
					r.addRecord(b, 0, b.length);
					r.closeRecordStore();
				} catch (Exception e) {
				}
			} else if(resultForm == d) {
				// очистить результаты перед выходом на главную форму
				items.clear();
				resultForm = null;
			} else if(resultForm != null && d instanceof Form) {
				// возврат на список результатов из формы деталей маршрута
				display(resultForm);
				return;
			}
			display(mainForm);
			return;
		}
		if(c == submitCmd) { // поиск маршрутов
			if(running) return;
			if(from == null || to == null) {
				display(warningAlert("Не выбран один из пунктов"), null);
				return;
			}
			showGone = false;
			display(loadingAlert("Загрузка"));
			start(RUN_REQUEST);
			return;
		}
		if(c == settingsCmd) {
			settingsForm = new Form("Настройки");
			settingsForm.addCommand(backCmd);
			settingsForm.setCommandListener(this);
			timezoneField = new TextField("Часовой пояс", timezone, 10, TextField.ANY);
			settingsForm.append(timezoneField);
			timezoneChoice = new ChoiceGroup("Отображение времени", Choice.POPUP, new String[] {
					"Местное время",
					"По указанному часовому поясу",
					}, null);
			timezoneChoice.setSelectedIndex(timezoneMode, true);
			settingsForm.append(timezoneChoice);
//			proxyChoice = new ChoiceGroup(null, Choice.MULTIPLE, new String[] { "Проксировать запросы" }, null);
//			proxyChoice.setSelectedIndex(0, proxy);
//			settingsForm.append(proxyChoice);
			display(settingsForm);
			return;
		}
		if(c == gpsCmd) { // выбрать ближайший город
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
				start(RUN_LOCATION);
			} catch (Throwable e) {
				display(warningAlert(e.toString()), searchForm);
			}
			return;
		}
		if(c == cancelCmd) {
			if(d instanceof List) { // отменено перемещение закладки
				movingBookmark = -1;
				((List)d).removeCommand(cancelCmd);
				((List)d).addCommand(removeBookmarkCmd);
				((List)d).addCommand(moveBookmarkCmd);
				((List)d).addCommand(backCmd);
				return;
			}
			if(d == gpsDialog) { // отменен диалог гпс
				gpsActive = false;
				try {
					resetGPS();
				} catch (Throwable e) {}
				display(searchForm);
				return;
			}
			// отменен поиск города
			cancelChoice();
			return;
		}
		if(c == doneCmd) { // выбрана точка
			int i = searchChoice.getSelectedIndex();
			if(i == -1) { // если список пустой то отмена
				cancelChoice();
				return;
			}
			select((String) searchIds.elementAt(i), searchChoice.getString(i));
			return;
		}
		if(c == prevDayCmd) { // предыдущий день в форме результатов
			if(running) return;
			Date date = dateField.getDate();
			date.setTime(date.getTime()-24*60*60*1000);
			dateField.setDate(date);
			display(loadingAlert("Загрузка"));
			start(RUN_REQUEST);
			return;
		}
		if(c == nextDayCmd) { // следующий день в форме результатов
			if(running) return;
			Date date = dateField.getDate();
			date.setTime(date.getTime()+24*60*60*1000);
			dateField.setDate(date);
			display(loadingAlert("Загрузка"));
			start(RUN_REQUEST);
			return;
		}
		if(c == bookmarksCmd) { // открыть список закладок
			if(running) return;
			display(loadingAlert("Загрузка"));
			start(RUN_BOOKMARKS_SCREEN);
			return;
		}
		if(c == addBookmarkCmd) { // добавить маршрут город-город в закладки
			if(from == null || to == null) {
				display(warningAlert("Не выбран один из пунктов"));
				return;
			}
			String fn = fromBtn.getText();
			String tn = toBtn.getText();
			
			if(bookmarks == null) {
				try {
					RecordStore r = RecordStore.openRecordStore(BOOKMARKS_RECORDNAME, false);
					bookmarks = getArray(new String(r.getRecord(1), "UTF-8"));
					r.closeRecordStore();
				} catch (Exception e) {
					bookmarks = new JSON(10);
				}
			} else {
				// есть ли уже такая закладка
				int l = bookmarks.size();
				for(int i = 0; i < l; i++) {
					JSON j = bookmarks.getObject(i);
					if(j.getInt("t") == BOOKMARK_CITIES && fn.equals(j.getString("c")) && tn.equals(j.getString("d"))) return;  
				}
			}
			
			JSON bm = new JSON((Hashtable) null);
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
		if(c == removeBookmarkCmd) { // удалить закладку (контекстка)
			if(bookmarks == null || movingBookmark > -1) return;
			int idx;
			((List)d).delete(idx = ((List)d).getSelectedIndex());
			bookmarks.remove(idx);
			display(infoAlert("Закладка удалена"), null);
			return;
		}
		if(c == moveBookmarkCmd) { // начать перемещение закладки (контекстка)
			if(bookmarks == null) return;
			movingBookmark = ((List)d).getSelectedIndex();
			if(movingBookmark == -1) return;
			((List)d).removeCommand(removeBookmarkCmd);
			((List)d).removeCommand(moveBookmarkCmd);
			((List)d).removeCommand(backCmd);
			((List)d).addCommand(cancelCmd);
			return;
		}
		if(c == reverseCmd) {
			String tmp = from;
			from = to;
			to = tmp;
			tmp = fromBtn.getText();
			fromBtn.setText(toBtn.getText());
			toBtn.setText(tmp);
			return;
		}
		if(c == List.SELECT_COMMAND) { // выбрана закладка в списке
			if(bookmarks == null) return;
			int i = ((List)d).getSelectedIndex();
			// завершить перемещение закладки
			if(movingBookmark > -1) {
				int j = movingBookmark;
				movingBookmark = -1;
				if(j != i) {
					((List)d).removeCommand(cancelCmd);
					((List)d).addCommand(removeBookmarkCmd);
					((List)d).addCommand(moveBookmarkCmd);
					((List)d).addCommand(backCmd);
					JSON bm = bookmarks.getObject(j);
					bookmarks.remove(j);
					bookmarks.put(i, bm);
					String s = ((List)d).getString(j);
					((List)d).delete(j);
					((List)d).insert(i, s, null);
				}
				return;
			}
			JSON bm = bookmarks.getObject(i);
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
		if(c == aboutCmd) {
			Form f = new Form("О программе");
			f.addCommand(backCmd);
			f.setCommandListener(this);
			StringItem s;
			try {
				f.append(new ImageItem(null, Image.createImage("/icon.png"), Item.LAYOUT_LEFT, null));
				s = new StringItem(null, "MahoRasp v" + getAppProperty("MIDlet-Version"));
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
			s = new StringItem("Донат", "boosty.to/nnproject/donate", Item.HYPERLINK);
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
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			display(f);
			return;
		}
	}

	public void itemStateChanged(Item item) {
		if(item == searchField) { // выполнять поиск при изменениях в поле ввода
			if(running) return;
			start(RUN_SEARCH);
		}
		if(item == searchChoice) {
			if(searchChoice.getSelectedIndex() != -1) {
				if(!searchDoneCmdAdded) return;
				searchForm.addCommand(doneCmd);
				searchDoneCmdAdded = false;
				return;
			}
			if(searchDoneCmdAdded) return;
			searchForm.removeCommand(doneCmd);
			searchDoneCmdAdded = true;
			return;
		}
	}
	
	public void run() {
		int run;
		synchronized(this) {
			run = this.run;
			notify();
		}
		running = run != RUN_SEARCH && run != RUN_LOCATION;
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
		case RUN_ITEM_DETAILS: { // показ деталей рейса
			Form f = new Form("");
			f.addCommand(backCmd);
			f.setCommandListener(this);
			JSON seg = result.getArray("segments").getObject(selectedItem);
			
			if(seg.getBoolean("has_transfers")) { // есть пересадки
				JSON types = seg.getArray("transport_types");
				String title = seg.getObject("departure_from").getString("title") + " - " + seg.getObject("arrival_to").getString("title");
				f.setTitle(title);
				StringItem t = new StringItem(null, title + "\n");
				t.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
				f.append(t);
				if(seg.has("details")) {
					JSON details = seg.getArray("details");
					int i = 0;
					int l = details.size();
					for(int j = 0; j < l; j++) {
						JSON n = details.getObject(j);
						if(n.has("thread")) {
							JSON th = n.getObject("thread");
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
							String dep, arr;
							Calendar departure = getLocalizedDate(dep = seg.getString("departure"));
							Calendar arrival = getLocalizedDate(arr = seg.getString("arrival"));
							boolean tzDiffer = parseTimeZone(dep) != parseTimeZone(arr) && timezoneMode == 0;
							StringItem s = new StringItem(null,
									"Отправление: " + point(n.getObject("from")) + "\n" +
									shortDate(departure) + " " + time(departure) + (tzDiffer ? " (" + getTimeZoneStr(dep) + ")" : "") + "\n" + d + "\n" +
									"Прибытие: " + point(n.getObject("to")) + "\n" +
									shortDate(arrival) + " " + time(arrival) + (tzDiffer ? " (" + getTimeZoneStr(arr) + ")" : "") + "\n" +
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
			} else { // нет пересадок
				JSON thread = seg.getObject("thread");
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
				String dep, arr;
				Calendar departure = getLocalizedDate(dep = seg.getString("departure"));
				Calendar arrival = getLocalizedDate(arr = seg.getString("arrival"));
				boolean tzDiffer = parseTimeZone(dep) != parseTimeZone(arr) && timezoneMode == 0;
				StringItem s = new StringItem(null,
						"Отправление:\n" + point(seg.getObject("from")) + "\n" +
						shortDate(departure) + " " + time(departure) + (tzDiffer ? " (" + getTimeZoneStr(dep) + ")" : "") + "\n" + d + "\n" +
						"Прибытие:\n" + point(seg.getObject("to")) + "\n" +
						shortDate(arrival) + " " + time(arrival) + (tzDiffer ? " (" + getTimeZoneStr(arr) + ")" : "") + "\n" +
						(seg.has("duration") ? (duration(seg.getInt("duration")) + "\n") : "") + "\n"
						);
				s.setFont(smallfont);
				f.append(s);
				if((m = seg.getNullableString("stops")) != null && m.length() > 0) {
					s = new StringItem(null, "Остановки: " + m + "\n\n");
					s.setFont(smallfont);
					f.append(s);
				}
				JSON o;
				if((o = seg.getNullableObject("tickets_info")) != null) {
					String r = "Цена:\n";
					JSON places = o.getArray("places");
					int i = places.size();
					for(int j = 0; j < i; j++) {
						JSON p = places.getObject(j);
						String name = p.getString("name");
						JSON price = p.getObject("price");
						if(name != null) {
							r += name + ": ";
						}
						r += price.getInt("whole") + "." + price.getInt("cents") + " " + p.getString("currency") + "\n";
					}
					s = new StringItem(null, r + "\n");
					s.setFont(smallfont);
					f.append(s);
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
		case RUN_SEARCH: { // поиск точек
			if(searching) {
				// отменить текущий поиск, если что-то уже ищется
				searchReader = null;
				searchCancel = true;
				try {
					synchronized(searchLock) {
						searchLock.wait();
					}
				} catch (Exception e) {}
			}
			searching = true;
			InputStreamReader r = null;
			s: {
				try {
					String query = searchField.getString().toLowerCase().trim();
					searchChoice.deleteAll();
					searchIds.removeAllElements();
					searchChoice.setLabel("Поиск...");
					Vector items = new Vector();
					search: {
						if(query.length() < 3) break search;
						r = searchReader = openCitiesStream();
						if(searchReader.read() != 'm' || searchReader.read() != '[')
							throw new Exception("Cities database is corrupted");
						StringBuffer sb;
						char c;
						while(!searchCancel) {
							sb = new StringBuffer();
							while((c = (char) searchReader.read()) != '"') {
								sb.append(c);
							}
							String regionName = sb.toString();
							for(;;) {
								sb = new StringBuffer();
								while((c = (char) searchReader.read()) != '"') {
									sb.append(c);
								}
								String code = sb.toString();
								sb = new StringBuffer();
								while((c = (char) searchReader.read()) != '"') {
									sb.append(c);
								}
								String cityName = sb.toString();
								if(cityName.toLowerCase().startsWith(query)) {
									searchChoice.append(cityName + ", " + regionName, null);
									searchIds.addElement(code);
								} else if(regionName.toLowerCase().startsWith(query)) {
									items.addElement(new String[] {cityName, regionName, code});
								}
								if(searchReader.read() != ',') {
									break;
								}
							}
							if(searchReader.read() != ',') {
								break;
							}
						}
					}
					if(searchForm == null || searchCancel) break s;
					searchChoice.setLabel("");
					for(Enumeration e = items.elements(); e.hasMoreElements(); ) {
						if(searchChoice.size() > 10) break;
						String[] s = (String[]) e.nextElement();
						searchChoice.append(s[0] + ", " + s[1], null);
						searchIds.addElement(s[2]);
					}
					// замена функции "отмена" на "готово"
					if(searchChoice.getSelectedIndex() != -1) {
						if(!searchDoneCmdAdded) break s;
		//				searchForm.removeCommand(cancelCmd);
						searchForm.addCommand(doneCmd);
						searchDoneCmdAdded = false;
						break s;
					}
					if(searchDoneCmdAdded) break s;
					searchForm.removeCommand(doneCmd);
		//			searchForm.addCommand(cancelCmd);
					searchDoneCmdAdded = true;
				} catch (Exception e) {
					if(searchCancel) break s;
					e.printStackTrace();
					display(warningAlert(e.toString()), searchForm);
				}
			}
			if(r != null) {
				try {
					r.close();
				} catch (Exception e) {}
			}
			searchCancel = searching = false;
			synchronized(searchLock) {
				searchLock.notifyAll();
			}
			return;
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
					bookmarks = getArray(new String(r.getRecord(1), "UTF-8"));
					r.closeRecordStore();
				}
				l.addCommand(removeBookmarkCmd);
				l.addCommand(moveBookmarkCmd);
				int i = bookmarks.size();
				for(int j = 0; j < i; j++) {
					JSON bm = bookmarks.getObject(j);
					l.append(bm.has("n") ? bm.getString("n") : bm.getString("c") + " - " + bm.getString("d"), null);
				}
			} catch (Exception e) {
			}
			display(l);
			break;
		}
		case RUN_UPDATE_RESULT: { // обновить форму результатов
			if(result == null) break;
			items.clear();
			resultForm.deleteAll();
			parseResults();
			display(resultForm);
			break;
		}
		case RUN_NEAREST_CITY: { // запросить ближайший город, координаты уже получены
			display(loadingAlert("Загрузка"), searchForm);
			try {
				JSON r = api("nearest_settlement/?lat=" + gpslat + "&lng=" + gpslon);
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
		case RUN_LOCATION: {
			try {
				runGPS();
			} catch (Throwable e) {}
			gpsDone();
			return;
		}
		}
		running = false;
	}

	private static String point(JSON o) {
		if("station".equals(o.getString("type"))) { // если станция, добавить её тип
			return o.getString("title") + " (" + o.getString("station_type_name") + ")";
		}
		return o.getString("title");
	}

	private static void parseResults() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateField.getDate());
		long time = System.currentTimeMillis();
		Calendar now = getLocalizedTime(time);
		JSON search = result.getObject("search");
		String title = search.getObject("from").getString("title") + " - " + search.getObject("to").getString("title");
		resultTitle = title;
		StringItem titleItem = new StringItem(n(cal.get(Calendar.DAY_OF_MONTH)) + "." + n(cal.get(Calendar.MONTH) + 1) + "." + cal.get(Calendar.YEAR), title + "\n");
		titleItem.setLayout(Item.LAYOUT_CENTER);
		resultForm.append(titleItem);
		
		StringItem left = new StringItem(null, "");
		left.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
		int idx = resultForm.append(left);
		
		JSON segments = result.getArray("segments");
		int size = segments.size();
		if(size == 0) {
			left.setText("Пусто!");
		}
		int goneCount = 0;
		for(int i = 0; i < size; i++) {
			JSON seg = segments.getObject(i);
			Calendar departure = getLocalizedDate(seg.getString("departure"));
			if(!showGone && oneDay(now, departure) && parseDateGMT(seg.getString("departure")) < time) {
				goneCount++;
				continue;
			}
			Calendar arrival = getLocalizedDate(seg.getString("arrival"));
			String r = "";
			if(seg.getBoolean("has_transfers")) {
				JSON types = seg.getArray("transport_types");
				for(int j = 0; j < types.size(); j++) {
					resultForm.append(new ImageItem(null, transportImg(types.getString(j)), Item.LAYOUT_LEFT, null));
				}
				JSON from = seg.getObject("departure_from");
				JSON to = seg.getObject("arrival_to");
				r += from.getString("title") + " - " + to.getString("title") + "\n";
				r += "с пересадками\n";
			} else {
				JSON thread = seg.getObject("thread");
				r += thread.getString("number") + " " + thread.getString("title") + "\n";
				resultForm.append(new ImageItem(null, transportImg(thread.getString("transport_type")), Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE, null));
			}
			r += (oneDay(cal, departure) ? time(departure) : shortDate(departure) + " " + time(departure));
			r += " - " + (oneDay(cal, arrival) ? time(arrival) : shortDate(arrival) + " " + time(arrival)) + "\n";
			StringItem s = new StringItem("", r + "\n");
			s.addCommand(itemCmd);
			s.setDefaultCommand(itemCmd);
			s.setItemCommandListener(midlet);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
			items.put(s, new Integer(i));
			resultForm.append(s);
		}
		
		if(goneCount > 0) {
			StringItem showGoneBtn = new StringItem(null, "Показать ушедшие (" + goneCount + ")", Item.BUTTON);
			showGoneBtn.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			showGoneBtn.addCommand(showGoneCmd);
			showGoneBtn.setDefaultCommand(showGoneCmd);
			showGoneBtn.setItemCommandListener(midlet);
			resultForm.insert(idx, showGoneBtn);
		}
	}

	private void gpsDone() {
		if(!gpsActive) return;
		gpsActive = false;
		if(gpslat == 0 && gpslon == 0) {
			// поймать ванну не удалось
			display(warningAlert("Не удалось получить геолокацию"), searchForm);
			return;
		}
		start(RUN_NEAREST_CITY);
//		display(searchForm);
	}

	private void start(int i) {
		try {
			synchronized(this) {
				run = i;
				new Thread(this).start();
				wait();
				run = 0;
			}
		} catch (Exception e) {}
	}
	
	private static void select(String code, String title) {
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
	
	private static void cancelChoice() {
		display(mainForm);
		if(searching) {
			searchReader = null;
			searchCancel = true;
		}
		searchForm = null;
		searchField = null;
		searchChoice = null;
		searchIds.removeAllElements();
	}
	
	InputStreamReader openCitiesStream() throws IOException {
		if(stream != null) {
			try {
				// попытка ресетнуть поток
				stream.reset();
				return new InputStreamReader(new JSON(stream), "UTF-8");
			} catch (Exception e) {
				// не получилось, переоткрываем
				try {
					stream.close();
				} catch (IOException e2) {}
				stream = getClass().getResourceAsStream("/cities");
				return new InputStreamReader(new JSON(stream), "UTF-8");
			}
		}
		stream = getClass().getResourceAsStream("/cities");
		return new InputStreamReader(new JSON(stream), "UTF-8");
	}
	
	private static void runGPS() {
		try {
			if(locationProvider == null) {
				locationProvider = LocationProvider.getInstance(null);
			}
			Location l = ((LocationProvider) locationProvider).getLocation(60);
			if(l != null) {
				QualifiedCoordinates c = l.getQualifiedCoordinates();
				gpslat = c.getLatitude();
				gpslon = c.getLongitude();
			}
		} catch (Throwable e) {
			gpslat = 0;
			gpslon = 0;
		}
	}
	
	private static void resetGPS() {
		if(locationProvider == null) return;
		((LocationProvider) locationProvider).reset();
	}
	
	private static Image transportImg(String transport) {
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
		// хеликоптер
		throw new RuntimeException("Unknown transport_type: " + transport);
	}
	
	private static void display(Alert a, Displayable d) {
		if(d == null) {
			display.setCurrent(a);
			return;
		}
		display.setCurrent(a, d);
	}
	
	private static void display(Displayable d) {
		if(d instanceof Alert) {
			display.setCurrent((Alert) d, mainForm);
			return;
		}
		display.setCurrent(d);
	}

	private static Alert loadingAlert(String text) {
		Alert a = new Alert("");
		a.setString(text);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(30000);
		return a;
	}
	
	private static Alert warningAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.ERROR);
		a.setString(text);
		a.setTimeout(3000);
		return a;
	}
	
	private static Alert infoAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.CONFIRMATION);
		a.setString(text);
		a.setTimeout(1500);
		return a;
	}
	
	static String n(int n) {
		if(n < 10) {
			return "0".concat(Integer.toString(n));
		} else return Integer.toString(n);
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

	// получить календарь текущего времени, учитывая настройки таймзоны
	static Calendar getLocalizedTime(long time) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(time - c.getTimeZone().getRawOffset() + parseTimeZone(timezone)));
		return c;
	}
	
	// парс даты, учитывая настройки таймзоны
	static Calendar getLocalizedDate(String date) {
		if(timezoneMode == 0)
			return parseDate(date);
		Calendar c = parseDate(date);
		c.setTime(new Date(c.getTime().getTime() - parseTimeZone(date) + parseTimeZone(timezone)));
		return c;
	}
	
	// парс даты в таймстампу
	static long parseDateGMT(String date) {
		Calendar c = parseDate(date);
		return c.getTime().getTime() + c.getTimeZone().getRawOffset() - parseTimeZone(date);
	}
	
	// отрезать таймзону из даты
	static String getTimeZoneStr(String date) {
		int i = date.lastIndexOf('+');
		if(i == -1)
			i = date.lastIndexOf('-');
		if(i == -1)
			return null;
		return date.substring(i);
	}

	// получение оффсета таймзоны даты в миллисекундах
	static int parseTimeZone(String date) {
		int i = date.lastIndexOf('+');
		boolean m = false;
		if(i == -1) {
			i = date.lastIndexOf('-');
			m = true;
		}
		if(i == -1)
			return 0;
		date = date.substring(i + 1);
		int offset = date.lastIndexOf(':');
		offset = (Integer.parseInt(date.substring(0, offset)) * 3600000) +
				(Integer.parseInt(date.substring(offset + 1)) * 60000);
		return m ? -offset : offset;
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
			t /= 60;
			return (t / 24) + " д. " + (t % 24) + " ч.";
		}
		if(t > 60) {
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

	static JSON api(String url) throws Exception {
//		url = "http://api.rasp.yandex.net/v3.0/" + url + (!url.endsWith("?") ? "&" : "") + "apikey=" + APIKEY + "&format=json&lang=ru_RU";
//		if (proxy)
//			url = "http://nnp.nnchan.ru:80/hproxy.php?u=" + url(url);
		String r = getUtf("http://api.rasp.yandex.net/v3.0/" + url + (!url.endsWith("?") ? "&" : "") + "apikey=" + APIKEY + "&format=json&lang=ru_RU");
		JSON j = getObject(r);
		if(j.has("error")) {
			// выбрасывать эксепшн с текстом ошибки
			throw new Exception(j.getObject("error").getString("text"));
		}
		return j;
	}
	
	// nnjson
	
	static final boolean parse_members = false;
	
	// used for storing nulls, get methods must return real null
	public static final Object json_null = new Object();
	
	public static final Boolean TRUE = new Boolean(true);
	public static final Boolean FALSE = new Boolean(false);

	public static JSON getObject(String text) {
		if (text == null || text.length() <= 1)
			throw new RuntimeException("JSON: Empty text");
		if (text.charAt(0) != '{')
			throw new RuntimeException("JSON: Not JSON object");
		return (JSON) parseJSON(text);
	}

	public static JSON getArray(String text) {
		if (text == null || text.length() <= 1)
			throw new RuntimeException("JSON: Empty text");
		if (text.charAt(0) != '[')
			throw new RuntimeException("JSON: Not JSON array");
		return (JSON) parseJSON(text);
	}

	static Object parseJSON(String str) {
		char first = str.charAt(0);
		int length = str.length() - 1;
		char last = str.charAt(length);
		switch(first) {
		case '"': { // string
			if (last != '"')
				throw new RuntimeException("JSON: Unexpected end of text");
			if(str.indexOf('\\') != -1) {
				char[] chars = str.substring(1, length).toCharArray();
				str = null;
				int l = chars.length;
				StringBuffer sb = new StringBuffer();
				int i = 0;
				// parse escaped chars in string
				loop: {
					while (i < l) {
						char c = chars[i];
						switch (c) {
						case '\\': {
							next: {
								replace: {
									if (l < i + 1) {
										sb.append(c);
										break loop;
									}
									char c1 = chars[i + 1];
									switch (c1) {
									case 'u':
										i+=2;
										sb.append((char) Integer.parseInt(
												new String(new char[] {chars[i++], chars[i++], chars[i++], chars[i++]}),
												16));
										break replace;
									case 'x':
										i+=2;
										sb.append((char) Integer.parseInt(
												new String(new char[] {chars[i++], chars[i++]}),
												16));
										break replace;
									case 'n':
										sb.append('\n');
										i+=2;
										break replace;
									case 'r':
										sb.append('\r');
										i+=2;
										break replace;
									case 't':
										sb.append('\t');
										i+=2;
										break replace;
									case 'f':
										sb.append('\f');
										i+=2;
										break replace;
									case 'b':
										sb.append('\b');
										i+=2;
										break replace;
									case '\"':
									case '\'':
									case '\\':
									case '/':
										i+=2;
										sb.append((char) c1);
										break replace;
									default:
										break next;
									}
								}
								break;
							}
							sb.append(c);
							i++;
							break;
						}
						default:
							sb.append(c);
							i++;
						}
					}
				}
				str = sb.toString();
				sb = null;
				return str;
			}
			return str.substring(1, length);
		}
		case '{': // JSON object or array
		case '[': {
			boolean object = first == '{';
			if (object ? last != '}' : last != ']')
				throw new RuntimeException("JSON: Unexpected end of text");
			int brackets = 0;
			int i = 1;
			char nextDelimiter = object ? ':' : ',';
			boolean escape = false;
			String key = null;
			Object res = object ? (Object) new JSON((Hashtable) null) : (Object) new JSON(10);
			
			for (int splIndex; i < length; i = splIndex + 1) {
				// skip all spaces
				for (; i < length - 1 && str.charAt(i) <= ' '; i++);

				splIndex = i;
				boolean quote = false;
				for (; splIndex < length && (quote || brackets > 0 || str.charAt(splIndex) != nextDelimiter); splIndex++) {
					char c = str.charAt(splIndex);
					if (!escape) {
						if (c == '\\') {
							escape = true;
						} else if (c == '"') {
							quote = !quote;
						}
					} else escape = false;
	
					if (!quote) {
						if (c == '{' || c == '[') {
							brackets++;
						} else if (c == '}' || c == ']') {
							brackets--;
						}
					}
				}

				// fail if unclosed quotes or brackets left
				if (quote || brackets > 0) {
					throw new RuntimeException("JSON: Corrupted JSON");
				}

				if (object && key == null) {
					key = str.substring(i, splIndex);
					key = key.substring(1, key.length() - 1);
					nextDelimiter = ',';
				} else {
					Object value = str.substring(i, splIndex).trim();
					// don't check length because if value is empty, then exception is going to be thrown anyway
					char c = ((String) value).charAt(0);
					// leave String[] as value to parse it later, if its object or array and nested parsing is disabled
					value = parse_members || (c != '{' && c != '[') ?
							parseJSON((String) value) : new String[] {(String) value};
					if (object) {
						((JSON) res)._put(key, value);
						key = null;
						nextDelimiter = ':';
					} else if (splIndex > i) {
						((JSON) res).addElement(value);
					}
				}
			}
			return res;
		}
		case 'n': // null
			return json_null;
		case 't': // true
			return TRUE;
		case 'f': // false
			return FALSE;
		default: // number
			if ((first >= '0' && first <= '9') || first == '-') {
				try {
					// hex
					if (length > 1 && first == '0' && str.charAt(1) == 'x') {
						if (length > 9) // str.length() > 10
							return new Long(Long.parseLong(str.substring(2), 16));
						return new Integer(Integer.parseInt(str.substring(2), 16));
					}
					// decimal
					if (str.indexOf('.') != -1 || str.indexOf('E') != -1 || "-0".equals(str))
						return new Double(Double.parseDouble(str));
					if (first == '-') length--;
					if (length > 8) // (str.length() - (str.charAt(0) == '-' ? 1 : 0)) >= 10
						return new Long(Long.parseLong(str));
					return new Integer(Integer.parseInt(str));
				} catch (Exception e) {}
			}
			throw new RuntimeException("JSON: Couldn't be parsed: " + str);
		}
	}

	// transforms string for exporting
	static String escape_utf8(String s) {
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

	static int getInt(Object o) {
		try {
			if (o instanceof String[])
				return Integer.parseInt(((String[]) o)[0]);
			if (o instanceof Integer)
				return ((Integer) o).intValue();
			if (o instanceof Long)
				return (int) ((Long) o).longValue();
			if (o instanceof Double)
				return ((Double) o).intValue();
		} catch (Throwable e) {}
		throw new RuntimeException("JSON: Cast to int failed: " + o);
	}

}
