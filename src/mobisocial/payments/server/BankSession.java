package mobisocial.payments.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

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
    private static final String AUTH_PATH = "/auth";
    
    @SuppressWarnings("unused")
    private String mSessionId;
    
    private BankSession(String sessionId) {
        mSessionId = sessionId;
    }
    
    public static BankSession newInstance(String username, String password) {
        // TODO: get saved token from local store if it exists
        String sessionId = login(username, password);
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
            @SuppressWarnings("unused")
            HttpResponse response = http.execute(post);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not encode request parameters", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error sending HTTP request", e);
            return null;
        }
        
        // TODO: save the session id
        
        return null;
    }
    
    // Send a request to the server to register a user
    public static String register(String username, String password) {
        HttpClient http = new DefaultHttpClient();
        URI uri;
        try {
            uri = URIUtils.createURI(URL_SCHEME, SERVER_LOCATION, -1, REGISTER_PATH, null, null);
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
            http.execute(post);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not encode request parameters", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error sending HTTP request", e);
            return null;
        }
        
        // TODO: check if registration successful
        
        return null;
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
            @SuppressWarnings("unused")
            HttpResponse response = http.execute(post);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not encode request parameters", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error sending HTTP request", e);
            return false;
        }
        
        // TODO: check if OK
        
        return true;
    }
    
    // TODO: get a token
}
