package mobisocial.payments;

import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class AcceptBillActivity extends Activity {
    public static final String TAG = "AcceptBillActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accept_bill);
        
        if (getIntent() == null || getIntent().getData() == null) {
            finish();
            return;
        }
        
        final Musubi musubi = Musubi.getInstance(this);
        final DbObj obj = musubi.objForUri(getIntent().getData());
        final JSONObject data = obj.getJson();
        final String name;
        final int amount;
        try {
            name = data.getString("payee");
            amount = data.getInt("amount");
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing JSON", e);
            return;
        }
        ((TextView)findViewById(R.id.verifyText))
            .setText(name + " is requesting $" + amount + 
                    ". Would you like to allow this transation?");
        findViewById(R.id.yesbutton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // In a new thread, authorize the user and send the token
                new Thread() {
                    @Override
                    public void run() {
                        if (data != null) {
                            DbFeed feed = obj.getContainingFeed();
                            try {
                                data.remove(Obj.FIELD_HTML);
                                data.remove("sent");
                                data.put("routing", "031176110");
                                data.put("accepted", true);
                                data.put("source", "payer");
                            } catch (JSONException e) {
                                Log.e(TAG, "Could not add field to JSON", e);
                                return;
                            }
                            Log.d(TAG, data.toString());
                            feed.insert(new MemObj("expayment", data));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finishActivity();
                                }
                            });
                        }
                    }
                }.start();
                Intent intent = new Intent(AcceptBillActivity.this, PaymentsActivity.class);
                intent.setData(obj.getContainingFeed().getUri());
                startActivity(intent);
                finishActivity();
            }
        });
        findViewById(R.id.nobutton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AcceptBillActivity.this, PaymentsActivity.class);
                startActivity(intent);
                finishActivity();
            }
        });
    }
    
    private void finishActivity() {
        finish();
    }
}
