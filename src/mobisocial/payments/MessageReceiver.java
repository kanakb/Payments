package mobisocial.payments;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import mobisocial.payments.server.BankSession;
import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;
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
        Log.d(TAG, data.toString());
        
        if (!data.has("sent")) {
            if (data.has("done")) {
                try {
                    if (!data.getString("payee").equals(obj.getContainingFeed().getLocalUser().getName())) {
                        return;
                    }
                } catch (JSONException e) {}
            } else {
                try {
                    if (data.has("account") && !data.getString("payee").equals(obj.getContainingFeed().getLocalUser().getName())) {
                        postAccountInfo(obj, context);
                    }
                } catch (JSONException e) {}
                return;
            }
        }
        
        DbFeed feed = obj.getContainingFeed();
        int myIndex = (feed.getMembers().get(0).getName()
                .equals(feed.getLocalUser().getName())) ? 1 : 0;
        String otherParty = feed.getMembers().get(myIndex).getName();
        
        // generate notification
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;
        Intent notificationIntent;
        if (!data.has("done")) {
            notification = new Notification(R.drawable.visa, "New bill", System.currentTimeMillis());
            notificationIntent = new Intent(context, AcceptBillActivity.class);
        } else {
            notification = new Notification(R.drawable.visa, "Payment received", System.currentTimeMillis());
            notificationIntent = new Intent(context, VerifyPaymentActivity.class);
        }
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        
        notificationIntent.setData(obj.getUri());

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        try {
            if (!data.has("done")) {
                notification.setLatestEventInfo(context, "New bill from " + otherParty, "$" + data.getInt("amount"), contentIntent);
            } else {
                notification.setLatestEventInfo(context, "New payment from " + otherParty, "Tap to continue.", contentIntent);
            }
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
    

    
    private void postAccountInfo(final DbObj obj, final Context context) {
        final DbFeed feed = obj.getContainingFeed();
        new Thread() {
            @Override
            public void run() {
                JSONObject json = obj.getJson();
                try {
                    JSONObject signed = BankSession.newInstance(
                            context, "test@test.com", "123456")
                            .getToken(json.optString("transaction"), json.optString("amount"));
                    json.remove("account");
                    json.put("signed", signed);
                    json.put("done", true);
                    json.put(Obj.FIELD_HTML, "<html>Payment is ready</html>");
                    json.put("source", "payer");
                    feed.postObj(new MemObj("expayment", json));
                } catch (JSONException e) {
                    Log.e(TAG, "JSON error", e);
                }
            }
        }.start();
    }
}
