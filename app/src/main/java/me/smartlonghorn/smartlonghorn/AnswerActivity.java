package me.smartlonghorn.smartlonghorn;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
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
    private ArrayList<String> responseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = this.getClass().getName();
        setContentView(R.layout.activity_answer);
        ButterKnife.bind(this);

        mWatsonQueryString = getIntent().getStringExtra(Utility.QUESTION_EXTRA);
        questionText.setText(mWatsonQueryString);

        responseList = new ArrayList<>();

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

        Utility.logUnanswered(mWatsonQueryString, responseList);
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
                connection.setConnectTimeout(timeoutConnection);
                connection.setReadTimeout(timeoutConnection);

                // Watson specific HTTP headers
                connection.setRequestProperty("X-SyncTimeout", "30");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", "Basic " + getEncodedCredentials());
                connection.setRequestProperty("Content-Type", "application/json");

                OutputStream out = connection.getOutputStream();
                String query = "{\"question\": {\"formattedAnswer\": true, \"questionText\": \"" + mWatsonQueryString + "\", \"items\": 5}}";
                out.write(query.getBytes());
                out.close();

                int responseCode;
                if (connection != null) {
                    responseCode = connection.getResponseCode();
                    Log.i(TAG, "Server Response Code: " + Integer.toString(responseCode));

                    if (responseCode != 200) {
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
                JSONArray evidenceArray = question.getJSONArray("answers");

                Log.d(TAG, "Answers: " + evidenceArray.toString(1));

                for (int i = 0; i < evidenceArray.length(); i++) {
                    JSONObject responseObject = evidenceArray.getJSONObject(i);
                    if (responseObject.length() == 0 || responseObject.getString("text").equals("${noAnswer}")) {
                        // Don't do anything, there are random blank answers provided... -_-
                        continue;
                    }
                    responses.add(responseObject.getString("formattedText"));
                    scores.add(Double.parseDouble(responseObject.getString("confidence")));
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
                    String response = responses.get(i);
                    responseList.add(response);

                    // Remove the (not necessarily relevant and very large) header on each answer
                    response = response.substring(response.indexOf("</h1>") + 6).replace("<span>\n</span>", "");
                    Spanned text = Html.fromHtml(response);
                    URLSpan[] currentSpans = text.getSpans(0, text.length(), URLSpan.class);

                    // Get rid of extra newlines that result from Html.fromHtml()
                    SpannableString buffer = new SpannableString(Utility.trimTrailingWhitespace(text));

                    // Part 1 of workaround to make both href and regular links/phone #s/etc. clickable
                    Linkify.addLinks(buffer, Linkify.ALL);
                    for (URLSpan span : currentSpans) {
                        int end = text.getSpanEnd(span);
                        int start = text.getSpanStart(span);
                        buffer.setSpan(span, start, end, 0);
                    }
                    switch (i) {
                        case 0:
                            responseOne.setText(buffer);
                            responseOne.setMovementMethod(LinkMovementMethod.getInstance()); // Part 2 of above workaround
                            cardOne.setVisibility(View.VISIBLE);
                            break;
                        case 1:
                            responseTwo.setText(buffer);
                            responseTwo.setMovementMethod(LinkMovementMethod.getInstance());
                            cardTwo.setVisibility(View.VISIBLE);
                            break;
                        case 2:
                            responseThree.setText(buffer);
                            responseThree.setMovementMethod(LinkMovementMethod.getInstance());
                            cardThree.setVisibility(View.VISIBLE);
                            break;
                    }
                }
            }
        }
    }
}
