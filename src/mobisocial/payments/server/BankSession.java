package mobisocial.payments.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import mobisocial.payments.PaymentsActivity;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

/**
 * A very simple bank interface. In the future, a bank could provide its own.
 */

public class BankSession {
    public static final String TAG = "BankSession";
    
    private static final String URL_SCHEME = "https";
    private static final String SERVER_LOCATION = "expay.herokuapp.com";
    private static final String REGISTER_PATH = "/register";
    private static final String LOGIN_PATH = "/login";
    private static final String TOKEN_PATH = "/token";
    private static final String AUTH_PATH = "/auth";
    
    private String mSessionId;
    
    private BankSession(String sessionId) {
        mSessionId = sessionId;
    }
    
    public static BankSession newInstance(Context context, String username, String password) {
        // Get the session id from the store if possible, otherwise log in
        SharedPreferences p = context.getSharedPreferences(PaymentsActivity.PREFS_NAME, 0);
        String sessionId = null;
        String savedUsername = p.getString("username", null);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 not supported", e);
            return null;
        }
        md.update(password.getBytes());
        String hashedPassword = Base64.encodeToString(md.digest(), Base64.DEFAULT);
        String savedPassword = p.getString("password", null);
        if (username.equals(savedUsername) && hashedPassword.equals(savedPassword)) {
            sessionId = p.getString("session_id", null);
        }
        
        // Session id not available
        if (sessionId == null) {
            sessionId = login(username, password);
            if (sessionId != null) {
                p.edit().putString("session_id", sessionId);
                p.edit().putString("username", username);
                p.edit().putString("password", hashedPassword);
            }
        }
        return new BankSession(sessionId);
    }
    
    // Send a request to the server to get a session id
    private static String login(String username, String password) {
        HttpClient http = new DefaultHttpClient();
        URI uri;
        try {
            uri = URIUtils.createURI(URL_SCHEME, SERVER_LOCATION, -1, LOGIN_PATH, null, null);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Malformed URL", e);
            return null;
        }
        
        HttpPost post = new HttpPost(uri);
        List<NameValuePair> postData = new ArrayList<NameValuePair>();
        postData.add(new BasicNameValuePair("email", username));
        postData.add(new BasicNameValuePair("password", password));
        try {
            post.setEntity(new UrlEncodedFormEntity(postData, HTTP.UTF_8));
            HttpResponse response = http.execute(post);
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));
            String responseStr = "";
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseStr += line;
            }
            if (responseStr.equals("false")) {
                Log.w(TAG, "Problem logging in");
                return null;
            }
            
            return responseStr;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not encode request parameters", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error sending HTTP request", e);
            return null;
        }
    }
    
    // Send a request to the server to register a user
    public static boolean register(String username, String password) {
        HttpClient http = new DefaultHttpClient();
        URI uri;
        try {
            uri = URIUtils.createURI(URL_SCHEME, SERVER_LOCATION, -1, REGISTER_PATH, null, null);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Malformed URL", e);
            return false;
        }
        
        HttpPost post = new HttpPost(uri);
        List<NameValuePair> postData = new ArrayList<NameValuePair>();
        postData.add(new BasicNameValuePair("email", username));
        postData.add(new BasicNameValuePair("password", password));
        try {
            post.setEntity(new UrlEncodedFormEntity(postData, HTTP.UTF_8));
            HttpResponse response = http.execute(post);
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));
            String responseStr = "";
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseStr += line;
            }
            if (responseStr.equals("false")) {
                Log.w(TAG, "Problem registering");
                return false;
            }
            return true;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not encode request parameters", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error sending HTTP request", e);
            return false;
        }
    }
    
    // Ask the server if the token is OK
    public static boolean authorize(String token, String amount) {
        HttpClient http = new DefaultHttpClient();
        URI uri;
        try {
            uri = URIUtils.createURI(URL_SCHEME, SERVER_LOCATION, -1, AUTH_PATH, null, null);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Malformed URL", e);
            return false;
        }
        
        HttpPost post = new HttpPost(uri);
        List<NameValuePair> postData = new ArrayList<NameValuePair>();
        postData.add(new BasicNameValuePair("amount", amount));
        postData.add(new BasicNameValuePair("token", token));
        try {
            post.setEntity(new UrlEncodedFormEntity(postData, HTTP.UTF_8));
            HttpResponse response = http.execute(post);
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));
            String responseStr = "";
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseStr += line;
            }
            if (responseStr.equals("false")) {
                Log.i(TAG, "Bad token");
                return false;
            }
            Log.d(TAG, responseStr);
            return true;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not encode request parameters", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error sending HTTP request", e);
            return false;
        }
    }
    
    // Get a token
    public JSONObject getToken(String amount) {
        HttpClient http = new DefaultHttpClient();
        URI uri;
        try {
            uri = URIUtils.createURI(URL_SCHEME, SERVER_LOCATION, -1, TOKEN_PATH, null, null);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Malformed URL", e);
            return null;
        }
        
        HttpPost post = new HttpPost(uri);
        post.setHeader("Set-Cookie", "_id=" + mSessionId);
        List<NameValuePair> postData = new ArrayList<NameValuePair>();
        postData.add(new BasicNameValuePair("amount", amount));
        postData.add(new BasicNameValuePair("sid", mSessionId));
        try {
            post.setEntity(new UrlEncodedFormEntity(postData, HTTP.UTF_8));
            HttpResponse response = http.execute(post);
            
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));
            String responseStr = "";
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseStr += line;
            }
            if (responseStr.equals("false")) {
                Log.w(TAG, "Bad token request");
                return null;
            }
            Log.d(TAG, responseStr);
            return new JSONObject(responseStr);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not encode request parameters", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error sending HTTP request", e);
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
            return null;
        }
    }
}
