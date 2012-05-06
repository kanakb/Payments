package mobisocial.payments;

import java.util.List;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SendBillActivity extends Activity {
    public static final String TAG = "SendBillActivity";
    
    private static final String ACTION_CREATE_FEED = "musubi.intent.action.CREATE_FEED";
    private static final int REQUEST_CREATE_FEED = 1;
    
    private Musubi mMusubi;
    private Uri mFeedUri;
    
    private OnClickListener mSendClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            sendBill(Integer.parseInt(
                            ((EditText)SendBillActivity.this.findViewById(R.id.amount))
                                .getText().toString()));
        }
    };
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CREATE_FEED && resultCode == RESULT_OK) {
            mFeedUri = data.getData();
            DbFeed feed = mMusubi.getFeed(mFeedUri);
            
            List<DbIdentity> members = feed.getMembers();
            if (members.size() > 2) {
                members = members.subList(0, 2);
            } else if (members.size() < 2) {
                Toast.makeText(this, "A payer must be specified.", Toast.LENGTH_SHORT);
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            
            // TODO: get this going with SocialKit
            DbIdentity other = members.get(1);
            ((TextView)findViewById(R.id.billLabel))
                .setText("What amount would you like to bill " + other.getName() + "?");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_bill);
        mMusubi = Musubi.getInstance(this);
        findViewById(R.id.sendBillButton).setOnClickListener(mSendClickListener);
        ((TextView)findViewById(R.id.billLabel))
            .setText("Payer must be specified.");
        Intent create = new Intent(ACTION_CREATE_FEED);
        startActivityForResult(create, REQUEST_CREATE_FEED);
    }
    
    private void sendBill(int amount) {
        if (mFeedUri == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        Log.d(TAG, "Feed URI: " + mFeedUri);
        
        DbFeed feed = mMusubi.getFeed(mFeedUri);
        
        DbIdentity me = feed.getLocalUser();
        List<DbIdentity> members = feed.getMembers();
        Log.d(TAG, "My ID: " + me.getId() + " Name: " + me.getName());
        for (DbIdentity member : members) {
            Log.d(TAG, "ID: " + member.getId() + " Name: " + member.getName());
        }
        
        JSONObject one = new JSONObject();
        try {
            one.put("amount", amount);
            one.put("payee", me.getName());
            one.put(Obj.FIELD_HTML, "<html>You owe me $" + amount + "</html>");
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error", e);
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        
        feed.insert(new MemObj("expayment", one));
        Log.d(TAG, feed.getLatestObj().getJson().toString());
        Intent data = new Intent();
        data.setData(mFeedUri);
        setResult(RESULT_OK, data);
        finish();
    }
}
