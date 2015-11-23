package me.smartlonghorn.smartlonghorn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * The launcher activity that displays an input area for a query, as well as navigation
 * to popular/trending/recent questions.
 */
public class MainActivity extends AppCompatActivity {

    private static final int NUM_RECENT_SEARCHES_TO_DISPLAY = 5;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.search_bar)
    AutoCompleteTextView searchBar;
    @Bind(R.id.popular_questions)
    Button popularQuestions;
    @Bind(R.id.trending_questions)
    Button trendingQuestions;
    @Bind(R.id.suggestion_list)
    ListView suggestionList;

    private ArrayList<String> recentSearches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // TODO: replace with an actual list
        final ArrayList<String> questionList = new ArrayList<>();
        questionList.add("What time does the PCL close?");
        questionList.add("How do I drop a class?");
        questionList.add("Where is Jester located?");

        searchBar.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, questionList));
        searchBar.setThreshold(2); // Number of characters necessary before displaying suggestions
        searchBar.setHint("Enter a question.");
        searchBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String query = parent.getItemAtPosition(position).toString().toUpperCase();
                askQuestion(query);
            }
        });

        searchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == EditorInfo.IME_ACTION_SEARCH) {
                    askQuestion(searchBar.getText().toString().toUpperCase());
                    return true;
                }
                return false;
            }
        });

        SharedPreferences recentFile = getSharedPreferences(
                getString(R.string.prefs_name),
                Context.MODE_PRIVATE);
        recentSearches = new ArrayList<>();

        // Recent searches are stored as search0, search1, search2, etc
        for (int i = 0; i < NUM_RECENT_SEARCHES_TO_DISPLAY; i++) {
            if (recentFile.contains("search" + i)) {
                String query = recentFile.getString("search" + i, "");
                recentSearches.add(query);
            }
        }

        // Populate list with recent searches
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        suggestionList.setAdapter(listAdapter);
        listAdapter.addAll(recentSearches);
//
//        if (recentSearches.isEmpty()) {
//            noRecentSearchesText.setVisibility(View.VISIBLE);
//            suggestionList.setVisibility(View.GONE);
//        }

        suggestionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                askQuestion((String) suggestionList.getItemAtPosition(position));
            }
        });

        popularQuestions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PopularQuestionsActivity.class);
                startActivity(intent);
            }
        });

        trendingQuestions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Open trending/seasonal questions activity
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
        SharedPreferences recentFile = getSharedPreferences(
                getString(R.string.prefs_name),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = recentFile.edit();

        // Push all previous searches back one in the list
        for (int i = 0; i < recentSearches.size(); i++) {
            editor.putString("search" + (i + 1), recentSearches.get(i));
        }

        // Insert the most recent (current) search
        editor.putString("search" + 0, query);
        editor.apply();

        Intent intent = new Intent(MainActivity.this, AnswerActivity.class);
        intent.putExtra("QUESTION", query);
        startActivity(intent);
    }
}
