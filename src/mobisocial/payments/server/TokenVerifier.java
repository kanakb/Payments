package mobisocial.payments.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.security.auth.x500.X500Principal;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.spongycastle.asn1.x500.RDN;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.style.BCStyle;

import android.util.Log;

public class TokenVerifier {
    public static final String TAG = "TokenVerifier";
    
    private static final String FRB_ENDPOINT =
            "https://www.frbservices.org/contacts/servlet/getDistrict?aba_number=";
    
    public static String nameForRoutingNumber(String routingNumber) {
        String url = FRB_ENDPOINT + routingNumber;
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = client.execute(httpGet);
            // Read the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                if (line.contains(new Long(routingNumber).toString())) {
                    Log.d(TAG, line);
                    return line.substring(
                            line.indexOf("My FedDirectory for ") +"My FedDirectory for ".length(),
                            line.indexOf("<br/>"));
                }
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG, "Problem with HTTP request", e);
        } catch (IOException e) {
            Log.e(TAG, "Problem with HTTP request", e);
        }
        return null;
    }
    
    public static String getCertificateOwner(String httpsUrl) {
        try {
            URL url = new URL(httpsUrl);
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            con.connect();
            InputStream is = con.getInputStream();
            is.read();
            for (Certificate cert : con.getServerCertificates()) {
                Log.d(TAG, cert.getClass().toString());
                Log.d(TAG, cert.getType());
                if (cert instanceof X509Certificate) {
                    X500Principal principal = ((X509Certificate)cert).getSubjectX500Principal();
                    Log.d(TAG, principal.toString());
                    X500Name x500name = new X500Name(principal.getName());
                    RDN[] orgs = x500name.getRDNs(BCStyle.O);
                    if (orgs.length > 0) {
                        return orgs[0].getFirst().getValue().toString();
                    }
                }
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Bad URL format", e);
        } catch (IOException e) {
            Log.e(TAG, "Problem with HTTP request", e);
        }
        return null;
    }
}
