package me.smartlonghorn.smartlonghorn;

import com.parse.ParseAnalytics;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Random methods to help with formatting and etc. Also holds constants for now.
 */
public class Utility {
    public static final String SOURCE_ENTER_KEY = "enter_key";
    public static final String SOURCE_AUTOCOMPLETE = "autocomplete";
    public static final String SOURCE_POPULAR = "popular_list";
    public static final String QUESTION_EXTRA = "QUESTION";

    /**
     * Trims trailing whitespace. Removes any of these characters:
     * 0009, HORIZONTAL TABULATION
     * 000A, LINE FEED
     * 000B, VERTICAL TABULATION
     * 000C, FORM FEED
     * 000D, CARRIAGE RETURN
     * 001C, FILE SEPARATOR
     * 001D, GROUP SEPARATOR
     * 001E, RECORD SEPARATOR
     * 001F, UNIT SEPARATOR
     *
     * @return "" if source is null, otherwise string with all trailing whitespace removed
     */
    public static CharSequence trimTrailingWhitespace(CharSequence source) {

        if (source == null)
            return "";

        int i = source.length();

        // loop back to the first non-whitespace character
        while (--i >= 0 && Character.isWhitespace(source.charAt(i))) {
        }

        return source.subSequence(0, i + 1);
    }

    /**
     * Log that a question was asked in Parse, specifying where it was asked from.
     */
    public static void logQuestionAsked(String query, String source) {
        HashMap<String, String> dimensions = new HashMap<>();
        dimensions.put("query", query);
        dimensions.put("source", source);
        ParseAnalytics.trackEventInBackground("question", dimensions);
    }

    /**
     * Log that a question had no correct answers.
     *
     * @param responses list of responses given to the user; may be empty if all under threshold.
     */
    public static void logUnanswered(String query, ArrayList<String> responses) {
        HashMap<String, String> dimensions = new HashMap<>();
        dimensions.put("query", query);
        dimensions.put("response1", (responses.isEmpty() ? "NONE" : responses.get(0)));
        dimensions.put("response2", (responses.size() < 2 ? "NONE" : responses.get(1)));
        dimensions.put("response3", (responses.size() < 3 ? "NONE" : responses.get(2)));
        ParseAnalytics.trackEventInBackground("unanswered", dimensions);
    }
}
