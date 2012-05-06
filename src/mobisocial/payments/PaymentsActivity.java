package mobisocial.payments;

import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class PaymentsActivity extends Activity {
    public static final String TAG = "PaymentsActivity";
    public static final String PREFS_NAME = "PaymentsPrefsFile";
    
    private static final int REQUEST_SEND_BILL = 1;
    
    private Musubi mMusubi;
    
    private OnClickListener mPayButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!askForMusubi()) {
                return;
            }
            //TODO: implement
        }
    };
    
    private OnClickListener mBillButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!askForMusubi()) {
                return;
            }

            Intent create = new Intent(PaymentsActivity.this, SendBillActivity.class);
            startActivityForResult(create, REQUEST_SEND_BILL);
        }
    };
    
    private FeedObserver mPayeeFeedObserver = new FeedObserver() {
        @Override
        public void onUpdate(DbObj obj) {
            // TODO: do something meaningful with the update
            Log.d(TAG, obj.getJson().toString());
        }
    };
    
    private boolean askForMusubi() {
        if (!Musubi.isMusubiInstalled(this)) {
            new AlertDialog.Builder(this).setTitle("Install Musubi?")
                .setMessage("This application lets you pay using the Musubi app" +
            " platform. Would you like to install Musubi now?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent getMusubi = Musubi.getMarketIntent();
                    startActivity(getMusubi);
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .create().show();
            return false;
        }
        return true;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SEND_BILL && resultCode == RESULT_OK) {
            if (data.getData() == null) {
                return;
            }
            
            Toast.makeText(this, "Bill sent.", Toast.LENGTH_LONG);
            Log.d(TAG, "notified bill sent");
            
            if (mMusubi == null) {
                mMusubi = Musubi.getInstance(this);
            }
            
            DbFeed feed = mMusubi.getFeed(data.getData());
            feed.registerStateObserver(mPayeeFeedObserver);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mMusubi = Musubi.forIntent(this, getIntent());
        findViewById(R.id.paybutton).setOnClickListener(mPayButtonListener);
        findViewById(R.id.billbutton).setOnClickListener(mBillButtonListener);
        //Log.d(TAG, TokenVerifier.nameForRoutingNumber("031176110"));
        //Log.d(TAG, TokenVerifier.getCertificateOwner("https://home.ingdirect.com"));
    }
}