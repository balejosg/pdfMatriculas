# Acrobat Reader Validation Report

## Metadata

- PDF file: `debug/acrobat-reader/matricula_preview-hidden_20260317-1940_f16e004.pdf`
- Git branch: `iter/windows-baseline-smoke`
- Git commit: `f16e0047ef19b82b0c5b29bf2bab194b8ba3b0df`
- Acrobat Reader version: `Adobe Acrobat 25.1.21288.0` (`FileVersion 25.1.21288.0`)
- Tester machine: `vboxuser` on `Windows 10 IoT Enterprise LTSC 2024 2009 64-bit`
- Test date: `2026-03-17`
- Final status: `FAIL`

## Open Behavior

- Startup result: Opened in Adobe Acrobat Reader without an `Error init PDF` startup alert from the document itself.
- Any init alert text: `none on open`
- Can edit fields: `no`

## Main Academic Cascade

- Scenario tested: Page-2 default ESO path; focused rerun on `txtEstudios` and the top academic control strip.
- Result: `FAIL`
- Notes: Clicking the rendered `txtEstudios` value on page 2 still did not enter the combo field reliably. Acrobat surfaced page/image actions such as `Recognize text` / `Edit image` and the AI assistant surface instead of a clean form-widget interaction. I could not complete a reliable study-type change from the rendered page-2 strip.

## Child Academic Cascade

- Scenario tested: Attempted follow-up smoke on the `child_` study selectors after the main page-2 strip check.
- Result: `FAIL`
- Notes: Blocked by the same page-2 interactivity failure. Because `txtEstudios` and the surrounding control strip were not reliably operable from the rendered surface, I could not reach a dependable child-flow cascade pass.

## Summary / Preview

- Result: `FAIL`
- Notes: Clicking `Vista previa` left the page in the same edit-state rendering. The button caption did not visibly change to `Editar`, the printable summary did not replace the edit UI, and no Acrobat JavaScript alert appeared.

## Validation

- Invalid-state result: `FAIL`
- Valid-state result: `not reached`
- Notes: Clicking `Validar` on the rendered page-2 strip did not produce the expected required-field alert (for example the initial `El campo 1er Apellido es obligatorio` message from the form JS). No success alert or Acrobat JavaScript error text appeared either; the click behaved as non-operable from the page surface.

## Signature Import

- `sig_alumno`: `not confirmed` in this rerun. A rendered-surface click near the visible signature area again triggered Acrobat page/image affordances rather than a clearly confirmed import-dialog flow.
- `sig_tutor1`: `not reached`
- `sig_tutor2`: `not reached`

## Findings

- Finding 1: The page-2 academic control strip is still intercepted by Acrobat page/image actions; `txtEstudios` is not reliably clickable as a live form widget.
- Finding 2: `btnValidate` and `btnTogglePreview` remain non-operable from the rendered page-2 surface, so validation and preview smoke behavior still fail.
- Finding 3: The rerun did not show a PDF init alert, but Acrobat continued surfacing non-form page/image/AI affordances during page-2 clicks, so the regression still appears localized to rendered-surface hit-testing / widget access.

## Reproduction Steps

1. Open `debug/acrobat-reader/matricula_preview-hidden_20260317-1940_f16e004.pdf` in Adobe Acrobat Reader and navigate to page 2.
2. Click the rendered `txtEstudios`, `Validar`, or `Vista previa` controls in the page-2 academic strip.
3. Observe that the expected combo/button behavior does not occur; Acrobat instead exposes page/image actions (`Recognize text`, `Edit image`) and the form state does not advance.

## Notes For Linux Fix

- Which field names or buttons were involved: `txtEstudios`, the page-2 academic control strip, `btnValidate`, `btnTogglePreview`, and the downstream `child_` academic flow.
- Whether the issue looks deterministic: `yes`
- Suggested focus area if obvious: Re-check page-2 widget hit-testing / annotation stacking / rendered-surface click routing around the academic strip, because the rerun still behaves like Acrobat is selecting the page/image layer instead of the widgets.
