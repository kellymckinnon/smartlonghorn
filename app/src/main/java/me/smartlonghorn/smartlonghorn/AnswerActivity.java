package me.smartlonghorn.smartlonghorn;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import butterknife.Bind;
import butterknife.ButterKnife;

public class AnswerActivity extends AppCompatActivity {

    @Bind(R.id.best_answer)
    TextView bestAnswer; // TODO convert to list of 3
    @Bind(R.id.question_text)
    TextView questionText;
    private String mWatsonQueryString = "";
    private String TAG;
    private String mWatsonAnswerString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = this.getClass().getName();
        setContentView(R.layout.activity_answer);
        ButterKnife.bind(this);

        mWatsonQueryString = getIntent().getStringExtra("QUESTION");
        questionText.setText(mWatsonQueryString);
        new WatsonQuery().execute();
    }

    private void showErrorMessage() {
        // TODO: Show a consistent message for server errors, connection errors, etc.
        // THIS IS NOT CALLED BC OF A BAD ANSWER. ONLY WHEN SOMETHING GOES WRONG. OR I
        // GUESS IF THERE ARE NO ANSWERS PROVIDED... NOT SURE IF WATSON WILL DO THAT

        bestAnswer.setText("Sorry, something went wrong. Please try again.");
    }

    /**
     * Takes a valid username and password for the Watson experience manager, which is
     * stored locally, and formats/encodes it properly.
     *
     * @return Base-64 encoded String representing the Watson credentials
     */
    private String getEncodedCredentials() {
        String textToEncode = BuildConfig.USERNAME + ":" + BuildConfig.PASSWORD;
        byte[] data = null;
        try {
            data = textToEncode.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    class WatsonQuery extends AsyncTask<Void, Integer, String> {

        /*
         *  Accepts all HTTPS certs. Do NOT use in production!!!
         *  // TODO look into this more, keeping it for now
         */
        final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {

            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
        }};
        private SSLContext context;
        private HttpsURLConnection connection;
        private String jsonData;

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected
        @Nullable
        String doInBackground(Void... ignore) {

            // establish SSL trust (insecure for demo)
            // TODO: Figure out how to make this secure? Maybe?
            try {
                context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, new java.security.SecureRandom());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            try {
                URL watsonURL = new URL(getString(R.string.watson_instance));
                int timeoutConnection = 30000;
                connection = (HttpsURLConnection) watsonURL.openConnection();
                connection.setSSLSocketFactory(context.getSocketFactory());
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setConnectTimeout(timeoutConnection);
                connection.setReadTimeout(timeoutConnection);

                // Watson specific HTTP headers
                connection.setRequestProperty("X-SyncTimeout", "30");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", "Basic " + getEncodedCredentials());
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Cache-Control", "no-cache");

                OutputStream out = connection.getOutputStream();
                String query = "{\"question\": {\"questionText\": \"" + mWatsonQueryString + "\"}}";
                out.write(query.getBytes());
                out.close();

                int responseCode;
                if (connection != null) {
                    responseCode = connection.getResponseCode();
                    Log.i(TAG, "Server Response Code: " + Integer.toString(responseCode));

                    if (responseCode != 200) {
                        showErrorMessage();
                        return null;
                    }

                    // Successful HTTP response state
                    InputStream input = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                        response.append('\r');
                    }
                    reader.close();

                    Log.i(TAG, "Watson Output: " + response.toString());
                    jsonData = response.toString();
                }
            } catch (IOException e) {
                e.printStackTrace();
                showErrorMessage();
            }

            return jsonData; // Will be null if error occurred
        }

        @Override
        protected void onPostExecute(String json) {
            if (json == null) {
                showErrorMessage();
                return;
            }

            JSONObject watsonResponse;
            try {
                watsonResponse = new JSONObject(json);
                JSONObject question = watsonResponse.getJSONObject("question");
                JSONArray evidenceArray = question.getJSONArray("evidencelist");
                JSONObject mostLikelyValue = evidenceArray.getJSONObject(0);
                mWatsonAnswerString = mostLikelyValue.get("text").toString();

                // TODO: Figure out how this works. The first answer was blank so it
                // hit the catch block. The second answer was there, but at .06% certainty
                // (this is for "Where is Jester located").

            } catch (JSONException e) {
                e.printStackTrace();
                showErrorMessage();
            }

            bestAnswer.setText(mWatsonAnswerString);
        }
    }

}
