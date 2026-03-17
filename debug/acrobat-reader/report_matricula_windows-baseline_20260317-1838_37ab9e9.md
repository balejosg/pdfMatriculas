# Acrobat Reader Validation Report

## Metadata

- PDF file: `debug/acrobat-reader/matricula_windows-baseline_20260317-1838_37ab9e9.pdf`
- Git branch: `iter/windows-baseline-smoke`
- Git commit: `37ab9e9a008ca9e1322a6ecb803199738bdb0a8e`
- Acrobat Reader version: `Adobe Acrobat 25.1.21288.0` (`FileVersion 25.1.21288.0`)
- Tester machine: `vboxuser` on `Microsoft Windows 11 IoT Enterprise LTSC 64-bit`
- Test date: `2026-03-17`
- Final status: `FAIL`

## Open Behavior

- Startup result: Opened in Adobe Acrobat Reader without an `Error init PDF` startup alert.
- Any init alert text: `none on open`
  Acrobat later showed a modal titled `Accessibility Setup Assistant, Screen 2 of 4` with the text:
  `The reading order for documents that are not tagged can be set. With the recommended setting the reading order is inferred from the document and font, but you can set a specific reading order.`
- Can edit fields: `no`

## Main Academic Cascade

- Scenario tested: Page 2 default ESO path; attempted to interact with `txtEstudios` and the top academic controls.
- Result: `FAIL`
- Notes: The page-2 academic controls were not reliably clickable in Reader. Repeated clicks in the `txtEstudios` / top-control strip either did nothing or selected the page image and showed Acrobat quick actions such as `Recognize text` and `Edit image` instead of opening the widget. The visible `txtEstudios` value remained `Educación Secundaria Obligatoria (ESO)`.

## Child Academic Cascade

- Scenario tested: Attempted to reach the child flow via the same page-2 control strip.
- Result: `FAIL`
- Notes: Blocked by the same page-2 interactivity failure. I could not reliably operate the top controls needed to reach or validate the `child_` flow.

## Summary / Preview

- Result: `FAIL`
- Notes: Clicking the `Vista previa` region selected the page image and showed Acrobat quick actions instead of toggling preview mode. `ResumenAcademico` never became visible.

## Validation

- Invalid-state result: `FAIL`
- Valid-state result: `not reached`
- Notes: Clicking the `Validar` region did not produce the expected required-field alert. No Acrobat JavaScript error text was shown; the control behaved as non-operable from the rendered page-2 surface.

## Signature Import

- `sig_alumno`: Opened Acrobat `Select Image` dialog. No Acrobat error shown before file selection.
- `sig_tutor1`: Opened Acrobat `Select Image` dialog. No Acrobat error shown before file selection.
- `sig_tutor2`: Opened Acrobat `Select Image` dialog. No Acrobat error shown before file selection.

## Findings

- Finding 1: Page-2 academic widgets are not reliably clickable in Acrobat Reader; the top control strip behaves like page/image selection instead of live form widgets.
- Finding 2: `Vista previa` and `Validar` are not operable from the rendered page-2 surface, so preview and validation smoke behavior cannot complete.
- Finding 3: Signature import widgets still open Acrobat's `Select Image` dialog, so the regression appears localized to the page-2 academic/top-control interaction layer rather than a total AcroForm failure.

## Reproduction Steps

1. Open `debug/acrobat-reader/matricula_windows-baseline_20260317-1838_37ab9e9.pdf` in Adobe Acrobat Reader and go to page 2.
2. Click `txtEstudios`, `Validar`, or `Vista previa` in the top control row.
3. Observe that Acrobat either does nothing or selects the page image and shows quick actions like `Recognize text` / `Edit image` instead of operating the widget.

## Notes For Linux Fix

- Which field names or buttons were involved: `txtEstudios`, the page-2 academic control strip, `btnValidate`, `btnTogglePreview`, and the child-flow entry path; signature widgets `sig_alumno`, `sig_tutor1`, and `sig_tutor2` remained reachable.
- Whether the issue looks deterministic: `yes`
- Suggested focus area if obvious: Inspect page-2 widget hit-testing / stacking / annotation interaction around the academic control strip, since the lower signature buttons still receive clicks in Reader.
