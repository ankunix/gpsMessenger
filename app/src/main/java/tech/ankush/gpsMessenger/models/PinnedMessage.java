// FERGUS CODE START

package tech.ankush.gpsMessenger.models;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

public class PinnedMessage {

    public String message;
    public String username;
    public Date date;
    public LatLng location;

    public PinnedMessage(String message, String username, Date date, double latitude, double longitude ) {
        this.message = message;
        this.username = username;
        this.date = date;
        this.location = new LatLng( latitude, longitude );
    }

}

// FERGUS CODE END
