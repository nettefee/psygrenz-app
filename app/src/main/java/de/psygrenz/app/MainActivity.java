package de.psygrenz.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private final List<DocumentItem> all = new ArrayList<>();
    private ArrayAdapter<DocumentItem> adapter;
    private EditText search;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("PsyGrenz-App");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 16, 24, 16);
        root.setBackgroundColor(Color.rgb(248, 246, 251));
        applySystemInsets(root, 24, 16, 24, 16);

        TextView heading = new TextView(this);
        heading.setText("PsyGrenz Bibliothek");
        heading.setTextSize(27);
        heading.setTextColor(Color.rgb(75, 50, 110));
        root.addView(heading);

        TextView intro = new TextView(this);
        intro.setText("Lesen und durchsuchen – vollständig offline");
        intro.setTextSize(14);
        intro.setTextColor(Color.rgb(100, 92, 106));
        intro.setPadding(0, 3, 0, 14);
        root.addView(intro);

        search = new EditText(this);
        search.setHint("Dokumente durchsuchen …");
        search.setSingleLine(true);
        search.setTextSize(17);
        search.setPadding(20, 8, 20, 8);
        root.addView(search, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setPadding(2, 12, 2, 12);
        root.addView(status);

        ListView list = new ListView(this);
        adapter = new DocumentAdapter();
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        loadManifest();
        show(all);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { filter(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });
        list.setOnItemClickListener((p, v, pos, id) -> openReader(adapter.getItem(pos)));
    }

    private void loadManifest() {
        try {
            JSONArray array = new JSONArray(readAsset("documents.json"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                all.add(new DocumentItem(o.getString("title"), o.getString("category"), o.getString("pdf"), o.getString("text")));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Dokumentenliste konnte nicht geladen werden.", Toast.LENGTH_LONG).show();
        }
    }

    private void filter(String raw) {
        String q = raw.trim().toLowerCase(Locale.GERMAN);
        if (q.isEmpty()) { show(all); return; }
        status.setText("Suche läuft …");
        new Thread(() -> {
            List<DocumentItem> found = new ArrayList<>();
            for (DocumentItem d : all) {
                try {
                    String haystack = d.title + " " + d.category + " " + readAsset(d.textPath);
                    if (haystack.toLowerCase(Locale.GERMAN).contains(q)) found.add(d);
                } catch (Exception ignored) {}
            }
            runOnUiThread(() -> show(found));
        }).start();
    }

    private void show(List<DocumentItem> docs) {
        adapter.clear();
        adapter.addAll(docs);
        adapter.notifyDataSetChanged();
        status.setText(docs.size() + " Dokumente");
    }

    private void openReader(DocumentItem d) {
        if (d == null) return;
        Intent i = new Intent(this, ReaderActivity.class);
        i.putExtra("title", d.title);
        i.putExtra("text", d.textPath);
        i.putExtra("pdf", d.pdfPath);
        startActivity(i);
    }

    private String readAsset(String path) throws IOException {
        try (InputStream in = getAssets().open(path); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) >= 0) out.write(b, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void applySystemInsets(View view, int l, int t, int r, int b) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(l, t + insets.getSystemWindowInsetTop(), r, b + insets.getSystemWindowInsetBottom());
            return insets;
        });
    }

    private final class DocumentAdapter extends ArrayAdapter<DocumentItem> {
        DocumentAdapter() { super(MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, new ArrayList<>()); }
        @Override public View getView(int position, View convert, android.view.ViewGroup parent) {
            View row = super.getView(position, convert, parent);
            TextView title = row.findViewById(android.R.id.text1);
            TextView category = row.findViewById(android.R.id.text2);
            DocumentItem d = getItem(position);
            title.setText(d == null ? "" : d.title);
            title.setTextSize(18);
            title.setTextColor(Color.rgb(35, 31, 38));
            category.setText(d == null ? "" : d.category);
            category.setTextSize(13);
            category.setTextColor(Color.rgb(103, 80, 135));
            row.setPadding(14, 12, 14, 12);
            return row;
        }
    }
}
