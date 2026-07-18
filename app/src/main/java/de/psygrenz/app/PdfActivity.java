package de.psygrenz.app;

import android.app.Activity;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.widget.*;
import java.io.*;

public class PdfActivity extends Activity {
    private PdfRenderer renderer;
    private ImageView image;
    private TextView counter;
    private int pageIndex = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        styleSystemBars();
        setTitle(getIntent().getStringExtra("title"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER);
        Button prev = new Button(this); prev.setText("Zurück");
        counter = new TextView(this); counter.setPadding(24, 0, 24, 0);
        Button next = new Button(this); next.setText("Weiter");
        nav.addView(prev); nav.addView(counter); nav.addView(next); root.addView(nav);
        setContentView(root);
        try { openPdf(getIntent().getStringExtra("pdf")); render(); }
        catch (Exception e) { Toast.makeText(this, "PDF konnte nicht geöffnet werden.", Toast.LENGTH_LONG).show(); }
        prev.setOnClickListener(v -> { if (pageIndex > 0) { pageIndex--; render(); } });
        next.setOnClickListener(v -> { if (renderer != null && pageIndex + 1 < renderer.getPageCount()) { pageIndex++; render(); } });
    }

    private void openPdf(String asset) throws IOException {
        File copy = new File(getCacheDir(), Integer.toHexString(asset.hashCode()) + ".pdf");
        if (!copy.exists()) try (InputStream in = getAssets().open(asset); OutputStream out = new FileOutputStream(copy)) {
            byte[] b = new byte[16384]; int n; while ((n = in.read(b)) >= 0) out.write(b, 0, n);
        }
        renderer = new PdfRenderer(ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY));
    }

    private void render() {
        if (renderer == null) return;
        try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
            int width = Math.max(1080, image.getWidth());
            int height = width * page.getHeight() / page.getWidth();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            image.setImageBitmap(bitmap);
            counter.setText((pageIndex + 1) + " / " + renderer.getPageCount());
        }
    }

    @Override protected void onDestroy() { if (renderer != null) renderer.close(); super.onDestroy(); }
    private void styleSystemBars() {
        int purple = Color.rgb(128, 0, 128);
        getWindow().setNavigationBarColor(purple); getWindow().setStatusBarColor(purple);
        if (Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null) {
            getWindow().getInsetsController().setSystemBarsAppearance(0,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS |
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        } else if (Build.VERSION.SDK_INT >= 26) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }
}
