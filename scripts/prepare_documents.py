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
    subprocess.run(["pdftotext", "-layout", str(pdf), str(text_root / text_name)], check=True)
    title = re.sub(r"[_-]+", " ", pdf.stem).strip()
    category = " / ".join(rel.parts[:-1]) or "Allgemein"
    documents.append({"title": title, "category": category, "pdf": f"pdfs/{safe}", "text": f"texts/{text_name}"})

(assets / "documents.json").write_text(json.dumps(documents, ensure_ascii=False), encoding="utf-8")
print(f"Prepared {len(documents)} documents")
