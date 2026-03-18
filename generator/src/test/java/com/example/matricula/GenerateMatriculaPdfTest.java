package com.example.matricula;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateMatriculaPdfTest {

    @Test
    void academicChoiceFieldsRefreshOnCommitWithReentryGuard() throws Exception {
        Path output = Files.createTempFile("matricula-auto-refresh", ".pdf");
        try {
            GenerateMatriculaPdf.main(new String[]{
                    "--model", "../data/model.json",
                    "--bg1", "../assets/backgrounds/bg_page1.png",
                    "--bg2", "../assets/backgrounds/bg_page2.png",
                    "--out", output.toString()
            });

            try (PDDocument document = PDDocument.load(output.toFile())) {
                PDAcroForm form = document.getDocumentCatalog().getAcroForm();
                assertNotNull(form);

                assertRefreshesOnCommit(form, "txtEstudios", "scheduleUpdateAll();");
                assertRefreshesOnCommit(form, "txtBACH_Itinerario", "scheduleUpdateAll();");
                assertRefreshesOnCommit(form, "child_txtEstudios", "scheduleUpdateAll();");
                assertDoesNotRefreshOnBlur(form, "txtEstudios", "scheduleUpdateAll();");
                assertDoesNotRefreshOnBlur(form, "txtBACH_Itinerario", "scheduleUpdateAll();");
                assertDoesNotRefreshOnBlur(form, "child_txtEstudios", "scheduleUpdateAll();");
                assertRefreshesOnBlur(form, "txtOptLibre_1", "scheduleResumen();");
                assertRefreshesOnBlur(form, "child_txtOptLibre_1", "scheduleResumen();");
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }

    @Test
    void applyScenarioSkipsEmptyOptionArrays() throws Exception {
        String js = buildJavaScript();
        assertTrue(js.contains("if(trf){ if(tro && tro.length>0){"), "troncal slots should ignore empty arrays");
        assertTrue(js.contains("if(cof){ if(coo && coo.length>0){"), "common slots should ignore empty arrays");
        assertTrue(js.contains("if(of){ if(oo && oo.length>0){"), "optional slots should ignore empty arrays");
    }

    @Test
    void dynamicChoiceFieldsAreClearedSafely() throws Exception {
        String js = buildJavaScript();
        assertTrue(js.contains("function clearChoice(fieldName){"), "JS should provide a safe clearer for choice fields");
        assertTrue(js.contains("clearChoice.call(this,prefix+'txtESO_Itinerario');"), "ESO itinerario should use safe choice clearing");
        assertTrue(js.contains("clearChoice.call(this,prefix+'optativa_'+j);"), "optional slots should use safe choice clearing");
        assertTrue(js.contains("clearChoice.call(this,prefix+'optTroncal_'+k);"), "troncal slots should use safe choice clearing");
        assertTrue(js.contains("clearChoice.call(this,prefix+'optComun_'+k);"), "common slots should use safe choice clearing");

        assertFalse(js.contains("itField2.display = display.hidden; itField2.value=''"), "choice fields should not be cleared with blank values");
        assertFalse(js.contains("of){ of.display=display.hidden; of.value=''"), "optativa slots should not set blank values directly");
        assertFalse(js.contains("tr){ tr.display=display.hidden; tr.value=''"), "troncal slots should not set blank values directly");
        assertFalse(js.contains("co){ co.display=display.hidden; co.value=''"), "common slots should not set blank values directly");
    }

    @Test
    void updateAllDoesNotRebuildStaticCombos() throws Exception {
        String js = buildJavaScript();
        assertFalse(js.contains("function updateAll(){ setupCombos.call(this);"), "updateAll should not rebuild static selectors on every blur");
        assertTrue(
                js.contains("function initMatricula(){\n  var prevSuspend = __suspendCascade; __suspendCascade = true;\n  try{\n  setPreview.call(this,false); setUiMode.call(this,'main');\n  setupCombos.call(this);"),
                "setupCombos should run during init with cascades suspended before the first refresh"
        );
    }

    @Test
    void visibleCascadeSelectorsGetExplicitDefaultValues() throws Exception {
        String js = buildJavaScript();
        assertTrue(js.contains("ensureValue.call(this,prefix+'txtESO_Cursos', SELECTORS.txtESO_Cursos);"), "ESO course selector should get an explicit default");
        assertTrue(js.contains("ensureValue.call(this,prefix+'txtBACH_Cursos', SELECTORS.txtBACH_Cursos);"), "Bachillerato course selector should get an explicit default");
        assertTrue(js.contains("ensureValue.call(this,prefix+'txtCICLOS_Cursos', SELECTORS.txtCICLOS_Cursos);"), "Ciclos course selector should get an explicit default");
        assertTrue(js.contains("ensureValue.call(this,prefix+'txtCICLOS_Grados', SELECTORS.txtCICLOS_Grados);"), "Ciclos grade selector should get an explicit default");
        assertTrue(js.contains("ensureValue.call(this,prefix+'matriculaCompleta', SELECTORS.matriculaCompleta);"), "Matricula completa should get an explicit default when visible");
    }

    @Test
    void setupCombosAssignsNonBlankDefaultsToBaseChoiceFields() throws Exception {
        String js = buildJavaScript();
        assertTrue(js.contains("ensureValue.call(this,'txtSexo', sexoItems);"), "Sexo should get an explicit default");
        assertTrue(js.contains("ensureValue.call(this,'txtPrimeraMatricula',primeraMatriculaItems);"), "Primera matricula should default to a non-blank value");
        assertTrue(js.contains("if(SELECTORS.txtEstudios){ setItems.call(this,'txtEstudios',SELECTORS.txtEstudios); ensureValue.call(this,'txtEstudios',SELECTORS.txtEstudios); }"), "txtEstudios should get an explicit default");
        assertTrue(js.contains("ensureValue.call(this,'txtDobleMatricula',dobleMatriculaItems);"), "Doble matricula should default to a non-blank value");
        assertTrue(js.contains("if(SELECTORS.txtEstudios){ setItems.call(this,'child_txtEstudios',SELECTORS.txtEstudios); ensureValue.call(this,'child_txtEstudios',SELECTORS.txtEstudios); }"), "Child estudios should get an explicit default");
    }

    @Test
    void previewSummaryUsesHiddenDisplayOutsidePreviewMode() throws Exception {
        String js = buildJavaScript();
        assertTrue(
                js.contains("function setPreview(on){ __preview=on; var r=f.call(this,'ResumenAcademico'); if(r){ r.display = on ? display.visible : display.hidden; }"),
                "Preview summary should be fully hidden in edit mode so it does not intercept page-2 clicks"
        );
        assertFalse(
                js.contains("function setPreview(on){ __preview=on; var r=f.call(this,'ResumenAcademico'); if(r){ r.display = on ? display.visible : display.noView; }"),
                "Preview summary should not remain in noView mode outside preview"
        );
    }

    @Test
    void dependentSelectorsAvoidRebuildingTheirOwnOptionListsOnEveryRefresh() throws Exception {
        String js = buildJavaScript();
        assertTrue(js.contains("function syncChoiceItems(fieldName, items){"), "JS should cache dynamic selector options");
        assertTrue(js.contains("syncChoiceItems.call(this,prefix+'txtESO_Programa', opts);"), "ESO program selector should only rebuild when its options actually change");
        assertTrue(js.contains("syncChoiceItems.call(this,prefix+'txtESO_Itinerario', itOpts);"), "ESO itinerary selector should only rebuild when its options actually change");
        assertTrue(js.contains("syncChoiceItems.call(this,prefix+'txtBACH_Itinerario', itOptsB);"), "Bach itinerary selector should only rebuild when its options actually change");

        assertFalse(js.contains("if(progField){ setItems.call(this,prefix+'txtESO_Programa', opts); ensureValue.call(this,prefix+'txtESO_Programa', opts); }"), "ESO program selector should not be rebuilt unconditionally");
        assertFalse(js.contains("if(itField){ itField.display = display.visible; setItems.call(this,prefix+'txtESO_Itinerario', itOpts); ensureValue.call(this,prefix+'txtESO_Itinerario', itOpts); }"), "ESO itinerary selector should not be rebuilt unconditionally");
        assertFalse(js.contains("if(itF){ setItems.call(this,prefix+'txtBACH_Itinerario', itOptsB); ensureValue.call(this,prefix+'txtBACH_Itinerario', itOptsB); }"), "Bach itinerary selector should not be rebuilt unconditionally");
    }

    @Test
    void page2DefaultWidgetsAreOrderedAboveOverlappingAlternatives() throws Exception {
        Path output = Files.createTempFile("matricula-page2-order", ".pdf");
        try {
            GenerateMatriculaPdf.main(new String[]{
                    "--model", "../data/model.json",
                    "--bg1", "../assets/backgrounds/bg_page1.png",
                    "--bg2", "../assets/backgrounds/bg_page2.png",
                    "--out", output.toString()
            });

            try (PDDocument document = PDDocument.load(output.toFile())) {
                PDAcroForm form = document.getDocumentCatalog().getAcroForm();
                assertNotNull(form);

                Map<Object, String> widgetNames = new HashMap<>();
                for (PDField field : form.getFieldTree()) {
                    for (PDAnnotationWidget widget : field.getWidgets()) {
                        widgetNames.put(widget.getCOSObject(), field.getFullyQualifiedName());
                    }
                }

                List<String> page2Order = new ArrayList<>();
                for (PDAnnotation annotation : document.getPage(1).getAnnotations()) {
                    if (annotation instanceof PDAnnotationWidget) {
                        String name = widgetNames.get(annotation.getCOSObject());
                        if (name != null) {
                            page2Order.add(name);
                        }
                    }
                }

                assertGreater(page2Order, "txtEstudios", "child_txtEstudios");
                assertGreater(page2Order, "txtESO_Cursos", "txtBACH_Cursos");
                assertGreater(page2Order, "txtESO_Cursos", "txtCICLOS_Cursos");
                assertGreater(page2Order, "btnValidate", "ResumenAcademico");
                assertGreater(page2Order, "btnTogglePreview", "ResumenAcademico");
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static void assertRefreshesOnBlur(PDAcroForm form, String fieldName, String scriptFragment) throws IOException {
        PDField field = form.getField(fieldName);
        assertNotNull(field, () -> "Missing field: " + fieldName);

        PDFormFieldAdditionalActions fieldActions = field.getActions();
        if (fieldActions != null) {
            assertNull(fieldActions.getV(), () -> "Field " + fieldName + " still uses Validate for automatic refresh");
        }

        List<String> blurScripts = new ArrayList<>();
        for (PDAnnotationWidget widget : field.getWidgets()) {
            PDAnnotationAdditionalActions widgetActions = widget.getActions();
            if (widgetActions == null) {
                continue;
            }
            if (widgetActions.getBl() instanceof PDActionJavaScript js) {
                blurScripts.add(js.getAction());
            }
        }

        assertFalse(blurScripts.isEmpty(), () -> "Field " + fieldName + " has no widget blur handler");
        assertTrue(
                blurScripts.stream().anyMatch(script -> script != null && script.contains(scriptFragment)),
                () -> "Field " + fieldName + " blur handler does not contain " + scriptFragment
        );
    }

    private static void assertDoesNotRefreshOnBlur(PDAcroForm form, String fieldName, String scriptFragment) throws IOException {
        PDField field = form.getField(fieldName);
        assertNotNull(field, () -> "Missing field: " + fieldName);

        for (PDAnnotationWidget widget : field.getWidgets()) {
            PDAnnotationAdditionalActions widgetActions = widget.getActions();
            if (widgetActions == null) {
                continue;
            }
            if (widgetActions.getBl() instanceof PDActionJavaScript js) {
                assertFalse(
                        js.getAction() != null && js.getAction().contains(scriptFragment),
                        () -> "Field " + fieldName + " should not refresh on blur with " + scriptFragment
                );
            }
        }
    }

    private static void assertRefreshesOnCommit(PDAcroForm form, String fieldName, String scriptFragment) throws IOException {
        PDField field = form.getField(fieldName);
        assertNotNull(field, () -> "Missing field: " + fieldName);
        assertInstanceOf(PDChoice.class, field, () -> "Field " + fieldName + " should be a choice field");

        PDChoice choice = (PDChoice) field;
        assertTrue(choice.isCommitOnSelChange(), () -> "Field " + fieldName + " must commit selection immediately");

        PDFormFieldAdditionalActions fieldActions = field.getActions();
        assertNotNull(fieldActions, () -> "Field " + fieldName + " has no field actions");
        assertNull(fieldActions.getV(), () -> "Field " + fieldName + " should not use Validate for academic cascades");
        assertInstanceOf(PDActionJavaScript.class, fieldActions.getK(), () -> "Field " + fieldName + " should refresh on commit via Keystroke");

        String js = ((PDActionJavaScript) fieldActions.getK()).getAction();
        assertNotNull(js, () -> "Field " + fieldName + " commit script is missing");
        assertTrue(js.contains(scriptFragment), () -> "Field " + fieldName + " commit script does not contain " + scriptFragment);
        assertTrue(js.contains("__suspendCascade"), () -> "Field " + fieldName + " commit script should respect the cascade reentry guard");
        assertTrue(js.contains("event.willCommit"), () -> "Field " + fieldName + " commit script should wait for a committed selection");
    }

    private static String buildJavaScript() throws Exception {
        Method method = GenerateMatriculaPdf.class.getDeclaredMethod("buildJavaScript", String.class);
        method.setAccessible(true);
        String model = Files.readString(Path.of("../data/model.json"), StandardCharsets.UTF_8);
        return (String) method.invoke(null, model);
    }

    private static void assertGreater(List<String> orderedFields, String higherPriorityField, String lowerPriorityField) {
        int higher = orderedFields.indexOf(higherPriorityField);
        int lower = orderedFields.indexOf(lowerPriorityField);
        assertTrue(higher >= 0, () -> "Missing field in page-2 annotation order: " + higherPriorityField);
        assertTrue(lower >= 0, () -> "Missing field in page-2 annotation order: " + lowerPriorityField);
        assertTrue(higher > lower, () -> higherPriorityField + " should be ordered after " + lowerPriorityField + " on page 2");
    }
}
