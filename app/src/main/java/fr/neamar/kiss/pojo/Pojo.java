package fr.neamar.kiss.pojo;

import android.util.Pair;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;

import fr.neamar.kiss.normalizer.StringNormalizer;

public abstract class Pojo {
    // Globally unique ID.
    // Usually starts with provider scheme, e.g. "app://" or "contact://" to
    // ensure unique constraint
    public String id = "(none)";

    // Name for this pojo, e.g. app name
    public String name = "";

    // Lower-cased name, for faster search
    public String nameNormalized = "";
    // Name displayed on the screen, may contain HTML (for instance, to put
    // query text in blue)
    public String displayName = "";
    // How relevant is this record ? The higher, the most probable it will be
    // displayed
    public int relevance = 0;
    // Array that contains the non-normalized positions for every normalized
    // character entry
    private int[] namePositionMap = null;
    // Tags assigned to this pojo
    public String tags;
    // Variable to store the formated (user selection in bold) tag
    public String displayTags = "";

    /**
     * Map a position in the normalized name to a position in the standard name string
     *
     * @param position Position in normalized name
     * @return Position in non-normalized string
     */
    public int mapPosition(int position) {
        if (this.namePositionMap != null) {
            if (position < this.namePositionMap.length) {
                return this.namePositionMap[position];
            } else {
                return this.name.length();
            }
        } else {
            // No mapping defined
            if (position < this.name.length()) {
                return position;
            } else {
                return this.name.length();
            }
        }
    }

    /**
     * Set the user-displayable name of this container
     * <p/>
     * When this method a searchable version of the name will be generated for the name and stored
     * as `nameNormalized`. Additionally a mapping from the positions in the searchable name
     * to the positions in the displayable name will be stored (as `namePositionMap`).
     *
     * @param name User-friendly name of this container
     */
    public void setName(String name) {
        // Set the actual user-friendly name
        this.name = name;

        if (name != null) {
            this.name = this.name.replaceAll("<", "&lt;");
            // Normalize name for faster searching
            Pair<String, int[]> normalized = StringNormalizer.normalizeWithMap(this.name);
            this.nameNormalized = normalized.first;
            this.namePositionMap = normalized.second;
        }
    }

    /**
     * Set which area of the display name should marked as highlighted in the `displayName`
     * attribute
     * <p/>
     * The start and end positions should be offsets in the normalized string and will be converted
     * to their non-normalized positions before they are used.
     *
     * @param positionNormalizedStart Highlighting start position in normalized name
     * @param positionNormalizedEnd   Highlighting end position in normalized name
     */
    public void setDisplayNameHighlightRegion(int positionNormalizedStart, int positionNormalizedEnd) {
        int positionStart = this.mapPosition(positionNormalizedStart);
        int positionEnd = this.mapPosition(positionNormalizedEnd);

        this.displayName = this.name.substring(0, positionStart)
                + '{' + this.name.substring(positionStart, positionEnd) + '}'
                + this.name.substring(positionEnd);
    }

    public void setDisplayNameHighlightRegion(List<Pair<Integer, Integer>> positions) {
        this.displayName = "";
        int lastPositionEnd = 0;
        for (Pair<Integer, Integer> position : positions) {
            int positionStart = this.mapPosition(position.first);
            int positionEnd = this.mapPosition(position.second);

            this.displayName += this.name.substring(lastPositionEnd, positionStart)
                    + '{' + this.name.substring(positionStart, positionEnd) + '}';
            lastPositionEnd = positionEnd;
        }
        this.displayName += this.name.substring(lastPositionEnd);
    }

    public void setTagHighlight(int positionStart, int positionEnd) {
        this.displayTags = this.tags.substring(0, positionStart)
                + '{' + this.tags.substring(positionStart, positionEnd) + '}'
                + this.tags.substring(positionEnd);
    }
    
    /**
     * Item comparer for sorting Pojos based on their human-readable text
     * description
     */
    public static class NameComparator implements Comparator<Pojo> {
    	private final Collator collator = Collator.getInstance();
    	
    	
        public final int compare(Pojo a, Pojo b) {
            int result = this.collator.compare(a.name, b.name);
            if(result == 0) {
            	// Fall back to ID-based ordering if names match exactly
            	result = this.collator.compare(a.id, b.id);
            }
            return result;
        }
    }
}
