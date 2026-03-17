# Acrobat Reader Validation Workflow

This folder is the handoff point between the Linux build environment and a Windows machine running Adobe Acrobat Reader.

## Goal

Use Linux for code changes, builds, and PDF generation.
Use Windows + Acrobat Reader for behavior that PDFBox tests cannot validate reliably:

- document-level Acrobat JavaScript on open
- field visibility and cascade behavior
- preview/edit mode switching
- validation alerts
- signature import buttons

## File Naming

Generate candidate PDFs in this folder with a stable, sortable name:

`matricula_<change>_<YYYYMMDD-HHMM>_<sha7>.pdf`

Example:

`matricula_bach-itinerario_20260309-1040_a1b2c3d.pdf`

Write the Windows validation report next to the PDF:

`report_<same-basename>.md`

Example:

`report_matricula_bach-itinerario_20260309-1040_a1b2c3d.md`

## Linux Side

1. Work on the same git branch that Windows will inspect.
2. Build the generator:

```bash
mvn -f generator/pom.xml -DskipTests package -Dmaven.repo.local=/tmp/m2repo
```

3. Generate the candidate PDF into this folder:

```bash
java -jar generator/target/matricula-pdf-generator-0.1.0.jar \
  --out debug/acrobat-reader/matricula_<change>_<YYYYMMDD-HHMM>_<sha7>.pdf
```

4. Tell the Windows tester exactly which PDF basename to validate.
5. Do not overwrite old candidate PDFs; keep each validation round as a separate file.

## Windows Side

1. Check out the same git branch.
2. Open the requested PDF in Adobe Acrobat Reader.
3. Follow [CHECKLIST.md](CHECKLIST.md).
4. Write the report using [REPORT_TEMPLATE.md](REPORT_TEMPLATE.md).
5. Save the report next to the tested PDF with the matching basename.

## Pass/Fail Rule

- `PASS`: no startup JS errors, core cascade behavior works, preview works, validation works, and signature import buttons are usable.
- `FAIL`: any regression that blocks normal form usage or shows Acrobat errors.
- `BLOCKED`: Reader cannot open the file correctly, the PDF is missing, or the machine cannot complete the checklist.

## Scope Discipline

Use Acrobat validation as a smoke/regression pass, not as the main development loop.

- If the Linux change only affects layout, metadata, or static PDF structure, stay on Linux.
- If the change touches JS behavior, widget actions, visibility, or signature import, run the Windows pass.
