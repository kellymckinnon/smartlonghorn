package me.smartlonghorn.smartlonghorn;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import butterknife.Bind;
import butterknife.ButterKnife;

public class AnswerActivity extends AppCompatActivity {

    private static final int NUM_RESPONSES_TO_DISPLAY = 3;
    @Bind(R.id.question_text)
    TextView questionText;
    @Bind(R.id.bad_answers)
    Button badAnswerButton;
    @Bind(R.id.error_message)
    TextView errorText;
    @Bind(R.id.no_answer)
    TextView noAnswerText;
    @Bind(R.id.failure_rephrase_button)
    Button failureRephraseButton;
    @Bind(R.id.failure_view_popular_button)
    Button failureViewPopularButton;
    @Bind(R.id.failure_search_site)
    Button failureSearchSite;
    @Bind(R.id.card1)
    CardView cardOne;
    @Bind(R.id.card2)
    CardView cardTwo;
    @Bind(R.id.card3)
    CardView cardThree;
    @Bind(R.id.response_1)
    TextView responseOne;
    @Bind(R.id.response_2)
    TextView responseTwo;
    @Bind(R.id.response_3)
    TextView responseThree;
    private String mWatsonQueryString = "";
    private String TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = this.getClass().getName();
        setContentView(R.layout.activity_answer);
        ButterKnife.bind(this);

        mWatsonQueryString = getIntent().getStringExtra("QUESTION");
        questionText.setText(mWatsonQueryString);

        badAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayNoAnswersUI();
                Snackbar.make(badAnswerButton, R.string.feedback_received_snackbar, Snackbar.LENGTH_LONG).show();
            }
        });

        new WatsonQuery().execute();
    }

    /**
     * If no answers are found (or the user deems them all unhelpful), an apology message
     * displays with alternate options to try (rephrase, view popular, search website)
     */
    private void displayNoAnswersUI() {
        badAnswerButton.setVisibility(View.GONE);
        cardOne.setVisibility(View.GONE);
        cardTwo.setVisibility(View.GONE);
        cardThree.setVisibility(View.GONE);
        
        noAnswerText.setVisibility(View.VISIBLE);
        failureRephraseButton.setVisibility(View.VISIBLE);
        failureRephraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        failureSearchSite.setVisibility(View.VISIBLE);
        failureSearchSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = null;
                try {
                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.utexas_search_url) + URLEncoder.encode((mWatsonQueryString), "UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                startActivity(browserIntent);
            }
        });

        failureViewPopularButton.setVisibility(View.VISIBLE);
        failureViewPopularButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AnswerActivity.this, PopularQuestionsActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * If there is an error connecting to Watson (server error or network connection error),
     * display the same choices as if none of the answers were good, but different text above it.
     */
    private void showErrorMessage() {
        displayNoAnswersUI();

        noAnswerText.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
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
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
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
                context.init(null, trustAllCerts, new SecureRandom());
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
            ArrayList<String> responses = new ArrayList<>();

            ArrayList<Double> scores = new ArrayList<>();
            try {
                watsonResponse = new JSONObject(json);
                JSONObject question = watsonResponse.getJSONObject("question");
                JSONArray evidenceArray = question.getJSONArray("evidencelist");

                for (int i = 0; i < evidenceArray.length(); i++) {
                    JSONObject responseObject = evidenceArray.getJSONObject(i);
                    if (responseObject.length() == 0) {
                        // Don't do anything, there are random blank answers provided... -_-
                        continue;
                    }
                    responses.add(responseObject.getString("text"));
                    scores.add(Double.parseDouble(responseObject.getString("value")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                showErrorMessage();
            }

            // TODO: Find the proper interval to reject all answers
            if (scores.isEmpty() || scores.get(0) < .1) {
                displayNoAnswersUI();
            } else {
                badAnswerButton.setVisibility(View.VISIBLE);

                for (int i = 0; i < Math.min(NUM_RESPONSES_TO_DISPLAY, responses.size()); i++) {
                    switch (i) {
                        case 0:
                            Linkify.addLinks(responseOne, Linkify.MAP_ADDRESSES);
                            responseOne.setText(responses.get(i));
                            cardOne.setVisibility(View.VISIBLE);
                            break;
                        case 1:
                            Linkify.addLinks(responseTwo, Linkify.MAP_ADDRESSES);
                            responseTwo.setText(responses.get(i));
                            cardTwo.setVisibility(View.VISIBLE);
                            break;
                        case 2:
                            Linkify.addLinks(responseThree, Linkify.MAP_ADDRESSES);
                            responseThree.setText(responses.get(i));
                            cardThree.setVisibility(View.VISIBLE);
                            break;
                    }
                }
            }
        }
    }
}
