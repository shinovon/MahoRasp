import javax.microedition.location.Location;
import javax.microedition.location.LocationProvider;
import javax.microedition.location.QualifiedCoordinates;

public class GPS implements Runnable {
	
	private static LocationProvider provider;

	public void run() {
		try {
			if(provider == null) {
				provider = LocationProvider.getInstance(null);
			}
			Location l = provider.getLocation(60);
			if(l != null) {
				QualifiedCoordinates c = l.getQualifiedCoordinates();
				MahoRaspApp2.gpslat = c.getLatitude();
				MahoRaspApp2.gpslon = c.getLongitude();
			}
		} catch (Exception e) {
			e.printStackTrace();
			MahoRaspApp2.gpslat = 0;
			MahoRaspApp2.gpslon = 0;
		}
		MahoRaspApp2.midlet.gpsDone();
	}
	
	static void reset() {
		if(provider == null) return;
		provider.reset();
	}

}
