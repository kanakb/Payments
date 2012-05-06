package mobisocial.payments;

import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.Musubi;
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
        
        if (getIntent().getData() == null) {
            return;
        }
        
        Musubi musubi = Musubi.getInstance(this);
        Obj obj = musubi.objForUri(getIntent().getData());
        JSONObject data = obj.getJson();
        String name;
        int amount;
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
                // TODO: keep going on yes
            }
        });
        findViewById(R.id.nobutton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AcceptBillActivity.this, PaymentsActivity.class);
                startActivity(intent);
            }
        });
    }
}
