package me.smartlonghorn.smartlonghorn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PopularQuestionsActivity extends AppCompatActivity {

    private static final String TAG = "PopularQuestions";
    @Bind(R.id.popular_list)
    ExpandableListView popularList;
    private HashMap<String, ArrayList<String>> questionMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_questions);
        ButterKnife.bind(this);

        questionMap = new HashMap<>();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("popular_questions");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> categoryList, ParseException e) {
                Log.d(TAG, "Retrieved: " + categoryList.size());

                for (int i = 0; i < categoryList.size(); i++) {
                    ParseObject o = categoryList.get(i);
                    String name = o.getString("category_name");
                    ArrayList<String> questions = (ArrayList<String>) o.get("question_list");
                    questionMap.put(name, questions);
                }

                Log.d(TAG, "Question map: " + questionMap.toString());

                String[] titles = questionMap.keySet().toArray(new String[questionMap.keySet().size()]);
                Arrays.sort(titles);

                SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                        PopularQuestionsActivity.this,
                        createGroupList("Category", titles),
                        android.R.layout.simple_expandable_list_item_1, // Group item layout
                        new String[]{"Category"}, // The key of group item
                        new int[]{android.R.id.text1}, // ID of each group item -- data under key goes into this TextView

                        createChildList(titles), // second-level entries
                        android.R.layout.simple_expandable_list_item_2,
                        new String[]{"Question"},
                        new int[]{android.R.id.text2}
                );

                popularList.setAdapter(adapter);
            }
        });

        popularList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                TextView tv = (TextView) v.findViewById(android.R.id.text2);
                String question = tv.getText().toString();

                Log.d(TAG, "askQuestion() called with: " + "query = [" + question + "]");
                SharedPreferences recentFile = getSharedPreferences(
                        getString(R.string.prefs_name),
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = recentFile.edit();

                ArrayList<String> recentSearches = new ArrayList<>();

                // Recent searches are stored as search0, search1, search2, etc
                for (int i = 0; i < 5; i++) {
                    if (recentFile.contains("search" + i)) {
                        String query = recentFile.getString("search" + i, "");
                        recentSearches.add(query);
                    }
                }

                // Push all previous searches back one in the list
                for (int i = 0; i < recentSearches.size(); i++) {
                    if (recentSearches.get(i).toLowerCase().equals(question.toLowerCase())) {
                        break;
                    }

                    editor.putString("search" + (i + 1), recentSearches.get(i));
                }

                // Insert the most recent (current) search
                editor.putString("search" + 0, question);
                editor.apply();

                Intent intent = new Intent(PopularQuestionsActivity.this, AnswerActivity.class);
                intent.putExtra("QUESTION", question);
                startActivity(intent);

                return true;
            }
        });
    }

    private List<List<Map<String, String>>> createChildList(String[] titles) {
        List<List<Map<String, String>>> listOfChildGroups = new ArrayList<>();
        for (String s : titles) {
            ArrayList<String> questions = questionMap.get(s);
            listOfChildGroups.add(createGroupList("Question", questions.toArray(new String[questions.size()])));
        }
        return listOfChildGroups;
    }

    /**
     * Utility method to convert a list of Strings to a list of one-item HashMaps with the same key
     * and each string as a value.
     *
     * @param key
     * @param values
     * @return
     */
    private List<Map<String, String>> createGroupList(String key, String[] values) {
        ArrayList<Map<String, String>> result = new ArrayList<>();

        for (String s : values) {
            HashMap<String, String> map = new HashMap<>();
            map.put(key, s);
            result.add(map);
        }

        return result;
    }
}
