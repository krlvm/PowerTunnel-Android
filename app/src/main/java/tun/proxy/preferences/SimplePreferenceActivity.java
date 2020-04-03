package tun.proxy.preferences;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SimplePreferenceActivity extends AppCompatActivity {

    public static int PREFERENCES_LAST_THEME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PREFERENCES_LAST_THEME = AppCompatDelegate.getDefaultNightMode();

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SimplePreferenceFragment()).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SimplePreferenceFragment()).commit();
    }
}
