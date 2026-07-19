package de.psygrenz.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
    private SharedPreferences preferences;
    private String documentKey;
    private Button favoriteButton;
    private Button noteButton;
    private Button documentStartButton;
    private int returnPosition = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String title = getIntent().getStringExtra("title");
        String textPath = getIntent().getStringExtra("text");
        String pdfPath = getIntent().getStringExtra("pdf");
        String query = getIntent().getStringExtra("query");
        documentKey = pdfPath;
        preferences = getSharedPreferences("psygrenz", MODE_PRIVATE);
        size = preferences.getFloat("reader_size", 18f);
        dark = preferences.getBoolean("reader_dark", false);
        recordRecentlyRead();

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
        Button smaller = button("A−"), larger = button("A+"), mode = button(dark ? "Tag" : "Nacht"), original = button("PDF");
        tools.addView(smaller); tools.addView(larger); tools.addView(mode); tools.addView(original);
        root.addView(tools);

        LinearLayout personal = new LinearLayout(this);
        personal.setPadding(dp(8), 0, dp(8), dp(6));
        favoriteButton = matchButton("");
        documentStartButton = matchButton("↑  Anfang");
        noteButton = matchButton("");
        updatePersonalButtons();
        LinearLayout.LayoutParams personalLeft = new LinearLayout.LayoutParams(0, dp(42), 1);
        personalLeft.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams personalCenter = new LinearLayout.LayoutParams(0, dp(42), 1);
        personalCenter.setMargins(dp(4), 0, dp(4), 0);
        LinearLayout.LayoutParams personalRight = new LinearLayout.LayoutParams(0, dp(42), 1);
        personalRight.setMargins(dp(4), 0, 0, 0);
        personal.addView(favoriteButton, personalLeft);
        personal.addView(documentStartButton, personalCenter);
        personal.addView(noteButton, personalRight);
        root.addView(personal);

        scroll = new ScrollView(this);
        body = new TextView(this);
        body.setTextSize(size);
        body.setLineSpacing(4, 1.18f);
        body.setPadding(24, 20, 24, 56);
        try {
            String documentText = readAsset(textPath);
            if (query != null && !query.trim().isEmpty()) {
                SpannableString highlighted = new SpannableString(documentText);
                String searchableText = documentText.toLowerCase(Locale.GERMAN);
                SearchQuery parsedQuery = SearchQuery.parse(query);
                for (String term : parsedQuery.highlightTerms()) {
                    int position = 0;
                    while ((position = searchableText.indexOf(term, position)) >= 0) {
                        matches.add(position);
                        int end = position + term.length();
                        highlighted.setSpan(new BackgroundColorSpan(Color.rgb(244, 190, 250)), position, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        highlighted.setSpan(new StyleSpan(Typeface.BOLD), position, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        position = end;
                    }
                }
                List<Integer> uniqueMatches = new ArrayList<>(new TreeSet<>(matches));
                matches.clear(); matches.addAll(uniqueMatches);
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
        } else body.post(() -> scroll.scrollTo(0, preferences.getInt("position:" + documentKey, 0)));

        smaller.setOnClickListener(v -> { size = Math.max(13f, size - 2f); body.setTextSize(size); preferences.edit().putFloat("reader_size", size).apply(); });
        larger.setOnClickListener(v -> { size = Math.min(34f, size + 2f); body.setTextSize(size); preferences.edit().putFloat("reader_size", size).apply(); });
        mode.setOnClickListener(v -> {
            dark = !dark; applyMode(); mode.setText(dark ? "Tag" : "Nacht");
            preferences.edit().putBoolean("reader_dark", dark).apply();
        });
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
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        documentStartButton.setOnClickListener(v -> {
            if (returnPosition < 0) {
                returnPosition = scroll.getScrollY();
                scroll.scrollTo(0, 0);
                documentStartButton.setText("↩  Lesestelle");
            } else {
                scroll.scrollTo(0, returnPosition);
                returnPosition = -1;
                documentStartButton.setText("↑  Anfang");
            }
        });
        noteButton.setOnClickListener(v -> showNotes());
    }

    private void toggleFavorite() {
        Set<String> favorites = new HashSet<>(preferences.getStringSet("favorites", new HashSet<>()));
        if (favorites.contains(documentKey)) favorites.remove(documentKey); else favorites.add(documentKey);
        preferences.edit().putStringSet("favorites", favorites).apply();
        updatePersonalButtons();
    }

    private void recordRecentlyRead() {
        try {
            JSONArray old = new JSONArray(preferences.getString("recent", "[]"));
            JSONArray updated = new JSONArray();
            updated.put(documentKey);
            for (int i = 0; i < old.length() && updated.length() < 20; i++) {
                String path = old.getString(i);
                if (!path.equals(documentKey)) updated.put(path);
            }
            preferences.edit().putString("recent", updated.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void showNotes() {
        List<AnchoredNote> notes = loadNotes();
        if (notes.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Notizen")
                    .setMessage("Zu diesem Dokument gibt es noch keine Notizen.")
                    .setNegativeButton("Schließen", null)
                    .setPositiveButton("Neue Notiz hier", (dialog, which) -> editAnchoredNote(null))
                    .show();
            return;
        }
        String[] labels = new String[notes.size()];
        for (int i = 0; i < notes.size(); i++)
            labels[i] = excerptAt(notes.get(i).offset) + "\n„" + notes.get(i).text + "“";
        new android.app.AlertDialog.Builder(this)
                .setTitle("Notizen zu diesem Dokument")
                .setItems(labels, (dialog, which) -> showNoteActions(notes.get(which)))
                .setNegativeButton("Schließen", null)
                .setPositiveButton("Neue Notiz hier", (dialog, which) -> editAnchoredNote(null))
                .show();
    }

    private void showNoteActions(AnchoredNote note) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(excerptAt(note.offset))
                .setMessage(note.text)
                .setNeutralButton("Löschen", (dialog, which) -> deleteAnchoredNote(note))
                .setNegativeButton("Bearbeiten", (dialog, which) -> editAnchoredNote(note))
                .setPositiveButton("Zur Textstelle", (dialog, which) -> jumpToOffset(note.offset))
                .show();
    }

    private void editAnchoredNote(AnchoredNote existing) {
        int anchorOffset = existing == null ? currentTextOffset() : existing.offset;
        EditText input = new EditText(this);
        input.setText(existing == null ? "" : existing.text);
        input.setHint("Deine Notiz zu dieser Textstelle …");
        input.setMinLines(5); input.setGravity(Gravity.TOP);
        input.setPadding(dp(18), dp(12), dp(18), dp(12));
        new android.app.AlertDialog.Builder(this)
                .setTitle(existing == null ? "Neue Notiz an dieser Stelle" : "Notiz bearbeiten")
                .setView(input)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    List<AnchoredNote> notes = loadNotes();
                    if (existing == null) notes.add(new AnchoredNote(anchorOffset, text));
                    else {
                        for (AnchoredNote note : notes)
                            if (note.id.equals(existing.id)) { note.text = text; break; }
                    }
                    saveNotes(notes);
                    updatePersonalButtons();
                }).show();
    }

    private void deleteAnchoredNote(AnchoredNote target) {
        List<AnchoredNote> notes = loadNotes();
        for (int i = notes.size() - 1; i >= 0; i--)
            if (notes.get(i).id.equals(target.id)) notes.remove(i);
        saveNotes(notes); updatePersonalButtons();
    }

    private List<AnchoredNote> loadNotes() {
        List<AnchoredNote> notes = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString("notes:" + documentKey, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                notes.add(new AnchoredNote(item.optString("id", UUID.randomUUID().toString()), item.optInt("offset", 0), item.optString("text", "")));
            }
        } catch (Exception ignored) {}
        String oldNote = preferences.getString("note:" + documentKey, "").trim();
        if (notes.isEmpty() && !oldNote.isEmpty()) {
            notes.add(new AnchoredNote(0, oldNote));
            saveNotes(notes);
            preferences.edit().remove("note:" + documentKey).apply();
        }
        return notes;
    }

    private void saveNotes(List<AnchoredNote> notes) {
        JSONArray array = new JSONArray();
        try {
            for (AnchoredNote note : notes) {
                JSONObject item = new JSONObject();
                item.put("id", note.id); item.put("offset", note.offset); item.put("text", note.text);
                array.put(item);
            }
        } catch (Exception ignored) {}
        preferences.edit().putString("notes:" + documentKey, array.toString()).apply();
    }

    private int currentTextOffset() {
        if (body.getLayout() == null) return 0;
        int vertical = Math.max(0, scroll.getScrollY() + dp(24));
        int line = body.getLayout().getLineForVertical(vertical);
        return body.getLayout().getLineStart(line);
    }

    private String excerptAt(int offset) {
        String text = body.getText().toString();
        if (text.isEmpty()) return "Textstelle";
        offset = Math.max(0, Math.min(offset, text.length()));
        int start = Math.max(0, offset - 28), end = Math.min(text.length(), offset + 72);
        String excerpt = text.substring(start, end).replaceAll("\\s+", " ").trim();
        return (start > 0 ? "…" : "") + excerpt + (end < text.length() ? "…" : "");
    }

    private void jumpToOffset(int offset) {
        body.post(() -> {
            if (body.getLayout() == null) return;
            int safeOffset = Math.max(0, Math.min(offset, body.length()));
            int line = body.getLayout().getLineForOffset(safeOffset);
            scroll.smoothScrollTo(0, Math.max(0, body.getLayout().getLineTop(line) - dp(18)));
        });
    }

    private void updatePersonalButtons() {
        boolean favorite = preferences.getStringSet("favorites", new HashSet<>()).contains(documentKey);
        int noteCount = loadNotes().size();
        favoriteButton.setText(favorite ? "★  Favorit" : "☆  Favorit");
        noteButton.setText(noteCount == 0 ? "✎  Notizen" : "✎  Notizen (" + noteCount + ")");
    }

    @Override protected void onPause() {
        if (scroll != null && documentKey != null)
            preferences.edit().putInt("position:" + documentKey,
                    returnPosition >= 0 ? returnPosition : scroll.getScrollY()).apply();
        super.onPause();
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

    private static final class AnchoredNote {
        final String id;
        final int offset;
        String text;
        AnchoredNote(int offset, String text) { this(UUID.randomUUID().toString(), offset, text); }
        AnchoredNote(String id, int offset, String text) { this.id = id; this.offset = offset; this.text = text; }
    }
}
