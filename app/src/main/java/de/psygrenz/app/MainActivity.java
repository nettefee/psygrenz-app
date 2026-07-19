package de.psygrenz.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQUEST_BACKUP_EXPORT = 7001;
    private static final int REQUEST_BACKUP_IMPORT = 7002;
    private static final int PURPLE = Color.rgb(128, 0, 128);       // #800080
    private static final int LILAC = Color.rgb(190, 78, 202);      // #be4eca
    private static final int PALE = Color.rgb(253, 239, 255);      // #fdefff

    private final List<DocumentItem> all = new ArrayList<>();
    private final List<NavNode> roots = new ArrayList<>();
    private final Deque<NavNode> history = new ArrayDeque<>();
    private NavNode current;
    private EditText search;
    private TextView breadcrumb;
    private TextView introduction;
    private TextView status;
    private GridView tiles;
    private ListView documents;
    private DocumentAdapter documentAdapter;
    private Button backButton;
    private Button homeButton;
    private LinearLayout navigationRow;
    private boolean favoritesMode = false;
    private boolean recentMode = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PALE);
        applySystemInsets(root);

        root.addView(AppHeader.create(this));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        navigationRow = new LinearLayout(this);
        navigationRow.setOrientation(LinearLayout.HORIZONTAL);
        navigationRow.setPadding(0, 0, 0, dp(10));
        backButton = navigationButton("‹  Zurück");
        homeButton = navigationButton("⌂  Startseite");
        LinearLayout.LayoutParams navLeft = new LinearLayout.LayoutParams(0, dp(44), 1);
        navLeft.setMargins(0, 0, dp(6), 0);
        LinearLayout.LayoutParams navRight = new LinearLayout.LayoutParams(0, dp(44), 1);
        navRight.setMargins(dp(6), 0, 0, 0);
        navigationRow.addView(backButton, navLeft);
        navigationRow.addView(homeButton, navRight);
        content.addView(navigationRow);

        breadcrumb = new TextView(this);
        breadcrumb.setTextSize(14);
        breadcrumb.setTextColor(PURPLE);
        breadcrumb.setPadding(dp(2), 0, dp(2), dp(8));
        content.addView(breadcrumb);

        introduction = new TextView(this);
        introduction.setText("Willkommen bei PsyGrenz – Ihrem Wegweiser durch die psychowissenschaftlichen Grenzgebiete: medial überlieferte Botschaften, die Licht auf den Sinn des Lebens und die Frage nach Tod und Wiedergeburt werfen. Tippen Sie auf eine Kachel, um einzutauchen.");
        introduction.setTextSize(15);
        introduction.setTextColor(Color.rgb(55, 45, 58));
        introduction.setLineSpacing(dp(2), 1.08f);
        introduction.setPadding(dp(15), dp(12), dp(15), dp(12));
        introduction.setBackground(rounded(Color.WHITE, dp(12), LILAC));
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.setMargins(0, 0, 0, dp(12));
        content.addView(introduction, introParams);

        search = new EditText(this);
        search.setHint("Alle Dokumente durchsuchen …");
        search.setSingleLine(true);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        search.setTextSize(16);
        search.setPadding(dp(16), dp(8), dp(16), dp(8));
        search.setBackground(rounded(Color.WHITE, dp(12), LILAC));
        content.addView(search, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setTextColor(Color.rgb(92, 75, 96));
        status.setTextSize(14);
        status.setPadding(dp(4), dp(10), dp(4), dp(8));
        content.addView(status);

        tiles = new GridView(this);
        tiles.setNumColumns(2);
        tiles.setHorizontalSpacing(dp(14));
        tiles.setVerticalSpacing(dp(14));
        tiles.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        content.addView(tiles, new LinearLayout.LayoutParams(-1, 0, 1));

        documents = new ListView(this);
        documentAdapter = new DocumentAdapter();
        documents.setAdapter(documentAdapter);
        content.addView(documents, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        loadData();
        if (getIntent().getBooleanExtra("show_favorites", false)) showFavorites();
        else if (getIntent().getBooleanExtra("show_recent", false)) showRecent();
        else showHome();

        backButton.setOnClickListener(v -> goBack());
        homeButton.setOnClickListener(v -> showHome());
        documents.setOnItemClickListener((p, v, pos, id) -> openReader(documentAdapter.getItem(pos)));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { runSearch(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });
        search.setOnEditorActionListener((view, actionId, event) -> {
            boolean enterPressed = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || enterPressed) {
                android.view.inputmethod.InputMethodManager keyboard =
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (keyboard != null) keyboard.hideSoftInputFromWindow(search.getWindowToken(), 0);
                search.clearFocus();
                return true;
            }
            return false;
        });
        if (getIntent().getBooleanExtra("show_backup", false)) root.post(this::showBackupDialog);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("show_favorites", false)) showFavorites();
        else if (intent.getBooleanExtra("show_recent", false)) showRecent();
        else showHome();
        if (intent.getBooleanExtra("show_backup", false)) navigationRow.post(this::showBackupDialog);
    }

    @Override protected void onResume() {
        super.onResume();
        if (favoritesMode && documentAdapter != null) showDocuments(favoriteDocuments());
        else if (recentMode && documentAdapter != null) showDocuments(recentDocuments());
    }

    private void showBackupDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Datensicherung")
                .setItems(new String[]{"Sicherung erstellen", "Sicherung wiederherstellen"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        create.setType("application/json");
                        create.putExtra(Intent.EXTRA_TITLE, "PsyGrenz-Sicherung.json");
                        startActivityForResult(create, REQUEST_BACKUP_EXPORT);
                    } else {
                        Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        open.setType("application/json");
                        open.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(open, REQUEST_BACKUP_IMPORT);
                    }
                }).setNegativeButton("Abbrechen", null).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == REQUEST_BACKUP_EXPORT) exportBackup(data.getData());
            else if (requestCode == REQUEST_BACKUP_IMPORT) importBackup(data.getData());
        } catch (Exception e) {
            Toast.makeText(this, "Die Datensicherung konnte nicht verarbeitet werden.", Toast.LENGTH_LONG).show();
        }
    }

    private void exportBackup(android.net.Uri target) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", 1);
        root.put("created", System.currentTimeMillis());
        JSONObject values = new JSONObject();
        Map<String, ?> stored = getSharedPreferences("psygrenz", MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> entry : stored.entrySet()) {
            JSONObject value = new JSONObject();
            Object content = entry.getValue();
            if (content instanceof String) { value.put("type", "string"); value.put("value", content); }
            else if (content instanceof Integer) { value.put("type", "int"); value.put("value", content); }
            else if (content instanceof Float) { value.put("type", "float"); value.put("value", content); }
            else if (content instanceof Long) { value.put("type", "long"); value.put("value", content); }
            else if (content instanceof Boolean) { value.put("type", "boolean"); value.put("value", content); }
            else if (content instanceof Set) {
                value.put("type", "set"); JSONArray array = new JSONArray();
                for (Object item : (Set<?>) content) array.put(String.valueOf(item));
                value.put("value", array);
            } else continue;
            values.put(entry.getKey(), value);
        }
        root.put("values", values);
        try (OutputStream out = getContentResolver().openOutputStream(target)) {
            if (out == null) throw new IOException();
            out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        Toast.makeText(this, "PsyGrenz-Sicherung wurde erstellt.", Toast.LENGTH_LONG).show();
    }

    private void importBackup(android.net.Uri source) throws Exception {
        String json;
        try (InputStream in = getContentResolver().openInputStream(source); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IOException();
            byte[] buffer = new byte[8192]; int read;
            while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
            json = out.toString(StandardCharsets.UTF_8.name());
        }
        JSONObject root = new JSONObject(json), values = root.getJSONObject("values");
        android.content.SharedPreferences.Editor editor = getSharedPreferences("psygrenz", MODE_PRIVATE).edit().clear();
        Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next(); JSONObject value = values.getJSONObject(key);
            String type = value.getString("type");
            if (type.equals("string")) editor.putString(key, value.getString("value"));
            else if (type.equals("int")) editor.putInt(key, value.getInt("value"));
            else if (type.equals("float")) editor.putFloat(key, (float) value.getDouble("value"));
            else if (type.equals("long")) editor.putLong(key, value.getLong("value"));
            else if (type.equals("boolean")) editor.putBoolean(key, value.getBoolean("value"));
            else if (type.equals("set")) {
                JSONArray array = value.getJSONArray("value"); Set<String> set = new HashSet<>();
                for (int i = 0; i < array.length(); i++) set.add(array.getString(i));
                editor.putStringSet(key, set);
            }
        }
        editor.apply(); showHome();
        Toast.makeText(this, "PsyGrenz-Sicherung wurde wiederhergestellt.", Toast.LENGTH_LONG).show();
    }

    private void loadData() {
        try {
            JSONArray docs = new JSONArray(readAsset("documents.json"));
            for (int i = 0; i < docs.length(); i++) {
                JSONObject o = docs.getJSONObject(i);
                all.add(new DocumentItem(o.getString("title"), o.getString("category"), o.getString("pdf"), o.getString("text")));
            }
            JSONArray nav = new JSONArray(readAsset("navigation.json"));
            for (int i = 0; i < nav.length(); i++) roots.add(parseNode(nav.getJSONObject(i), null));
        } catch (Exception e) {
            Toast.makeText(this, "Bibliothek konnte nicht geladen werden.", Toast.LENGTH_LONG).show();
        }
    }

    private NavNode parseNode(JSONObject o, NavNode parent) throws Exception {
        NavNode n = new NavNode(o.getString("title"), o.optString("description", ""), o.optString("path", ""), parent);
        JSONArray children = o.optJSONArray("children");
        if (children != null) for (int i = 0; i < children.length(); i++) n.children.add(parseNode(children.getJSONObject(i), n));
        return n;
    }

    private void showHome() {
        favoritesMode = false;
        recentMode = false;
        current = null;
        history.clear();
        search.setText("");
        breadcrumb.setText("Startseite");
        navigationRow.setVisibility(View.GONE);
        introduction.setVisibility(View.VISIBLE);
        showTiles(roots);
    }

    private void showFavorites() {
        favoritesMode = true;
        recentMode = false;
        current = null;
        history.clear();
        search.setText("");
        breadcrumb.setText("Favoriten");
        navigationRow.setVisibility(View.VISIBLE);
        introduction.setVisibility(View.GONE);
        showDocuments(favoriteDocuments());
    }

    private void showRecent() {
        favoritesMode = false;
        recentMode = true;
        current = null; history.clear(); search.setText("");
        breadcrumb.setText("Zuletzt gelesen");
        navigationRow.setVisibility(View.VISIBLE);
        introduction.setVisibility(View.GONE);
        showDocuments(recentDocuments());
    }

    private List<DocumentItem> recentDocuments() {
        List<DocumentItem> recent = new ArrayList<>();
        try {
            JSONArray paths = new JSONArray(getSharedPreferences("psygrenz", MODE_PRIVATE).getString("recent", "[]"));
            for (int i = 0; i < paths.length(); i++) {
                String path = paths.getString(i);
                for (DocumentItem document : all) if (document.pdfPath.equals(path)) { recent.add(document); break; }
            }
        } catch (Exception ignored) {}
        return recent;
    }

    private List<DocumentItem> favoriteDocuments() {
        Set<String> saved = getSharedPreferences("psygrenz", MODE_PRIVATE).getStringSet("favorites", Collections.emptySet());
        List<DocumentItem> favorites = new ArrayList<>();
        for (DocumentItem document : all) if (saved.contains(document.pdfPath)) favorites.add(document);
        return favorites;
    }

    private void openNode(NavNode node) {
        if (current != null) history.push(current);
        current = node;
        search.setText("");
        breadcrumb.setText(buildBreadcrumb(node));
        navigationRow.setVisibility(View.VISIBLE);
        introduction.setVisibility(View.GONE);
        if (!node.children.isEmpty()) showTiles(node.children);
        else showDocuments(documentsFor(node.path));
    }

    private void goBack() {
        search.setText("");
        if (current == null) {
            if (favoritesMode || recentMode) showHome();
            return;
        }
        NavNode parent = current.parent;
        if (parent == null) showHome();
        else {
            current = parent;
            breadcrumb.setText(buildBreadcrumb(parent));
            navigationRow.setVisibility(View.VISIBLE);
            showTiles(parent.children);
        }
    }

    private void showTiles(List<NavNode> nodes) {
        documents.setVisibility(View.GONE);
        tiles.setVisibility(View.VISIBLE);
        tiles.setAdapter(new TileAdapter(nodes));
        status.setText(nodes.size() + (nodes.size() == 1 ? " Bereich" : " Bereiche"));
        tiles.setOnItemClickListener((p, v, pos, id) -> openNode(nodes.get(pos)));
    }

    private void showDocuments(List<DocumentItem> docs) {
        tiles.setVisibility(View.GONE);
        documents.setVisibility(View.VISIBLE);
        documentAdapter.clear();
        documentAdapter.addAll(docs);
        documentAdapter.notifyDataSetChanged();
        status.setText(docs.size() + " Dokumente");
    }

    private List<DocumentItem> documentsFor(String path) {
        List<DocumentItem> result = new ArrayList<>();
        for (DocumentItem d : all) if (d.category.equals(path) || d.category.startsWith(path + "/")) result.add(d);
        return result;
    }

    private void runSearch(String raw) {
        String q = raw.trim().toLowerCase(Locale.GERMAN);
        if (q.isEmpty()) {
            if (favoritesMode) {
                showDocuments(favoriteDocuments());
            } else if (recentMode) {
                showDocuments(recentDocuments());
            } else if (current == null) {
                introduction.setVisibility(View.VISIBLE);
                showTiles(roots);
            } else {
                introduction.setVisibility(View.GONE);
                if (!current.children.isEmpty()) showTiles(current.children);
                else showDocuments(documentsFor(current.path));
            }
            return;
        }
        introduction.setVisibility(View.GONE);
        status.setText("Suche läuft …");
        SearchQuery query = SearchQuery.parse(raw);
        Set<String> favoriteFilter = null;
        if (favoritesMode)
            favoriteFilter = new HashSet<>(getSharedPreferences("psygrenz", MODE_PRIVATE).getStringSet("favorites", Collections.emptySet()));
        else if (recentMode) {
            favoriteFilter = new HashSet<>();
            for (DocumentItem document : recentDocuments()) favoriteFilter.add(document.pdfPath);
        }
        final Set<String> activeFilter = favoriteFilter;
        new Thread(() -> {
            List<DocumentItem> found = new ArrayList<>();
            for (DocumentItem d : all) try {
                if (activeFilter != null && !activeFilter.contains(d.pdfPath)) continue;
                String haystack = d.title + " " + d.category + " " + readAsset(d.textPath);
                if (query.matches(haystack)) found.add(d);
            } catch (Exception ignored) {}
            runOnUiThread(() -> showDocuments(found));
        }).start();
    }

    private String buildBreadcrumb(NavNode n) {
        List<String> parts = new ArrayList<>();
        while (n != null) { parts.add(n.title); n = n.parent; }
        Collections.reverse(parts);
        return "Startseite  ›  " + android.text.TextUtils.join("  ›  ", parts);
    }

    private void openReader(DocumentItem d) {
        if (d == null) return;
        Intent i = new Intent(this, ReaderActivity.class);
        i.putExtra("title", d.title); i.putExtra("text", d.textPath); i.putExtra("pdf", d.pdfPath);
        i.putExtra("query", search.getText().toString().trim());
        startActivity(i);
    }

    private Button navigationButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(15); b.setTextColor(PURPLE);
        b.setAllCaps(false);
        b.setBackground(rounded(Color.WHITE, dp(12), LILAC));
        b.setMinHeight(0); b.setMinimumHeight(0);
        b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    private GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(radius); g.setStroke(2, stroke); return g;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    private final class TileAdapter extends BaseAdapter {
        private final List<NavNode> nodes;
        TileAdapter(List<NavNode> nodes) { this.nodes = nodes; }
        public int getCount() { return nodes.size(); }
        public Object getItem(int p) { return nodes.get(p); }
        public long getItemId(int p) { return p; }
        public View getView(int p, View old, ViewGroup parent) {
            TextView tile = old instanceof TextView ? (TextView) old : new TextView(MainActivity.this);
            NavNode node = nodes.get(p);
            if (node.description.isEmpty()) {
                tile.setText(node.title);
            } else {
                String text = node.title + "\n\n" + node.description;
                SpannableString styled = new SpannableString(text);
                styled.setSpan(new StyleSpan(Typeface.BOLD), 0, node.title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                int descriptionStart = node.title.length() + 2;
                styled.setSpan(new RelativeSizeSpan(0.76f), descriptionStart, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new ForegroundColorSpan(Color.rgb(70, 60, 74)), descriptionStart, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tile.setText(styled);
            }
            tile.setTextSize(18); tile.setTextColor(PURPLE); tile.setGravity(Gravity.CENTER);
            tile.setLineSpacing(dp(1), 1.05f);
            tile.setLayoutParams(new AbsListView.LayoutParams(-1, dp(node.parent == null ? 176 : 124)));
            tile.setPadding(dp(12), dp(12), dp(12), dp(12));
            tile.setBackground(rounded(Color.WHITE, dp(14), LILAC));
            return tile;
        }
    }

    private final class DocumentAdapter extends ArrayAdapter<DocumentItem> {
        DocumentAdapter() { super(MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, new ArrayList<>()); }
        @Override public View getView(int position, View convert, ViewGroup parent) {
            View row = super.getView(position, convert, parent);
            TextView title = row.findViewById(android.R.id.text1), category = row.findViewById(android.R.id.text2);
            DocumentItem d = getItem(position);
            title.setText(d == null ? "" : d.title); title.setTextSize(18); title.setTextColor(Color.rgb(35, 31, 38));
            category.setText(d == null ? "" : d.category); category.setTextSize(13); category.setTextColor(PURPLE);
            row.setPadding(14, 12, 14, 12); return row;
        }
    }

    private static final class NavNode {
        final String title, description, path; final NavNode parent; final List<NavNode> children = new ArrayList<>();
        NavNode(String title, String description, String path, NavNode parent) { this.title = title; this.description = description; this.path = path; this.parent = parent; }
    }
}
