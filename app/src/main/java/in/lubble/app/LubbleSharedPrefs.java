package in.lubble.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ishaan on 17/2/18.
 */

public class LubbleSharedPrefs {

    private static LubbleSharedPrefs instance;
    private final SharedPreferences preferences;
    private final String LUBBLE_SHARED_PREFERENCE_KEY = "in.lubble.mainSharedPrefs";

    private final String REFERRER_UID = "referrer_uid";
    private final String CURRENT_ACTIVE_GROUP = "CURRENT_ACTIVE_GROUP";
    private final String IS_PUBLIC_GROUP_INFO_SHOWN = "IS_PUBLIC_GROUP_INFO_SHOWN";

    private LubbleSharedPrefs(Context context) {
        preferences = context.getSharedPreferences(LUBBLE_SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
    }

    public static void initializeInstance(Context context) {
        if (instance == null) {
            instance = new LubbleSharedPrefs(context);
        }
    }

    public static LubbleSharedPrefs getInstance() {
        if (instance == null) {
            throw new IllegalStateException(LubbleSharedPrefs.class.getCanonicalName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }
        return instance;
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }

    /**
     * Clear all SharedPreferences for RiderInfo
     */
    public void clearAll() {
        preferences.edit().clear().commit();
    }

    //******************************************/

    public String getReferrerUid() {
        return preferences.getString(REFERRER_UID, "");
    }

    public boolean setReferrerUid(String uid) {

        return preferences.edit().putString(REFERRER_UID, uid).commit();
    }

    public String getCurrentActiveGroupId() {
        return preferences.getString(CURRENT_ACTIVE_GROUP, "");
    }

    public boolean setCurrentActiveGroupId(String gid) {

        return preferences.edit().putString(CURRENT_ACTIVE_GROUP, gid).commit();
    }

    public boolean getIsPublicGroupInfoShown() {
        return preferences.getBoolean(IS_PUBLIC_GROUP_INFO_SHOWN, false);
    }

    public boolean setIsPublicGroupInfoShown(boolean isShown) {

        return preferences.edit().putBoolean(IS_PUBLIC_GROUP_INFO_SHOWN, isShown).commit();
    }

}
