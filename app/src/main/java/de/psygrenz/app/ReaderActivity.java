package de.psygrenz.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class ReaderActivity extends Activity {
    private TextView body;
    private float size = 18f;
    private boolean dark = false;
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String title = getIntent().getStringExtra("title");
        String textPath = getIntent().getStringExtra("text");
        String pdfPath = getIntent().getStringExtra("pdf");

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

        ScrollView scroll = new ScrollView(this);
        body = new TextView(this);
        body.setTextSize(size);
        body.setLineSpacing(4, 1.18f);
        body.setPadding(24, 20, 24, 56);
        try { body.setText(readAsset(textPath)); }
        catch (Exception e) { body.setText("Der Text konnte nicht geladen werden."); }
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        applyMode();

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
