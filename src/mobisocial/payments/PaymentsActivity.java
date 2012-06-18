package mobisocial.payments;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONException;
import org.json.JSONObject;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class PaymentsActivity extends Activity {
    public static final String TAG = "PaymentsActivity";
    public static final String PREFS_NAME = "PaymentsPrefsFile";
    
    private static final int REQUEST_SEND_BILL = 1;
    
    private Musubi mMusubi;
    private HashSet<String> mNotifiedSet = new HashSet<String>();
    
    private static final String PUBLIC_KEY = 
            "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwQbMQ6HLpvcS/uOxAzMy\n" +
            "fKKVmnEsz6lRLEBNvHobFLeemZqhlLHuxYTLEU44bbqtN2ZUqucNGWq7YaQInJgC\n" +
            "tb3I6qx8PUDcvq8a9BAEtBGjs/PvVVgag06YDHshXWnIOZ18s+8aDVphyMouxtKg\n" +
            "LnbRJJpcBFuH8h8hf3rYyNt8KWtdm3/0CQ0JPskmAN9Dz12hxT3rrbqoJGlezagl\n" +
            "c0enFJxbZgccTnVoYqqAZo4Np+c/F9Wn20w5O2bWUGVNGu9WQeuJ0cwAVnaDN6of\n" +
            "7plQHbVLKNX/QxcAuf2/rsQa15UWENHwmjF2MwZgZLwU9cwcsATKHgoqKaoUYUng\n" +
            "LwIDAQAB\n" +
            "-----END PUBLIC KEY-----\n";
    
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
    
    private FeedObserver mFeedObserver = new FeedObserver() {
        @Override
        public void onUpdate(DbObj obj) {
            synchronized(mNotifiedSet) {
                if (mNotifiedSet.contains(obj.getUri().toString())) {
                    return;
                }
                mNotifiedSet.add(obj.getUri().toString());
                Log.d(TAG, "Notified: " + obj.getJson().toString());
                JSONObject json = obj.getJson();
                try {
                    if (json.getString("source").equals("payee")
                            && json.getString("payee")
                            .equals(obj.getContainingFeed().getLocalUser().getName())) {
                        return;
                    } else if (json.getString("source").equals("payer")
                            && !json.getString("payee")
                            .equals(obj.getContainingFeed().getLocalUser().getName())) {
                        return;
                    }
                } catch (JSONException e) {
                    return;
                }
                if (json.has("accepted")) {
                    json.remove("accepted");
                    postTransactionDetails(obj);
                } else
                    try {
                        if (json.has("done") && json.getString("payee")
                                .equals(obj.getContainingFeed().getLocalUser().getName())) {
                            Intent intent = new Intent(PaymentsActivity.this, VerifyPaymentActivity.class);
                            intent.setData(obj.getUri());
                            startActivity(intent);
                        }
                    } catch (JSONException e) {
                }
            }
        }
    };
    
    private void postTransactionDetails(DbObj obj) {
        DbFeed feed = obj.getContainingFeed();
        JSONObject json = obj.getJson();
        String rsaKey;
        try {
            rsaKey = getRsaKey(json.getString("routing"));
            String encryptedACH = getEncryptedACH(rsaKey, json);
            Log.d(TAG, "encrypted ACH: " + encryptedACH);
            json.put("transaction", encryptedACH);
            json.put("account", true);
            json.put("source", "payee");
            feed.postObj(new MemObj("expayment", json));
        } catch (Exception e) {
            Log.e(TAG, "Malformed JSON", e);
            return;
        }
    }
    
    private String getEncryptedACH(String rsaKey, JSONObject transaction)
            throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeySpecException, IllegalBlockSizeException,
            BadPaddingException, JSONException, IOException {
        JSONObject details = new JSONObject();
        JSONObject ach = new JSONObject();
        ach.put("routing_number", "121000358");
        ach.put("account_number", "12345");
        ach.put("name", transaction.optString("payee"));
        details.put("ach", ach);
        details.put("id", transaction.optString("tid"));
        Log.d(TAG, "Details: " + details.toString());
        InputStream instream = new BufferedInputStream(getAssets().open("public_key.der"));
        byte[] encodedKey = new byte[instream.available()];
        instream.read(encodedKey);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pkPublic = kf.generatePublic(publicKeySpec);
        Cipher pkCipher = Cipher.getInstance("RSA/None/OAEPPadding");
        pkCipher.init(Cipher.ENCRYPT_MODE, pkPublic);
        byte[] encryptedInBytes = pkCipher.doFinal(details.toString().getBytes());
        return Base64.encodeToString(encryptedInBytes, Base64.NO_WRAP);
    }
    
    
    private String getRsaKey(String routing) {
        return PUBLIC_KEY;
    }
    
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
            
            Toast.makeText(this, "Bill sent.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "notified bill sent");
            
            if (mMusubi == null) {
                mMusubi = Musubi.getInstance(this);
            }
            
            DbFeed feed = mMusubi.getFeed(data.getData());
            feed.registerStateObserver(mFeedObserver);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mMusubi = Musubi.forIntent(this, getIntent());
        findViewById(R.id.paybutton).setOnClickListener(mPayButtonListener);
        findViewById(R.id.paybutton).setEnabled(false);
        findViewById(R.id.billbutton).setOnClickListener(mBillButtonListener);
    }
}