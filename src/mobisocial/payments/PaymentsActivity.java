package mobisocial.payments;

import java.util.List;

import mobisocial.payments.server.TokenVerifier;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class PaymentsActivity extends Activity {
    public static final String TAG = "PaymentsActivity";
    
    private static final String ACTION_CREATE_FEED = "musubi.intent.action.CREATE_FEED";
    private static final int REQUEST_CREATE_FEED = 1;
    
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

            Intent create = new Intent(ACTION_CREATE_FEED);
            startActivityForResult(create, REQUEST_CREATE_FEED);
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
        if (requestCode == REQUEST_CREATE_FEED && resultCode == RESULT_OK) {
            Uri feedUri = data.getData();
            DbFeed feed = mMusubi.getFeed(feedUri);
            
            List<DbIdentity> members = feed.getMembers();
            if (members.size() > 2) {
                members = members.subList(0, 2);
            } else if (members.size() < 2) {
                Toast.makeText(this, "A payer must be specified.", Toast.LENGTH_SHORT);
                finish();
                return;
            }
            
            // TODO: get this going with SocialKit
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mMusubi = Musubi.forIntent(this, getIntent());
        findViewById(R.id.paybutton).setOnClickListener(mPayButtonListener);
        findViewById(R.id.billbutton).setOnClickListener(mBillButtonListener);
        Log.d(TAG, TokenVerifier.nameForRoutingNumber("031176110"));
        Log.d(TAG, TokenVerifier.getCertificateOwner("https://home.ingdirect.com"));
    }
}