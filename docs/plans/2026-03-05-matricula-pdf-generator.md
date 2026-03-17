# Matricula PDF Generator Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the Java/PDFBox generator build reliably and produce a 2-page interactive PDF with AcroForm fields + Acrobat JavaScript.

**Architecture:** A Java 21 CLI (`GenerateMatriculaPdf`) uses PDFBox to create a fresh PDF, draws sanitized PNG backgrounds, places AcroForm widgets on top, and injects document-level Acrobat JavaScript to drive cascades/visibility/validation.

**Tech Stack:** Java 21, Maven, Apache PDFBox 2.0.30.

### Task 1: Fix JavaScript String Assembly Build Break

**Files:**
- Modify: `generator/src/main/java/com/example/matricula/GenerateMatriculaPdf.java`

**Step 1: Fix broken `sb.append(...)` blocks in `buildJavaScript()`**

Replace any multi-line Java string literals with explicit `\n` inside the string and close every `sb.append("...");` on the same Java line.

Specifically fix the three broken blocks:
- `getScenarioKey(prefix)` body
- `setUiMode(mode)` block where the double-matricula logic is currently embedded as raw JS
- `updateAll()` block where the child-scenario logic is currently embedded as raw JS

**Step 2: Fix Java escaping issues inside JS snippets**

- Escape JS regex digit class for Java: `^\\d+$` (so JS sees `^\d+$`).
- Remove or escape any literal double quotes inside Java strings (e.g. use `app.alert('El campo '+labels[n]+' es obligatorio')`).

**Step 3: Run compile to verify failure is gone**

Run: `mvn -f generator/pom.xml -DskipTests package`

Expected: the earlier "unclosed string literal" errors are gone; if new errors appear, continue with Task 2.

### Task 2: Fix Remaining Java/PDFBox Compile Errors

**Files:**
- Modify: `generator/src/main/java/com/example/matricula/GenerateMatriculaPdf.java`

**Step 1: Fix obvious signature/import mismatches**

- Ensure `addButton(...)` signature matches its call sites (UI buttons + signature buttons). Keep the last boolean as the `printed` flag.
- Import missing PDFBox types used by helper methods (notably `PDBorderStyleDictionary`).

**Step 2: Fix widget color usage for PDFBox 2.x**

PDFBox annotations use `PDColor`, not `java.awt.Color`.

Minimal safe approach for now: remove `w.setColor(Color.*)` calls (or replace with `w.setColor(new PDColor(new float[]{...}, PDDeviceRGB.INSTANCE))`).

**Step 3: Re-run build**

Run: `mvn -f generator/pom.xml -DskipTests package`

Expected: `BUILD SUCCESS` and a shaded jar in `generator/target/`.

### Task 3: Generate The PDF From The CLI

**Files:**
- Output: `matricula_interactiva.pdf` (default) or chosen `--out`

**Step 1: Run the shaded jar**

Run: `java -jar generator/target/matricula-pdf-generator-0.1.0-shaded.jar`

Expected: prints `Wrote: .../matricula_interactiva.pdf`.

### Task 4: Smoke-Verify The Output PDF Structure

**Files:**
- Verify: `matricula_interactiva.pdf`

**Step 1: Confirm the file exists and is non-trivial in size**

Run: `ls -lh matricula_interactiva.pdf`

**Step 2: Quick field presence check (best-effort)**

Open in Acrobat Reader and confirm:
- Fields are editable on top of the background.
- Academic selectors update option lists.
- "Vista previa" toggles showing the printable `ResumenAcademico` field.
- "Validar" runs validation alerts.
