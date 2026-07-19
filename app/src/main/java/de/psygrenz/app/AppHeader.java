package de.psygrenz.app;

import android.app.*;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
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
                    String info = "PsyGrenz ist Ihr Wegweiser durch die psychowissenschaftlichen Grenzgebiete: medial überlieferte Botschaften, die Licht auf den Sinn des Lebens und die Frage nach Tod und Wiedergeburt werfen.\n\n" +
                                    "Die PsyGrenz-App enthält alle medialen Schriften (Protokolle), die auch auf der Internetseite psygrenz.de veröffentlicht sind. Alle Inhalte stehen vollständig zum Lesen zur Verfügung – auch ohne Internetverbindung.\n\n" +
                                    "Ergänzende Informationen, weitere Materialien und Hintergründe finden Sie auf unserer Webseite: www.psygrenz.de";
                    SpannableString linkedInfo = new SpannableString(info);
                    String linkText = "www.psygrenz.de";
                    int linkStart = info.lastIndexOf(linkText);
                    linkedInfo.setSpan(new URLSpan("https://www.psygrenz.de"), linkStart, linkStart + linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Über diese App")
                            .setMessage(linkedInfo)
                            .setPositiveButton("Schließen", (DialogInterface d, int w) -> d.dismiss()).create();
                    dialog.setOnShowListener(ignored -> {
                        TextView message = dialog.findViewById(android.R.id.message);
                        if (message != null) {
                            message.setMovementMethod(LinkMovementMethod.getInstance());
                            message.setLinkTextColor(Color.rgb(128, 0, 128));
                        }
                    });
                    dialog.show();
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
