package mobisocial.payments;

import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class VerifyPaymentActivity extends Activity {
    public static final String TAG = "VerifyPaymentActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accept_bill);
        
        if (getIntent() == null || getIntent().getData() == null) {
            finish();
            return;
        }
        
        // Clear any pending notifications
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
        
        Musubi musubi = Musubi.getInstance(this);
        DbObj obj = musubi.objForUri(getIntent().getData());
        DbFeed feed = obj.getContainingFeed();
        JSONObject json = obj.getJson();
        int myIndex = (feed.getMembers().get(0).getName()
                .equals(feed.getLocalUser().getName())) ? 1 : 0;
        try {
            ((TextView)findViewById(R.id.verifyText))
                .setText("Success!" +
                         "\nPayer: " + feed.getMembers().get(myIndex).getName() +
                         "\nAmount: " + json.getString("amount") +
                         "\nTransaction ID: " + json.getString("tid"));
                ((Button)findViewById(R.id.yesbutton)).setText("OK");
        } catch (JSONException e1) {
            finish();
            return;
        }
        
        // Send the token if yes is clicked
        findViewById(R.id.yesbutton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VerifyPaymentActivity.this, PaymentsActivity.class);
                startActivity(intent);
                finishActivity();
            }
        });
        findViewById(R.id.nobutton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VerifyPaymentActivity.this, PaymentsActivity.class);
                startActivity(intent);
                finishActivity();
            }
        });
        
        // Hide the buttons unless there's something to ask
        ((Button)findViewById(R.id.yesbutton)).setVisibility(Button.INVISIBLE);
        ((Button)findViewById(R.id.nobutton)).setVisibility(Button.INVISIBLE);
        
        //getBankNames(getIntent().getData());
    }
    
    private void finishActivity() {
        finish();
    }
}
