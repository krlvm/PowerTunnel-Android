package ru.krlvm.powertunnel.android.ui;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

public class NoUnderlineSpan extends UnderlineSpan {

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false);
    }

    public static Spannable stripUnderlines(Spanned spanned) {
        Spannable s = ((Spannable) spanned);
        for (URLSpan u: s.getSpans(0, s.length(), URLSpan.class)) {
            s.setSpan(new NoUnderlineSpan(), s.getSpanStart(u), s.getSpanEnd(u), 0);
        }
        return s;
    }

    public static void stripUnderlines(TextView textView) {
        Spannable s = new SpannableString(textView.getText());
        for (URLSpan u: s.getSpans(0, s.length(), URLSpan.class)) {
            s.setSpan(new NoUnderlineSpan(), s.getSpanStart(u), s.getSpanEnd(u), 0);
        }
        textView.setText(s);
    }
}