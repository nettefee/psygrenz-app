package de.psygrenz.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.*;
import java.io.*;

public class PdfActivity extends Activity {
    private PdfRenderer renderer;
    private ZoomImageView image;
    private TextView counter;
    private Button previous;
    private Button next;
    private int pageIndex = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String title = getIntent().getStringExtra("title");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(253, 239, 255));
        applySystemInsets(root);
        root.addView(AppHeader.create(this));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(7), dp(10), dp(7));
        header.setBackgroundColor(Color.rgb(253, 239, 255));
        Button back = headerButton("‹");
        Button home = headerButton("⌂");
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(Color.rgb(128, 0, 128));
        heading.setTextSize(18);
        heading.setSingleLine(true);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        heading.setPadding(dp(10), 0, 0, 0);
        header.addView(back);
        header.addView(home);
        header.addView(heading, new LinearLayout.LayoutParams(0, dp(42), 1));
        root.addView(header);

        image = new ZoomImageView(this);
        image.setBackgroundColor(Color.WHITE);
        root.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackgroundColor(Color.rgb(253, 239, 255));
        previous = pageButton("‹  Vorherige");
        counter = new TextView(this);
        counter.setGravity(Gravity.CENTER);
        counter.setTextSize(16);
        counter.setTextColor(Color.rgb(128, 0, 128));
        next = pageButton("Nächste  ›");
        nav.addView(previous, new LinearLayout.LayoutParams(0, dp(46), 1));
        nav.addView(counter, new LinearLayout.LayoutParams(dp(82), dp(46)));
        nav.addView(next, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(nav);
        setContentView(root);
        try { openPdf(getIntent().getStringExtra("pdf")); render(); }
        catch (Exception e) { Toast.makeText(this, "PDF konnte nicht geöffnet werden.", Toast.LENGTH_LONG).show(); }
        previous.setOnClickListener(v -> { if (pageIndex > 0) { pageIndex--; render(); } });
        next.setOnClickListener(v -> { if (renderer != null && pageIndex + 1 < renderer.getPageCount()) { pageIndex++; render(); } });
        back.setOnClickListener(v -> finish());
        home.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        });
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
            image.setPageBitmap(bitmap);
            counter.setText((pageIndex + 1) + " / " + renderer.getPageCount());
            previous.setEnabled(pageIndex > 0);
            next.setEnabled(pageIndex + 1 < renderer.getPageCount());
        }
    }

    private Button pageButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(Color.rgb(128, 0, 128));
        button.setMinHeight(0); button.setMinimumHeight(0);
        button.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(190, 78, 202)));
        return button;
    }

    private Button headerButton(String text) {
        Button button = new Button(this);
        button.setText(text); button.setTextSize(21); button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER); button.setIncludeFontPadding(false);
        button.setMinHeight(0); button.setMinimumHeight(0);
        button.setBackground(rounded(Color.rgb(190, 78, 202), dp(18), Color.WHITE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(40));
        params.setMargins(dp(3), 0, dp(3), 0); button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color); background.setCornerRadius(radius); background.setStroke(dp(1), stroke);
        return background;
    }

    private void applySystemInsets(View view) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class ZoomImageView extends ImageView {
        private final Matrix matrix = new Matrix();
        private final ScaleGestureDetector detector;
        private Bitmap bitmap;
        private float zoom = 1f, lastX, lastY;

        ZoomImageView(Activity activity) {
            super(activity);
            setScaleType(ScaleType.MATRIX);
            detector = new ScaleGestureDetector(activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) {
                    float wanted = Math.max(1f, Math.min(5f, zoom * d.getScaleFactor()));
                    float factor = wanted / zoom;
                    zoom = wanted;
                    matrix.postScale(factor, factor, d.getFocusX(), d.getFocusY());
                    setImageMatrix(matrix);
                    return true;
                }
            });
        }

        void setPageBitmap(Bitmap page) {
            bitmap = page;
            super.setImageBitmap(page);
            post(this::resetPage);
        }

        private void resetPage() {
            if (bitmap == null || getWidth() == 0 || getHeight() == 0) return;
            zoom = 1f;
            float fit = Math.min((float) getWidth() / bitmap.getWidth(), (float) getHeight() / bitmap.getHeight());
            float left = (getWidth() - bitmap.getWidth() * fit) / 2f;
            float top = (getHeight() - bitmap.getHeight() * fit) / 2f;
            matrix.reset(); matrix.postScale(fit, fit); matrix.postTranslate(left, top);
            setImageMatrix(matrix);
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh); post(this::resetPage);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            detector.onTouchEvent(event);
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                lastX = event.getX(); lastY = event.getY();
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && !detector.isInProgress() && zoom > 1f) {
                matrix.postTranslate(event.getX() - lastX, event.getY() - lastY);
                setImageMatrix(matrix); lastX = event.getX(); lastY = event.getY();
            }
            return true;
        }
    }

    @Override protected void onDestroy() { if (renderer != null) renderer.close(); super.onDestroy(); }
}
