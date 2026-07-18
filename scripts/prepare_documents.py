#!/usr/bin/env python3
import json, pathlib, re, shutil, subprocess, sys

source = pathlib.Path(sys.argv[1])
assets = pathlib.Path(sys.argv[2])
pdf_root = assets / "pdfs"
text_root = assets / "texts"
shutil.rmtree(pdf_root, ignore_errors=True)
shutil.rmtree(text_root, ignore_errors=True)
pdf_root.mkdir(parents=True)
text_root.mkdir(parents=True)
documents = []

for index, pdf in enumerate(sorted(source.rglob("*.pdf")), 1):
    rel = pdf.relative_to(source)
    safe = f"doc_{index:03d}.pdf"
    text_name = f"doc_{index:03d}.txt"
    shutil.copy2(pdf, pdf_root / safe)
    raw_file = text_root / (text_name + ".raw")
    subprocess.run(["pdftotext", "-nopgbrk", str(pdf), str(raw_file)], check=True)
    raw = raw_file.read_text(encoding="utf-8", errors="replace")
    raw_file.unlink()

    # PDF-Zeilen sind optische Zeilen, keine echten Absätze. Für die mobile
    # Leseransicht werden weiche Zeilenumbrüche verbunden, Worttrennungen
    # repariert und nur echte Leerzeilen als Absatz beibehalten.
    raw = raw.replace("\r\n", "\n").replace("\r", "\n")
    raw = re.sub(r"(?m)^\s*-?\s*\d+\s*-?\s*$", "", raw)
    raw = re.sub(r"(?<=\w)-\n(?=[a-zäöüß])", "", raw)
    paragraphs = []
    for block in re.split(r"\n\s*\n+", raw):
        lines = [re.sub(r"\s+", " ", line).strip() for line in block.splitlines()]
        lines = [line for line in lines if line]
        if not lines:
            continue
        paragraph = " ".join(lines)
        paragraph = re.sub(r"\s+([,.;:!?])", r"\1", paragraph)
        paragraphs.append(paragraph)
    cleaned = "\n\n".join(paragraphs).strip() + "\n"
    (text_root / text_name).write_text(cleaned, encoding="utf-8")
    title = re.sub(r"[_-]+", " ", pdf.stem).strip()
    category = "/".join(rel.parts[:-1]) or "Allgemein"
    documents.append({"title": title, "category": category, "pdf": f"pdfs/{safe}", "text": f"texts/{text_name}"})

(assets / "documents.json").write_text(json.dumps(documents, ensure_ascii=False), encoding="utf-8")
print(f"Prepared {len(documents)} documents")
