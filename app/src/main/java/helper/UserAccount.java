package helper;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class UserAccount {

    private static final String MY_PREFS_NAME = "CMS.userAccount3";
    private SharedPreferences prefs;

    public UserAccount(Context context) {
        prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    }


    public boolean isLoggedIn() {
        return !(prefs.getString("token", "").isEmpty());
    }

    public void setUser(UserDetail userDetail) {
        prefs.edit()
                .putString("username", userDetail.getUsername())
                .putString("token", userDetail.getToken())
                .putString("firstname", userDetail.getFirstname())
                .putString("lastname", userDetail.getLastname())
                .putString("userpictureurl", userDetail.getUserPictureUrl())
                .putInt("userid", userDetail.getUserid())
                .putString("password", userDetail.getPassword())
                .apply();
    }

    public String getToken() {
        return prefs.getString("token", "");
    }

    public String getUsername() {
        return prefs.getString("username", "");

    }

    public String getFirstName() {
        return prefs.getString("firstname", "");

    }

    public int getUserID() {
        return prefs.getInt("userid", 0);

    }

    public String getURL() {
        return prefs.getString("url", "");
    }

    public String getSitename() {
        return prefs.getString("sitename", "");
    }

    public void logout() {
        prefs.edit()
                .clear()
                .apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean("notificationEnable", false);
    }

    public void setNotificationsEnabled(boolean b) {
        prefs.edit()
                .putBoolean("notificationEnable", b)
                .apply();
    }

    public void setURL(String u) {
        prefs.edit()
                .putString("url", u)
                .apply();
    }

    public void setSitename(String u) {
        prefs.edit()
                .putString("sitename", u)
                .apply();
    }

}
