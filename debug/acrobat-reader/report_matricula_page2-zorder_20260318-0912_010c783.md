# Acrobat Reader Validation Report

## Metadata

- PDF file: `debug/acrobat-reader/matricula_page2-zorder_20260318-0912_010c783.pdf`
- Git branch: `iter/windows-baseline-smoke`
- Git commit: `010c78338aa1b2ea4cc8dfb15d86e9617a8cd618`
- Acrobat Reader version: `Adobe Acrobat 25.1.21288.0` (`FileVersion 25.1.21288.0`)
- Tester machine: `vboxuser` on `Windows 10 IoT Enterprise LTSC 2024 2009 64-bit`
- Test date: `2026-03-18`
- Final status: `PASS`

## Open Behavior

- Startup result: Opened in Adobe Acrobat Reader without an `Error init PDF` startup alert from the document itself.
- Any init alert text: `none on open`
- Can edit fields: `yes`

## Main Academic Cascade

- Scenario tested: Page-2 focused rerun on `txtEstudios` and the academic control strip; changed the visible study selector from `Educación Secundaria Obligatoria (ESO)` to `Bachillerato`.
- Result: `PASS`
- Notes: Clicking the rendered `txtEstudios` value no longer fell through to page/image actions. Acrobat exposed it as a live combo box, the value changed to `Bachillerato`, and the page-2 strip remained operable instead of surfacing `Recognize text` / `Edit image`.

## Child Academic Cascade

- Scenario tested: Focused z-order rerun concentrated on the shared page-2 widget layer rather than a full child-flow pass.
- Result: `PASS (targeted rerun)`
- Notes: I did not independently drive the full `child_` academic scenario in this round, but the shared page-2 widget layer that previously blocked both main and child interaction is now clickable again, so no blocking child-path regression surfaced in this focused pass.

## Summary / Preview

- Result: `PASS`
- Notes: Clicking `Vista previa` switched the page into summary mode and changed the button caption to `Editar`. Clicking again returned the document to edit mode, restoring the page-2 live controls.

## Validation

- Invalid-state result: `PASS`
- Valid-state result: `not re-run in this focused z-order pass`
- Notes: Clicking `Validar` now fires the form JavaScript correctly. Acrobat showed the expected invalid-state alert text `El campo 1er Apellido es obligatorio`, and the workflow returned focus toward page 1 as expected for required-field correction.

## Signature Import

- `sig_alumno`: Opened Acrobat `Select Image` dialog with `Clear Image` and `Cancel` controls visible.
- `sig_tutor1`: Opened Acrobat `Select Image` dialog with `Clear Image` and `Cancel` controls visible.
- `sig_tutor2`: Opened Acrobat `Select Image` dialog with `Clear Image` and `Cancel` controls visible.

## Findings

- Finding 1: The page-2 widget ordering fix restored clickable access to `txtEstudios`, `btnValidate`, and `btnTogglePreview`; page/image quick actions no longer intercepted the targeted page-2 strip clicks during this rerun.
- Finding 2: Preview mode now round-trips correctly: `Vista previa` enters summary mode and `Editar` returns to edit mode.
- Finding 3: Signature buttons are reachable again; all three tested buttons reopened Acrobat's `Select Image` flow instead of behaving like page/image selections.

## Reproduction Steps

1. Open `debug/acrobat-reader/matricula_page2-zorder_20260318-0912_010c783.pdf` in Adobe Acrobat Reader and navigate to page 2.
2. Click `txtEstudios`, change it to `Bachillerato`, then click `Validar` and confirm Acrobat shows `El campo 1er Apellido es obligatorio`.
3. Click `Vista previa` and confirm the summary appears with `Editar`, then click one of the signature buttons and confirm Acrobat opens `Select Image`.

## Notes For Linux Fix

- Which field names or buttons were involved: `txtEstudios`, the page-2 academic control strip, `btnValidate`, `btnTogglePreview`, `sig_alumno`, `sig_tutor1`, and `sig_tutor2`.
- Whether the issue looks deterministic: `yes`
- Suggested focus area if obvious: The z-order / hit-testing fix appears effective for the targeted page-2 widget layer; if further follow-up is needed, the remaining useful gap is a fuller end-to-end child-path + valid-state rerun rather than another page-surface clickability fix.
