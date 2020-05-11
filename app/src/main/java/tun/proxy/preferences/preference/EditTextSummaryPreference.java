package tun.proxy.preferences.preference;

import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import androidx.annotation.RequiresApi;

public class EditTextSummaryPreference extends EditTextPreference {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EditTextSummaryPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public EditTextSummaryPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EditTextSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextSummaryPreference(Context context) {
        super(context);
    }

    @Override
    public CharSequence getSummary() {
        return getText();
    }
}