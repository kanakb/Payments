package mobisocial.payments;

import mobisocial.payments.server.TokenVerifier;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class PaymentsActivity extends Activity {
    public static final String TAG = "PaymentsActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.d(TAG, TokenVerifier.nameForRoutingNumber("031176110"));
        Log.d(TAG, TokenVerifier.getCertificateOwner("https://home.ingdirect.com"));
    }
}