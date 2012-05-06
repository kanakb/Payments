package mobisocial.payments;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class MessageReceiver extends BroadcastReceiver {
	public static final String TAG = "MessageReceiver";
	
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received message " + intent);

        Uri objUri = intent.getParcelableExtra("objUri");
        if (objUri == null) {
            Log.i(TAG, "No object found");
            return;
        }

        Musubi musubi = Musubi.forIntent(context, intent);
        DbObj obj = musubi.objForUri(objUri);
        if (obj.getSender().isOwned()) {
            return;
        }
        JSONObject data = obj.getJson();
        
        // generate notification
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(android.R.visa, "New bill", System.currentTimeMillis());
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        
        Intent notificationIntent = new Intent(context, AcceptBillActivity.class);
        notificationIntent.setData(obj.getUri());

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        try {
            notification.setLatestEventInfo(context, "New bill from " + data.getString("payee"), "$" + data.getInt("amount"), contentIntent);
        } catch (JSONException e) {
            Log.w(TAG, "JSON incomplete", e);
            return;
        }
        long[] vibrate = {0,100,200,300};
        notification.vibrate = vibrate;
        nm.notify(0, notification);

        // Don't notify in Musubi
        Bundle b = new Bundle();
        b.putInt("notification", 0);
        setResult(Activity.RESULT_OK, null, b);
    }
}
