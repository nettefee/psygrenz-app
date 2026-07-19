package de.psygrenz.app;

import android.app.*;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

final class AppHeader {
    private AppHeader() {}

    static View create(Activity activity) {
        int purple = Color.rgb(128, 0, 128);
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6));
        bar.setBackgroundColor(purple);

        TextView book = new TextView(activity);
        book.setText("📖"); book.setTextSize(25); book.setGravity(Gravity.CENTER);
        TextView title = new TextView(activity);
        title.setText("PsyGrenz"); title.setTextSize(23); title.setTextColor(Color.WHITE); title.setGravity(Gravity.CENTER);
        Button menu = new Button(activity);
        menu.setText("☰"); menu.setTextSize(24); menu.setTextColor(Color.WHITE);
        menu.setGravity(Gravity.CENTER); menu.setIncludeFontPadding(false);
        menu.setBackgroundColor(Color.TRANSPARENT); menu.setMinWidth(0); menu.setMinimumWidth(0);

        bar.addView(book, new LinearLayout.LayoutParams(dp(activity, 54), dp(activity, 54)));
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(activity, 54), 1));
        bar.addView(menu, new LinearLayout.LayoutParams(dp(activity, 54), dp(activity, 54)));

        menu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(activity, menu);
            popup.getMenu().add("Info");
            popup.getMenu().add("Schließen");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().toString().equals("Info")) {
                    new AlertDialog.Builder(activity).setTitle("Über PsyGrenz")
                            .setMessage("Der Informationstext wird noch ergänzt.")
                            .setPositiveButton("Schließen", (DialogInterface d, int w) -> d.dismiss()).show();
                } else activity.finishAffinity();
                return true;
            });
            popup.show();
        });
        return bar;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
