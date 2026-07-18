package de.psygrenz.app;

final class DocumentItem {
    final String title;
    final String category;
    final String pdfPath;
    final String textPath;

    DocumentItem(String title, String category, String pdfPath, String textPath) {
        this.title = title;
        this.category = category;
        this.pdfPath = pdfPath;
        this.textPath = textPath;
    }

    @Override public String toString() {
        return title + "\n" + category;
    }
}
