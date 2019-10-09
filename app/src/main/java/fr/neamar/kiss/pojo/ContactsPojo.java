package fr.neamar.kiss.pojo;

import android.graphics.drawable.Drawable;

import java.util.concurrent.Future;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class ContactsPojo extends Pojo {

    public final String lookupKey;

    public final String phone;
    //phone without special characters
    public final StringNormalizer.Result normalizedPhone;

    public final Future<Drawable> roundedIcon;

    // Is this a primary phone?
    public final Boolean primary;

    // How many times did we phone this contact?
    public final int timesContacted;

    // Is this contact starred ?
    public final Boolean starred;

    // Is this number a home (local) number ?
    public final Boolean homeNumber;

    public StringNormalizer.Result normalizedNickname = null;

    private String nickname = "";

    public ContactsPojo(String id, String lookupKey, String phone, StringNormalizer.Result normalizedPhone,
                        Future<Drawable> icon, Future<Drawable> roundedIcon, Boolean primary,
                        int timesContacted, Boolean starred, Boolean homeNumber) {
        super(id, icon);
        this.lookupKey = lookupKey;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
        this.roundedIcon = roundedIcon;
        this.primary = primary;
        this.timesContacted = timesContacted;
        this.starred = starred;
        this.homeNumber = homeNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        if (nickname != null) {
            // Set the actual user-friendly name
            this.nickname = nickname;
            this.normalizedNickname = StringNormalizer.normalizeWithResult(this.nickname, false);
        } else {
            this.nickname = null;
            this.normalizedNickname = null;
        }
    }
}
