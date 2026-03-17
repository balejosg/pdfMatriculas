# Acrobat Reader Smoke Checklist

Run this checklist against one candidate PDF in `debug/acrobat-reader/`.

## 1. Open Behavior

- Open the PDF in Adobe Acrobat Reader.
- Confirm no startup alert such as `Error init PDF: ...`.
- Confirm the form is interactive and fields are editable.

## 2. Main Academic Cascade

- Change `txtEstudios` between ESO, Bachillerato, and Ciclos Formativos.
- Confirm the correct dependent selectors appear and irrelevant ones hide.
- Confirm visible selectors populate with reasonable options and do not show stale values from the previous study type.
- If the tested change is about a specific path, exercise that path twice to catch stale-choice bugs.

## 3. Child Academic Cascade

- Repeat the same smoke test for the `child_` study selectors.
- Confirm the child flow behaves independently from the main flow.

## 4. Summary / Preview

- Make at least one change that should affect the academic summary.
- Click the preview toggle.
- Confirm the printable summary becomes visible.
- Confirm edit-only UI fields hide in preview mode.
- Click the preview toggle again and confirm the form returns to edit mode.

## 5. Validation

- Click the validation button with required fields still blank.
- Confirm Reader shows a useful validation alert instead of failing silently.
- Fill a minimal valid path for the scenario under test.
- Click validation again and confirm it succeeds without Acrobat JS errors.

## 6. Signature Buttons

- Click `sig_alumno` and try importing a signature image.
- Repeat for `sig_tutor1` and `sig_tutor2`.
- Confirm the import flow works or record the exact Reader error if it fails.

## 7. Regression Notes

- Record any alert text exactly as shown by Reader.
- Record whether the issue is deterministic or intermittent.
- Record the smallest sequence of clicks/changes needed to reproduce the problem.

## Minimum Smoke Path

If time is tight, the minimum useful pass is:

1. Open PDF and confirm no init error.
2. Change one main academic path.
3. Change one child academic path.
4. Toggle preview on and off.
5. Run validation once in an invalid state and once in a minimally valid state.
6. Test one signature import button.
