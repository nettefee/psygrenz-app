package de.psygrenz.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReaderActivity extends Activity {
    private TextView body;
    private float size = 18f;
    private boolean dark = false;
    private LinearLayout root;
    private ScrollView scroll;
    private final List<Integer> matches = new ArrayList<>();
    private int currentMatch = 0;
    private TextView matchCounter;
    private Button previousMatch;
    private Button nextMatch;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String title = getIntent().getStringExtra("title");
        String textPath = getIntent().getStringExtra("text");
        String pdfPath = getIntent().getStringExtra("pdf");
        String query = getIntent().getStringExtra("query");

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        applySystemInsets(root);
        root.addView(AppHeader.create(this));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(7), dp(10), dp(7));
        header.setBackgroundColor(Color.rgb(253, 239, 255));
        Button back = headerButton("‹");
        Button home = headerButton("⌂");
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(20);
        heading.setTextColor(Color.rgb(128, 0, 128));
        heading.setPadding(dp(10), 0, 0, 0);
        header.addView(back);
        header.addView(home);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(header);

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER);
        tools.setPadding(6, 6, 6, 6);
        Button smaller = button("A−"), larger = button("A+"), mode = button("Nacht"), original = button("PDF");
        tools.addView(smaller); tools.addView(larger); tools.addView(mode); tools.addView(original);
        root.addView(tools);

        scroll = new ScrollView(this);
        body = new TextView(this);
        body.setTextSize(size);
        body.setLineSpacing(4, 1.18f);
        body.setPadding(24, 20, 24, 56);
        try {
            String documentText = readAsset(textPath);
            if (query != null && !query.trim().isEmpty()) {
                query = query.trim();
                SpannableString highlighted = new SpannableString(documentText);
                String searchableText = documentText.toLowerCase(Locale.GERMAN);
                String searchableQuery = query.toLowerCase(Locale.GERMAN);
                int position = 0;
                while ((position = searchableText.indexOf(searchableQuery, position)) >= 0) {
                    matches.add(position);
                    int end = position + query.length();
                    highlighted.setSpan(new BackgroundColorSpan(Color.rgb(244, 190, 250)), position, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    highlighted.setSpan(new StyleSpan(Typeface.BOLD), position, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    position = end;
                }
                body.setText(highlighted);
            } else body.setText(documentText);
        }
        catch (Exception e) { body.setText("Der Text konnte nicht geladen werden."); }

        LinearLayout matchNavigation = new LinearLayout(this);
        matchNavigation.setGravity(Gravity.CENTER_VERTICAL);
        matchNavigation.setPadding(dp(8), dp(5), dp(8), dp(7));
        matchNavigation.setBackgroundColor(Color.rgb(253, 239, 255));
        previousMatch = matchButton("‹  Vorheriger");
        matchCounter = new TextView(this);
        matchCounter.setGravity(Gravity.CENTER);
        matchCounter.setTextColor(Color.rgb(128, 0, 128));
        matchCounter.setTextSize(15);
        nextMatch = matchButton("Nächster  ›");
        matchNavigation.addView(previousMatch, new LinearLayout.LayoutParams(0, dp(42), 1));
        matchNavigation.addView(matchCounter, new LinearLayout.LayoutParams(dp(72), dp(42)));
        matchNavigation.addView(nextMatch, new LinearLayout.LayoutParams(0, dp(42), 1));
        matchNavigation.setVisibility(matches.isEmpty() ? View.GONE : View.VISIBLE);
        root.addView(matchNavigation);

        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        applyMode();

        if (!matches.isEmpty()) {
            updateMatchNavigation();
            scrollToMatch(0);
        }

        smaller.setOnClickListener(v -> { size = Math.max(13f, size - 2f); body.setTextSize(size); });
        larger.setOnClickListener(v -> { size = Math.min(34f, size + 2f); body.setTextSize(size); });
        mode.setOnClickListener(v -> { dark = !dark; applyMode(); });
        back.setOnClickListener(v -> finish());
        home.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });
        original.setOnClickListener(v -> {
            Intent i = new Intent(this, PdfActivity.class);
            i.putExtra("title", title); i.putExtra("pdf", pdfPath); startActivity(i);
        });
        previousMatch.setOnClickListener(v -> {
            if (currentMatch > 0) scrollToMatch(currentMatch - 1);
        });
        nextMatch.setOnClickListener(v -> {
            if (currentMatch + 1 < matches.size()) scrollToMatch(currentMatch + 1);
        });
    }

    private Button matchButton(String text) {
        Button button = button(text);
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(128, 0, 128));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE); background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(190, 78, 202));
        button.setBackground(background);
        return button;
    }

    private void scrollToMatch(int index) {
        if (index < 0 || index >= matches.size()) return;
        currentMatch = index;
        updateMatchNavigation();
        body.post(() -> {
            if (body.getLayout() != null) {
                int line = body.getLayout().getLineForOffset(matches.get(currentMatch));
                scroll.smoothScrollTo(0, Math.max(0, body.getLayout().getLineTop(line) - dp(18)));
            }
        });
    }

    private void updateMatchNavigation() {
        matchCounter.setText((currentMatch + 1) + " / " + matches.size());
        previousMatch.setEnabled(currentMatch > 0);
        nextMatch.setEnabled(currentMatch + 1 < matches.size());
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(18, 8, 18, 8);
        return b;
    }
    private Button headerButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(21); b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER); b.setIncludeFontPadding(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(190, 78, 202)); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), Color.WHITE);
        b.setBackground(bg); b.setPadding(dp(8), 0, dp(8), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(46), dp(40));
        lp.setMargins(dp(3), 0, dp(3), 0); b.setLayoutParams(lp); return b;
    }
    private void applyMode() {
        root.setBackgroundColor(dark ? Color.rgb(28, 27, 31) : Color.WHITE);
        body.setTextColor(dark ? Color.rgb(235, 225, 240) : Color.rgb(35, 32, 38));
    }
    private String readAsset(String path) throws IOException {
        try (InputStream in = getAssets().open(path); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) >= 0) out.write(b, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void applySystemInsets(View view) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
