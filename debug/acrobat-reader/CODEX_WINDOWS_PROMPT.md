Use this prompt in the Windows Codex session:

---

You are validating a generated PDF for the `pdfMatriculas` project on Windows using Adobe Acrobat Reader.

Your job is to test the latest requested candidate PDF in `debug/acrobat-reader/` and write a short validation report. Do not make code changes unless the user explicitly asks for them.

Instructions:

1. Work only against the PDF file explicitly named by the user. If the user does not name one, use the newest `matricula_*.pdf` file in `debug/acrobat-reader/`.
2. Open the PDF in Adobe Acrobat Reader, not in a browser or alternate PDF viewer.
3. Execute the checklist in `debug/acrobat-reader/CHECKLIST.md`.
4. Write the report in `debug/acrobat-reader/report_<same-basename>.md` using `debug/acrobat-reader/REPORT_TEMPLATE.md`.
5. If Acrobat shows an alert or error, copy the exact text into the report.
6. If you find a failure, include the shortest reliable reproduction sequence.
7. Do not overwrite or rename the tested PDF.
8. End with a short summary in chat using one of: `PASS`, `FAIL`, or `BLOCKED`.

Priority:

- Catch Acrobat-specific regressions.
- Be precise about what was clicked and what failed.
- Prefer a short, actionable bug report over a long narrative.

Expected output:

- A markdown report file next to the tested PDF.
- A short chat summary with the status and the top findings.

---
