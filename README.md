# PsyGrenz-App

Offline-Android-App für die PsyGrenz-Dokumentensammlung.

Geplant und teilweise bereits vorbereitet:

- vollständig offline nutzbar
- Suche über Titel und Dokumentinhalte
- responsive Leseransicht mit einstellbarer Schriftgröße
- Nachtmodus
- Original-PDF-Ansicht
- automatische APK-Erstellung mit GitHub Actions

Die PDFs werden nicht im Quellcode-Repository gespeichert. Der Build lädt das private Dokumentpaket aus dem GitHub-Release `documents` und bettet es in die APK ein.
