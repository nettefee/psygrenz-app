package de.psygrenz.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PURPLE = Color.rgb(128, 0, 128);       // #800080
    private static final int LILAC = Color.rgb(190, 78, 202);      // #be4eca
    private static final int PALE = Color.rgb(253, 239, 255);      // #fdefff

    private final List<DocumentItem> all = new ArrayList<>();
    private final List<NavNode> roots = new ArrayList<>();
    private final Deque<NavNode> history = new ArrayDeque<>();
    private NavNode current;
    private EditText search;
    private TextView breadcrumb;
    private TextView status;
    private GridView tiles;
    private ListView documents;
    private DocumentAdapter documentAdapter;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PALE);
        applySystemInsets(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(6), dp(10), dp(6));
        top.setBackgroundColor(PURPLE);
        Button back = navButton("‹");
        Button home = navButton("⌂");
        TextView title = new TextView(this);
        title.setText("PsyGrenz");
        title.setTextColor(Color.WHITE);
        title.setTextSize(23);
        title.setPadding(dp(10), 0, 0, 0);
        top.addView(back);
        top.addView(home);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(top);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        breadcrumb = new TextView(this);
        breadcrumb.setTextSize(14);
        breadcrumb.setTextColor(PURPLE);
        breadcrumb.setPadding(dp(2), 0, dp(2), dp(8));
        content.addView(breadcrumb);

        search = new EditText(this);
        search.setHint("Alle Dokumente durchsuchen …");
        search.setSingleLine(true);
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
        showHome();

        back.setOnClickListener(v -> goBack());
        home.setOnClickListener(v -> showHome());
        documents.setOnItemClickListener((p, v, pos, id) -> openReader(documentAdapter.getItem(pos)));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { runSearch(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showHome();
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
        NavNode n = new NavNode(o.getString("title"), o.optString("path", ""), parent);
        JSONArray children = o.optJSONArray("children");
        if (children != null) for (int i = 0; i < children.length(); i++) n.children.add(parseNode(children.getJSONObject(i), n));
        return n;
    }

    private void showHome() {
        current = null;
        history.clear();
        search.setText("");
        breadcrumb.setText("Startseite");
        showTiles(roots);
    }

    private void openNode(NavNode node) {
        if (current != null) history.push(current);
        current = node;
        search.setText("");
        breadcrumb.setText(buildBreadcrumb(node));
        if (!node.children.isEmpty()) showTiles(node.children);
        else showDocuments(documentsFor(node.path));
    }

    private void goBack() {
        search.setText("");
        if (current == null) return;
        NavNode parent = current.parent;
        if (parent == null) showHome();
        else {
            current = parent;
            breadcrumb.setText(buildBreadcrumb(parent));
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
            if (current == null) showTiles(roots);
            else if (!current.children.isEmpty()) showTiles(current.children);
            else showDocuments(documentsFor(current.path));
            return;
        }
        status.setText("Suche läuft …");
        new Thread(() -> {
            List<DocumentItem> found = new ArrayList<>();
            for (DocumentItem d : all) try {
                String haystack = d.title + " " + d.category + " " + readAsset(d.textPath);
                if (haystack.toLowerCase(Locale.GERMAN).contains(q)) found.add(d);
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
        startActivity(i);
    }

    private Button navButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(22); b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.TRANSPARENT); b.setMinWidth(56); b.setMinHeight(48);
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
            tile.setText(nodes.get(p).title);
            tile.setTextSize(18); tile.setTextColor(PURPLE); tile.setGravity(Gravity.CENTER);
            tile.setLayoutParams(new AbsListView.LayoutParams(-1, dp(124)));
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
        final String title, path; final NavNode parent; final List<NavNode> children = new ArrayList<>();
        NavNode(String title, String path, NavNode parent) { this.title = title; this.path = path; this.parent = parent; }
    }
}
