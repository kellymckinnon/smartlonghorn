package me.smartlonghorn.smartlonghorn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.algolia.search.saas.APIClient;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.algolia.search.saas.listeners.SearchListener;
import com.parse.ParseAnalytics;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * The launcher activity that displays an input area for a query, as well as navigation
 * to popular/trending/recent questions.
 */
public class MainActivity extends AppCompatActivity implements SearchListener {

    public static final String TAG = "MainActivity";
    private static final int NUM_SUGGESTIONS_TO_DISPLAY = 5;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.query_bar)
    MaterialEditText searchBar;
    @Bind(R.id.popular_questions)
    Button popularQuestions;
    @Bind(R.id.trending_questions)
    Button trendingQuestions;
    @Bind(R.id.suggestion_list)
    ListView suggestionList;
    @Bind(R.id.logo_title)
    TextView title;
    @Bind(R.id.suggestion_header)
    TextView suggestionHeader;
    @Bind(R.id.no_suggestions_text)
    TextView noSuggestionsText;
    @Bind(R.id.no_recent_questions_text)
    TextView noRecentQuestionsText;
    private ArrayList<String> recentSearches;
    private ArrayAdapter<CharSequence> suggestionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        APIClient client = new APIClient(getString(R.string.algolia_app_id), getString(R.string.algolia_public_key));
        final Index index = client.initIndex(getString(R.string.algolia_table_name));

        searchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // We only want to do this once
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    return false;
                }

                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == EditorInfo.IME_ACTION_GO) {
                    String query = searchBar.getText().toString();

                    HashMap<String, String> dimensions = new HashMap<>();
                    dimensions.put("query", query);
                    dimensions.put("source", "enter_key");
                    ParseAnalytics.trackEventInBackground("question", dimensions);

                    askQuestion(query);
                    return true;
                }
                return false;
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) { // No input, show recently asked questions
                    suggestionHeader.setText(getString(R.string.recent_questions));
                    noSuggestionsText.setVisibility(View.GONE);

                    showRecentQuestions();
                } else { // There is a query, get suggestions
                    index.searchASync(new Query(s.toString()).setHitsPerPage(NUM_SUGGESTIONS_TO_DISPLAY).ignorePlural(true).removeWordsIfNoResult(Query.RemoveWordsType.REMOVE_ALLOPTIONAL), MainActivity.this);
                }

            }
        });

        SharedPreferences recentFile = getSharedPreferences(
                getString(R.string.prefs_name),
                Context.MODE_PRIVATE);
        recentSearches = new ArrayList<>();

        // Recent searches are stored as search0, search1, search2, etc
        for (int i = 0; i < NUM_SUGGESTIONS_TO_DISPLAY; i++) {
            if (recentFile.contains("search" + i)) {
                String query = recentFile.getString("search" + i, "");
                recentSearches.add(query);
            }
        }

        // Populate list with recent searches
        suggestionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        suggestionList.setAdapter(suggestionAdapter);

        if (recentSearches.isEmpty()) {
            noRecentQuestionsText.setVisibility(View.VISIBLE);
        } else {
            suggestionAdapter.addAll(recentSearches);
        }

        Typeface tf = Typeface.createFromAsset(getAssets(), getString(R.string.logo_font));
        title.setTypeface(tf);

        suggestionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String query = suggestionList.getItemAtPosition(position).toString();
                HashMap<String, String> dimensions = new HashMap<>();
                dimensions.put("query", query);
                dimensions.put("source", "suggestion_clicked");
                ParseAnalytics.trackEventInBackground("question", dimensions);
                askQuestion(query);
            }
        });

        popularQuestions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PopularQuestionsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Launch a new activity that asks the inputted question to Watson.
     *
     * @param query query to send to Watson
     */
    private void askQuestion(String query) {
        Log.d(TAG, "askQuestion() called with: " + "query = [" + query + "]");
        SharedPreferences recentFile = getSharedPreferences(
                getString(R.string.prefs_name),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = recentFile.edit();

        // Push all previous searches back one in the list
        for (int i = 0; i < recentSearches.size(); i++) {
            if (recentSearches.get(i).toLowerCase().equals(query.toLowerCase())) {
                break;
            }

            editor.putString("search" + (i + 1), recentSearches.get(i));
        }

        // Insert the most recent (current) search
        editor.putString("search" + 0, query);
        editor.apply();

        Intent intent = new Intent(MainActivity.this, AnswerActivity.class);
        intent.putExtra("QUESTION", query);
        startActivity(intent);
    }

    @Override
    public void searchResult(Index index, Query query, JSONObject results) {
        Log.d(TAG, "searchResult() called with: " + "index = [" + index + "], query = [" + query + "], results = [" + results + "]");
        ArrayList<Spanned> suggestions = new ArrayList<>();
        if (results == null || results.isNull("hits")) {
            showNoSuggestions();
            return;
        }

        try {
            JSONArray hits = results.getJSONArray("hits");
            for (int i = 0; i < hits.length(); i++) {
                String result = hits.getJSONObject(i).getJSONObject("_highlightResult").getJSONObject("text").getString("value");
                suggestions.add(Html.fromHtml(result.replace("<em>", "<strong><<font color='#914200'>").replace("</em>", "</font></strong>")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            showNoSuggestions();
        }

        if (searchBar.getText().length() == 0) {
            // Since this is async, our search may have changed/deleted
            showRecentQuestions();
        } else if (suggestions.isEmpty()) {
            showNoSuggestions();
        } else {
            suggestionAdapter.clear();
            suggestionAdapter.addAll(suggestions);
            noRecentQuestionsText.setVisibility(View.GONE);
            noSuggestionsText.setVisibility(View.GONE);
            suggestionHeader.setText(R.string.suggested_questions);
            noSuggestionsText.setVisibility(View.GONE);
            suggestionList.setVisibility(View.VISIBLE);
        }
    }

    private void showRecentQuestions() {
        Log.d(TAG, "showRecentQuestions() called");
        if (recentSearches.isEmpty()) {
            suggestionList.setVisibility(View.GONE);
            noSuggestionsText.setVisibility(View.GONE);
            noRecentQuestionsText.setVisibility(View.VISIBLE);
        } else {
            suggestionAdapter.clear();
            suggestionAdapter.addAll(recentSearches);
            suggestionList.setVisibility(View.VISIBLE);
        }
    }

    private void showNoSuggestions() {
        Log.d(TAG, "showNoSuggestions() called");
        suggestionList.setVisibility(View.GONE);
        noSuggestionsText.setVisibility(View.VISIBLE);
        noRecentQuestionsText.setVisibility(View.GONE);
    }

    @Override
    public void searchError(Index index, Query query, AlgoliaException e) {
        Log.d(TAG, "searchError() called with: " + "index = [" + index + "], query = [" + query + "], e = [" + e + "]");
        e.printStackTrace();
        showNoSuggestions();
    }
}
