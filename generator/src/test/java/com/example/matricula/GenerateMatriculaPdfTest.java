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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    void backgroundAssetsKeepOfficialHeaderAndAcademicBody() throws Exception {
        assertTrue(
                darkPixelRatio(Path.of("../assets/backgrounds/bg_page1.png"), 450, 220, 2050, 480) >= 0.24,
                "Page 1 background should keep the official title/subtitle block under the logos"
        );
        assertTrue(
                darkPixelRatio(Path.of("../assets/backgrounds/bg_page2.png"), 60, 730, 2420, 1900) >= 0.15,
                "Page 2 background should keep the central academic block instead of leaving a blank middle section"
        );
    }

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
    void page2InteractiveWidgetsDoNotOverlap() throws Exception {
        Path output = Files.createTempFile("matricula-page2-layout", ".pdf");
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

                List<String> overlaps = new ArrayList<>();
                List<PDAnnotationWidget> widgets = new ArrayList<>();
                Map<String, PDAnnotationWidget> widgetsByName = new HashMap<>();
                for (PDAnnotation annotation : document.getPage(1).getAnnotations()) {
                    if (annotation instanceof PDAnnotationWidget) {
                        String name = widgetNames.get(annotation.getCOSObject());
                        if (name != null && !"ResumenAcademico".equals(name)) {
                            PDAnnotationWidget widget = (PDAnnotationWidget) annotation;
                            widgets.add(widget);
                            widgetsByName.put(name, widget);
                        }
                    }
                }

                for (int i = 0; i < widgets.size(); i++) {
                    PDAnnotationWidget left = widgets.get(i);
                    String leftName = widgetNames.get(left.getCOSObject());
                    for (int j = i + 1; j < widgets.size(); j++) {
                        PDAnnotationWidget right = widgets.get(j);
                        String rightName = widgetNames.get(right.getCOSObject());
                        if (isAllowedDynamicOverlap(leftName, rightName)) {
                            continue;
                        }
                        if (rectanglesOverlap(left.getRectangle(), right.getRectangle())) {
                            overlaps.add(leftName + " <-> " + rightName);
                        }
                    }
                }

                assertTrue(overlaps.isEmpty(), () -> "Page 2 should not contain overlapping widgets, found: " + overlaps);

                float signatureTopY = 416.6f;
                float signatureClearance = 20f;
                List<String> tooCloseToSignatures = new ArrayList<>();
                for (PDAnnotationWidget widget : widgets) {
                    String name = widgetNames.get(widget.getCOSObject());
                    if (name == null || name.startsWith("sig_") || name.startsWith("child_")) {
                        continue;
                    }
                    float bottomY = widget.getRectangle().getLowerLeftY();
                    if (bottomY > signatureTopY && bottomY < signatureTopY + signatureClearance) {
                        tooCloseToSignatures.add(name + "@" + bottomY);
                    }
                }

                assertTrue(
                        tooCloseToSignatures.isEmpty(),
                        () -> "Page 2 selectors above the signature band need at least " + signatureClearance
                                + "pt of clearance, found: " + tooCloseToSignatures
                );

                assertTrue(
                        verticalGap(widgetsByName.get("txtDobleMatricula"), widgetsByName.get("optTroncal_1")) >= 6f,
                        "Doble matricula should keep clear vertical separation from Tr1"
                );
                assertTrue(
                        verticalGap(widgetsByName.get("lblC3"), widgetsByName.get("lblOpt7")) >= 8f,
                        "C3 should keep clear vertical separation from O7"
                );
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

    private static boolean rectanglesOverlap(org.apache.pdfbox.pdmodel.common.PDRectangle left, org.apache.pdfbox.pdmodel.common.PDRectangle right) {
        float overlapWidth = Math.min(left.getUpperRightX(), right.getUpperRightX()) - Math.max(left.getLowerLeftX(), right.getLowerLeftX());
        float overlapHeight = Math.min(left.getUpperRightY(), right.getUpperRightY()) - Math.max(left.getLowerLeftY(), right.getLowerLeftY());
        return overlapWidth > 0 && overlapHeight > 0;
    }

    private static float verticalGap(PDAnnotationWidget upper, PDAnnotationWidget lower) {
        assertNotNull(upper);
        assertNotNull(lower);
        return upper.getRectangle().getLowerLeftY() - lower.getRectangle().getUpperRightY();
    }

    private static double darkPixelRatio(Path path, int x0, int y0, int x1, int y1) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, () -> "Could not read image " + path);

        int darkPixels = 0;
        int totalPixels = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                if (r < 240 || g < 240 || b < 240) {
                    darkPixels++;
                }
                totalPixels++;
            }
        }

        return totalPixels == 0 ? 0.0 : (double) darkPixels / totalPixels;
    }

    private static boolean isAllowedDynamicOverlap(String leftName, String rightName) {
        return isFreeTextBandName(leftName) && isSelectionGridName(rightName)
                || isFreeTextBandName(rightName) && isSelectionGridName(leftName);
    }

    private static boolean isFreeTextBandName(String name) {
        return name != null && (name.startsWith("txtOptLibre_")
                || name.startsWith("child_txtOptLibre_")
                || name.startsWith("lblLibre")
                || name.startsWith("child_lblLibre"));
    }

    private static boolean isSelectionGridName(String name) {
        return name != null && (name.startsWith("optTroncal_")
                || name.startsWith("optComun_")
                || name.startsWith("optativa_")
                || name.startsWith("child_optTroncal_")
                || name.startsWith("child_optComun_")
                || name.startsWith("child_optativa_")
                || name.startsWith("lblTr")
                || name.startsWith("lblC")
                || name.startsWith("lblOpt")
                || name.startsWith("child_lblTr")
                || name.startsWith("child_lblC")
                || name.startsWith("child_lblOpt"));
    }
}
