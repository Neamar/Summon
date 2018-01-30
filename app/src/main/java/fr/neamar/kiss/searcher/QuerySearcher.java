package fr.neamar.kiss.searcher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import fr.neamar.kiss.KissApplication;
import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.db.DBHelper;
import fr.neamar.kiss.db.ValuedHistoryRecord;
import fr.neamar.kiss.pojo.Pojo;
import fr.neamar.kiss.pojo.PojoWithTags;

/**
 * AsyncTask retrieving data from the providers and updating the view
 *
 * @author dorvaryn
 */
public class QuerySearcher extends Searcher {
    private final Pattern patternTagSplit = Pattern.compile("\\s+");
    private final String query;
    private HashMap<String, Integer> knownIds;
    /**
     * Store user preferences
     */
    private SharedPreferences prefs;

    public QuerySearcher(MainActivity activity, String query) {
        super(activity, query);
        this.query = query;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);

    }

    @Override
    protected int getMaxResultCount() {
        // Convert `"number-of-display-elements"` to double first before truncating to int to avoid
        // `java.lang.NumberFormatException` crashes for values larger than `Integer.MAX_VALUE`
        return (Double.valueOf(prefs.getString("number-of-display-elements", String.valueOf(DEFAULT_MAX_RESULTS)))).intValue();
    }

    @Override
    public boolean addResult(Pojo... pojos) {
        if (pojos.length > 1) {
            ArrayList<Pojo> filteredList = new ArrayList<>(pojos.length);
            for (Pojo pojo : pojos) {
                if (pojo instanceof PojoWithTags && !isTagFilterOk((PojoWithTags) pojo)) {
                    // skip this pojo
                    continue;
                }
                applyBoost(pojo);
                filteredList.add(pojo);
            }
            // call super implementation to update the adapter
            return super.addResult(filteredList.toArray(new Pojo[0]));
        } else if (pojos.length == 1) {
            Pojo pojo = pojos[0];
            if (pojo instanceof PojoWithTags && !isTagFilterOk((PojoWithTags) pojo)) {
                // skip this pojo
                return true;
            }
            applyBoost(pojo);
        }
        // call super implementation to update the adapter
        return super.addResult(pojos);
    }

    protected void applyBoost(Pojo pojo) {
        // Give a boost if item was previously selected for this query
        if (knownIds.containsKey(pojo.id)) {
            pojo.relevance += 25 * knownIds.get(pojo.id);
        }
    }

    protected boolean isTagFilterOk(PojoWithTags pojo) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return false;
        if (pojo.getTags() != null && !pojo.getTags().isEmpty()) {
            // split tags string so we can search faster
            TreeSet<String> tagList = null;
            boolean excludeTagsPresent = !activity.getExcludeTags().isEmpty();
            boolean includeTagsPresent = !activity.getIncludeTags().isEmpty();
            if ( excludeTagsPresent || includeTagsPresent )
            {
                tagList = new TreeSet<>();
                Collections.addAll(tagList, patternTagSplit.split(pojo.getTags()));
            }

            if ( excludeTagsPresent ) {
                // remove pojos that contain tags that should be hidden
                for (String tag : tagList) {
                    if (activity.getExcludeTags().contains(tag)) {
                        return false;
                    }
                }
            }
            if (includeTagsPresent) {
                // remove pojos if they don't have the include tags
                boolean bIncludeTagFound = false;
                for (String tag : activity.getIncludeTags()) {
                    if (tagList.contains(tag)) {
                        bIncludeTagFound = true;
                        break;
                    }
                }
                if (!bIncludeTagFound) {
                    return false;
                }
            }
        } else if (!activity.getIncludeTags().isEmpty()) {
            // if we have "must have" tags but the app has no tags, remove it
            return false;
        }
        return true;
    }

    /**
     * Called on the background thread
     */
    @Override
    protected Void doInBackground(Void... voids) {
        MainActivity activity = activityWeakReference.get();
        if (activity == null)
            return null;

        // Have we ever made the same query and selected something ?
        List<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(activity, query);
        knownIds = new HashMap<>();
        for (ValuedHistoryRecord id : lastIdsForQuery) {
            knownIds.put(id.record, id.value);
        }

        // Request results via "addResult"
        KissApplication.getDataHandler(activity).requestResults(query, this);
        return null;
    }
}
