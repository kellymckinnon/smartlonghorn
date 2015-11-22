package me.smartlonghorn.smartlonghorn;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PopularQuestionsActivity extends AppCompatActivity {

    @Bind(R.id.popular_list)
    ExpandableListView popularList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_questions);
        ButterKnife.bind(this);

        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                this,
                createGroupList("Category", getResources().getStringArray(R.array.categories)),
                android.R.layout.simple_expandable_list_item_1, // Group item layout
                new String[]{"Category"}, // The key of group item
                new int[]{android.R.id.text1}, // ID of each group item -- data under key goes into this TextView

                createChildList(), // second-level entries
                android.R.layout.simple_expandable_list_item_2,
                new String[]{"Question"},
                new int[]{android.R.id.text2}
        );

        popularList.setAdapter(adapter);
    }

    private List<List<Map<String, String>>> createChildList() {
        List<List<Map<String, String>>> listOfChildGroups = new ArrayList<>();

        // TODO: must manually update this when categories are updated
        listOfChildGroups.add(createGroupList("Question", getResources().getStringArray(R.array.admissions)));
        listOfChildGroups.add(createGroupList("Question", getResources().getStringArray(R.array.financial_aid)));
        listOfChildGroups.add(createGroupList("Question", getResources().getStringArray(R.array.health)));
        listOfChildGroups.add(createGroupList("Question", getResources().getStringArray(R.array.housing)));
        listOfChildGroups.add(createGroupList("Question", getResources().getStringArray(R.array.safety)));

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
