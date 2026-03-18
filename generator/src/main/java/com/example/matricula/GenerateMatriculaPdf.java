package com.example.matricula;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDJavascriptNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.action.PDFormFieldAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GenerateMatriculaPdf {

    private static final PDRectangle A4 = PDRectangle.A4;

    private static final float PAGE_W = A4.getWidth();
    private static final float PAGE_H = A4.getHeight();

    private static class Args {
        Path modelJson = Path.of("data/model.json");
        Path bg1 = Path.of("assets/backgrounds/bg_page1.png");
        Path bg2 = Path.of("assets/backgrounds/bg_page2.png");
        Path out = Path.of("matricula_interactiva.pdf");
    }

    public static void main(String[] argv) throws Exception {
        Args args = parseArgs(argv);
        requireFile(args.modelJson);
        requireFile(args.bg1);
        requireFile(args.bg2);

        String modelJson = Files.readString(args.modelJson, StandardCharsets.UTF_8);
        generate(args, modelJson);
        System.out.println("Wrote: " + args.out.toAbsolutePath());
    }

    private static Args parseArgs(String[] argv) {
        Args a = new Args();
        for (int i = 0; i < argv.length; i++) {
            String k = argv[i];
            String v = (i + 1 < argv.length) ? argv[i + 1] : null;
            if (k.equals("--model") && v != null) {
                a.modelJson = Path.of(v);
                i++;
            } else if (k.equals("--bg1") && v != null) {
                a.bg1 = Path.of(v);
                i++;
            } else if (k.equals("--bg2") && v != null) {
                a.bg2 = Path.of(v);
                i++;
            } else if (k.equals("--out") && v != null) {
                a.out = Path.of(v);
                i++;
            }
        }
        return a;
    }

    private static void requireFile(Path p) {
        if (!Files.isRegularFile(p)) {
            throw new IllegalArgumentException("Missing file: " + p.toAbsolutePath());
        }
    }

    private static void generate(Args args, String modelJson) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page1 = new PDPage(A4);
            PDPage page2 = new PDPage(A4);
            doc.addPage(page1);
            doc.addPage(page2);

            drawBackground(doc, page1, args.bg1);
            drawBackground(doc, page2, args.bg2);
            drawPage2AcademicScaffold(doc, page2);

            PDAcroForm form = new PDAcroForm(doc);
            form.setNeedAppearances(true);
            form.setDefaultResources(new org.apache.pdfbox.pdmodel.PDResources());
            // Ensure Helvetica is available via default appearance.
            form.getDefaultResources().put(COSName.getPDFName("Helv"), PDType1Font.HELVETICA);
            form.setDefaultAppearance("/Helv 9 Tf 0 g");
            doc.getDocumentCatalog().setAcroForm(form);

            // --- Page 1: header dynamic fields ---
            PDTextField printEstudio1 = addText(form, page1, "printEstudio1", rect(100, 750, 380, 14), 11, true, false);
            PDTextField printEstudio2 = addText(form, page1, "printEstudio2", rect(100, 736, 380, 14), 10, true, false);
            PDTextField numExpediente = addText(form, page1, "numExpediente", rect(88, 722, 150, 12), 9, true, false);
            PDTextField printCurso = addText(form, page1, "printCurso", rect(287, 722, 80, 12), 9, true, false);
            printEstudio1.setReadOnly(true);
            printEstudio2.setReadOnly(true);
            numExpediente.setReadOnly(true);
            printCurso.setReadOnly(true);

            // Privacy checkboxes (positions from FP template page 1 images).
            addCheck(form, page1, "leidoPolitica", rect(430.6f, 722.8f, 12, 12), true);
            addCheck(form, page1, "aceptoPolitica", rect(472.2f, 722.8f, 12, 12), true);

            // --- Page 1: student fields ---
            // Student rows.
            addText(form, page1, "txtApellido1", inset(cell(18.4f, 209.2f, 673.1f, 686.2f), 2, 1), 9, true, false);
            addText(form, page1, "txtApellido2", inset(cell(209.2f, 400.6f, 673.1f, 686.2f), 2, 1), 9, true, false);
            addText(form, page1, "txtNombre", inset(cell(400.6f, 580.2f, 673.1f, 686.2f), 2, 1), 9, true, false);

            addText(form, page1, "txtDireccion", inset(cell(18.4f, 276.7f, 647.4f, 660.7f), 2, 1), 9, true, false);
            addText(form, page1, "txtPoblacion", inset(cell(276.7f, 417.5f, 647.4f, 660.7f), 2, 1), 9, true, false);
            addText(form, page1, "txtCodPostal", inset(cell(417.5f, 456.9f, 647.4f, 660.7f), 2, 1), 9, true, false);
            addText(form, page1, "txtProvincia", inset(cell(456.9f, 580.2f, 647.4f, 660.7f), 2, 1), 9, true, false);

            addText(form, page1, "txtNumDocumento", inset(cell(18.4f, 102.2f, 621.9f, 635.2f), 2, 1), 9, true, false);
            addText(form, page1, "txtNumSS", inset(cell(102.2f, 243.0f, 621.9f, 635.2f), 2, 1), 9, true, false);
            addText(form, page1, "txtTfnoCasa", inset(cell(243.0f, 327.4f, 621.9f, 635.2f), 2, 1), 9, true, false);
            addText(form, page1, "txtEmailAlumno", inset(cell(327.4f, 580.2f, 621.9f, 635.2f), 2, 1), 9, true, false);

            // Date of birth split into day/month/year inside the single cell.
            addText(form, page1, "txtDiaNac", rect(22, 598.2f, 14, 11), 9, true, false);
            addText(form, page1, "txtMesNac", rect(40, 598.2f, 14, 11), 9, true, false);
            addText(form, page1, "txtAnioNac", rect(58, 598.2f, 28, 11), 9, true, false);

            addText(form, page1, "txtPoblacionNac", inset(cell(74.0f, 243.0f, 597.1f, 609.7f), 2, 1), 9, true, false);
            addText(form, page1, "txtProvinciaNac", inset(cell(243.0f, 344.3f, 597.1f, 609.7f), 2, 1), 9, true, false);
            addCombo(form, page1, "txtSexo", inset(cell(344.3f, 378.1f, 597.1f, 609.7f), 2, 1), 9, true, false);
            addText(form, page1, "txtNacionalidad", inset(cell(378.1f, 479.5f, 597.1f, 609.7f), 2, 1), 9, true, false);
            addText(form, page1, "txtPais", inset(cell(479.5f, 580.2f, 597.1f, 609.7f), 2, 1), 9, true, false);

            // Duplicated row on the template: copy of SS and email.
            addText(form, page1, "txtNumSS2", inset(cell(18.4f, 299.2f, 572.3f, 584.2f), 2, 1), 9, true, false);
            addText(form, page1, "txtEmailAlumno2", inset(cell(299.2f, 580.2f, 572.3f, 584.2f), 2, 1), 9, true, false);

            // --- Page 1: tutor 1 ---
            addText(form, page1, "txtApellido1Tutor1", inset(cell(18.4f, 209.2f, 521.2f, 534.6f), 2, 1), 9, true, false);
            addText(form, page1, "txtApellido2Tutor1", inset(cell(209.2f, 400.6f, 521.2f, 534.6f), 2, 1), 9, true, false);
            addText(form, page1, "txtNombreTutor1", inset(cell(400.6f, 580.2f, 521.2f, 534.6f), 2, 1), 9, true, false);

            addText(form, page1, "txtNIFTutor1", inset(cell(18.5f, 158.5f, 496.2f, 509.1f), 2, 1), 9, true, false);
            addText(form, page1, "txtFechaNacTutor1", inset(cell(158.5f, 259.9f, 496.2f, 509.1f), 2, 1), 9, true, false);
            addText(form, page1, "txtMovilTutor1", inset(cell(259.9f, 327.4f, 496.2f, 509.1f), 2, 1), 9, true, false);
            addText(form, page1, "txtEmailTutor1", inset(cell(327.4f, 496.4f, 496.2f, 509.1f), 2, 1), 9, true, false);
            addText(form, page1, "txtNacionalidadTutor1", inset(cell(496.4f, 580.1f, 496.2f, 509.1f), 2, 1), 9, true, false);

            addText(form, page1, "txtDireccionTutor1", inset(cell(18.5f, 276.7f, 471.6f, 483.6f), 2, 1), 9, true, false);
            addText(form, page1, "txtPoblacionTutor1", inset(cell(276.7f, 417.5f, 471.6f, 483.6f), 2, 1), 9, true, false);
            addText(form, page1, "txtCodPostalTutor1", inset(cell(417.5f, 456.9f, 471.6f, 483.6f), 2, 1), 9, true, false);
            addText(form, page1, "txtProvinciaTutor1", inset(cell(456.9f, 580.1f, 471.6f, 483.6f), 2, 1), 9, true, false);

            // --- Page 1: tutor 2 ---
            addText(form, page1, "txtApellido1Tutor2", inset(cell(18.4f, 209.2f, 420.5f, 434.0f), 2, 1), 9, true, false);
            addText(form, page1, "txtApellido2Tutor2", inset(cell(209.2f, 400.6f, 420.5f, 434.0f), 2, 1), 9, true, false);
            addText(form, page1, "txtNombreTutor2", inset(cell(400.6f, 580.2f, 420.5f, 434.0f), 2, 1), 9, true, false);

            addText(form, page1, "txtNIFTutor2", inset(cell(18.5f, 158.5f, 395.6f, 408.5f), 2, 1), 9, true, false);
            addText(form, page1, "txtFechaNacTutor2", inset(cell(158.5f, 259.9f, 395.6f, 408.5f), 2, 1), 9, true, false);
            addText(form, page1, "txtMovilTutor2", inset(cell(259.9f, 327.4f, 395.6f, 408.5f), 2, 1), 9, true, false);
            addText(form, page1, "txtEmailTutor2", inset(cell(327.4f, 496.4f, 395.6f, 408.5f), 2, 1), 9, true, false);
            addText(form, page1, "txtNacionalidadTutor2", inset(cell(496.4f, 580.1f, 395.6f, 408.5f), 2, 1), 9, true, false);

            addText(form, page1, "txtDireccionTutor2", inset(cell(18.5f, 276.7f, 371.0f, 382.9f), 2, 1), 9, true, false);
            addText(form, page1, "txtPoblacionTutor2", inset(cell(276.7f, 417.5f, 371.0f, 382.9f), 2, 1), 9, true, false);
            addText(form, page1, "txtCodPostalTutor2", inset(cell(417.5f, 456.9f, 371.0f, 382.9f), 2, 1), 9, true, false);
            addText(form, page1, "txtProvinciaTutor2", inset(cell(456.9f, 580.1f, 371.0f, 382.9f), 2, 1), 9, true, false);

            // --- Page 1: other data ---
            addCombo(form, page1, "txtPrimeraMatricula", rect(90, 321.0f, 60, 12), 9, true, false);
            addText(form, page1, "txtCentroProcedencia", inset(cell(18.5f, 299.2f, 294.7f, 307.8f), 2, 1), 9, true, false);
            addText(form, page1, "txtEstudiosProcedencia", inset(cell(299.2f, 580.0f, 294.7f, 307.8f), 2, 1), 9, true, false);
            addCombo(form, page1, "txtNumHermanos", rect(140, 270.7f, 40, 12), 9, true, false);
            addCombo(form, page1, "txtFNumerosa", rect(420, 270.7f, 120, 12), 9, true, false);
            addText(form, page1, "txtNotificaciones", inset(cell(18.5f, 580.0f, 244.9f, 256.8f), 2, 1), 9, true, false);

            // --- Page 1: repeated courses checkboxes (positions from FP template images) ---
            float repY = 201.65f;
            float repSize = 12f;
            float[] repX = new float[]{122.683f, 184.932f, 247.181f, 309.430f, 371.679f, 439.427f, 507.175f};
            for (int i = 0; i < repX.length; i++) {
                addCheck(form, page1, "txtCursosRepetidosIES" + i, rect(repX[i], repY, repSize, repSize), true);
            }

            // --- Page 1: authorization 1 yes/no checkboxes (positions from FP template images) ---
            addCheck(form, page1, "txtAutoriza1_si", rect(545.046f, 141.56f, 12, 12), true);
            addCheck(form, page1, "txtAutoriza1_no", rect(564.576f, 142.33f, 12, 12), true);

            // Authorization contact fields (aligned to the template check icons).
            addText(form, page1, "txtAutorizaTfnoTutor1", rect(262.0f, 114.07f, 204.0f, 12.0f), 9, true, false);
            addText(form, page1, "txtAutorizaMailTutor1", rect(262.0f, 100.57f, 204.0f, 12.0f), 9, true, false);
            addText(form, page1, "txtAutorizaTfnoTutor2", rect(262.0f, 87.05f, 204.0f, 12.0f), 9, true, false);
            addText(form, page1, "txtAutorizaMailTutor2", rect(262.0f, 73.56f, 204.0f, 12.0f), 9, true, false);

            // --- Page 2: authorizations 2..5 checkboxes (positions from BACH template page 2 images) ---
            // Using same x for each row.
            float authSiX = 544.989f;
            float authNoX = 564.520f;
            // From template image bboxes (top-based), converted already to y-from-bottom here:
            // y_bottom = PAGE_H - bottom
            float[] authY = new float[]{779.385f, 750.160f, 727.483f, 704.777f};
            for (int idx = 0; idx < authY.length; idx++) {
                int n = idx + 2;
                addCheck(form, page2, "txtAutoriza" + n + "_si", rect(authSiX, authY[idx], 12, 12), true);
                addCheck(form, page2, "txtAutoriza" + n + "_no", rect(authNoX, authY[idx], 12, 12), true);
            }

            // --- Page 2: academic UI + printable summary ---
            // Printable summary field (prints but is hidden in edit mode via JS).
            addText(form, page2, "ResumenAcademico", rect(22, 450, 555, 220), 9, true, false);
            PDTextField resumen = (PDTextField) form.getField("ResumenAcademico");
            if (resumen != null) {
                resumen.setMultiline(true);
                resumen.setReadOnly(true);
            }

            // UI mode bar and shared metadata fields (no-print, visible in edit mode).
            addLabel(form, page2, "uiModeLabel", rect(22, 656, 150, 12), 10);
            addText(form, page2, "txtLugar", rect(178, 656, 126, 12), 9, false, true);
            addText(form, page2, "txtFecha", rect(310, 656, 80, 12), 9, false, true);
            addButton(form, page2, "btnValidate", rect(398, 654, 84, 16), "Validar", false);
            addButton(form, page2, "btnTogglePreview", rect(488, 654, 88, 16), "Vista previa", false);

            // Principal academic section.
            float mainLabelX = 22;
            float mainFieldX = 112;
            float selectorLabelW = 84;
            float selectorFieldW = 186;
            float selectorRowH = 12;
            float mainSelectorY = 640;
            addLabel(form, page2, "lblEstudios", rect(mainLabelX, mainSelectorY, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtEstudios", rect(mainFieldX, mainSelectorY, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblEsoCurso", rect(mainLabelX, mainSelectorY - selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtESO_Cursos", rect(mainFieldX, mainSelectorY - selectorRowH, 90, 12), 9, false, true);
            addLabel(form, page2, "lblBachCurso", rect(mainLabelX, mainSelectorY - 2 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtBACH_Cursos", rect(mainFieldX, mainSelectorY - 2 * selectorRowH, 90, 12), 9, false, true);
            addLabel(form, page2, "lblCiclosCurso", rect(mainLabelX, mainSelectorY - 3 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtCICLOS_Cursos", rect(mainFieldX, mainSelectorY - 3 * selectorRowH, 90, 12), 9, false, true);
            addLabel(form, page2, "lblGrado", rect(mainLabelX, mainSelectorY - 4 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtCICLOS_Grados", rect(mainFieldX, mainSelectorY - 4 * selectorRowH, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblPrograma", rect(mainLabelX, mainSelectorY - 5 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtESO_Programa", rect(mainFieldX, mainSelectorY - 5 * selectorRowH, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblEsoItinerario", rect(mainLabelX, mainSelectorY - 6 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtESO_Itinerario", rect(mainFieldX, mainSelectorY - 6 * selectorRowH, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblBachItinerario", rect(mainLabelX, mainSelectorY - 7 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtBACH_Itinerario", rect(mainFieldX, mainSelectorY - 7 * selectorRowH, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblMatriculaCompleta", rect(mainLabelX, mainSelectorY - 8 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "matriculaCompleta", rect(mainFieldX, mainSelectorY - 8 * selectorRowH, 90, 12), 9, false, true);
            addLabel(form, page2, "lblDoble", rect(mainLabelX, mainSelectorY - 9 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "txtDobleMatricula", rect(mainFieldX, mainSelectorY - 9 * selectorRowH, 90, 12), 9, false, true);

            float mainFreeLabelX = 314;
            float mainFreeFieldX = 340;
            float freeFieldW = 236;
            float mainFreeTopY = 624;
            for (int i = 1; i <= 10; i++) {
                float freeY = mainFreeTopY - (i - 1) * selectorRowH;
                addLabel(form, page2, "lblLibre" + i, rect(mainFreeLabelX, freeY, 22, 12), 9);
                addText(form, page2, "txtOptLibre_" + i, rect(mainFreeFieldX, freeY, freeFieldW, 12), 9, false, true);
            }

            // Principal selection grid in three columns.
            float[] mainGridLabelX = new float[]{22, 205, 388};
            float[] mainGridFieldX = new float[]{45, 228, 411};
            float gridFieldW = 160;
            float gridRowH = 12;
            float mainGridY = 510;
            addLabel(form, page2, "lblTr1", rect(mainGridLabelX[0], mainGridY, 20, 12), 9);
            addCombo(form, page2, "optTroncal_1", rect(mainGridFieldX[0], mainGridY, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblTr2", rect(mainGridLabelX[1], mainGridY, 20, 12), 9);
            addCombo(form, page2, "optTroncal_2", rect(mainGridFieldX[1], mainGridY, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblTr3", rect(mainGridLabelX[2], mainGridY, 20, 12), 9);
            addCombo(form, page2, "optTroncal_3", rect(mainGridFieldX[2], mainGridY, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblC1", rect(mainGridLabelX[0], mainGridY - gridRowH, 20, 12), 9);
            addCombo(form, page2, "optComun_1", rect(mainGridFieldX[0], mainGridY - gridRowH, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblC2", rect(mainGridLabelX[1], mainGridY - gridRowH, 20, 12), 9);
            addCombo(form, page2, "optComun_2", rect(mainGridFieldX[1], mainGridY - gridRowH, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "lblC3", rect(mainGridLabelX[2], mainGridY - gridRowH, 20, 12), 9);
            addCombo(form, page2, "optComun_3", rect(mainGridFieldX[2], mainGridY - gridRowH, gridFieldW, 12), 9, false, true);
            for (int i = 1; i <= 11; i++) {
                int zeroBased = i - 1;
                int row = zeroBased / 3;
                int col = zeroBased % 3;
                float optY = mainGridY - (row + 3) * gridRowH;
                addLabel(form, page2, "lblOpt" + i, rect(mainGridLabelX[col], optY, 20, 12), 9);
                addCombo(form, page2, "optativa_" + i, rect(mainGridFieldX[col], optY, gridFieldW, 12), 9, false, true);
            }

            // Child academic section under the signature boxes.
            float childSelectorY = 304;
            addLabel(form, page2, "child_lblEstudios", rect(mainLabelX, childSelectorY, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_txtEstudios", rect(mainFieldX, childSelectorY, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblEsoCurso", rect(mainLabelX, childSelectorY - selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_txtESO_Cursos", rect(mainFieldX, childSelectorY - selectorRowH, 90, 12), 9, false, true);
            addLabel(form, page2, "child_lblBachCurso", rect(mainLabelX, childSelectorY - 2 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_txtBACH_Cursos", rect(mainFieldX, childSelectorY - 2 * selectorRowH, 90, 12), 9, false, true);
            addLabel(form, page2, "child_lblCiclosCurso", rect(mainLabelX, childSelectorY - 3 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_txtCICLOS_Cursos", rect(mainFieldX, childSelectorY - 3 * selectorRowH, 90, 12), 9, false, true);
            addLabel(form, page2, "child_lblGrado", rect(mainLabelX, childSelectorY - 4 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_txtCICLOS_Grados", rect(mainFieldX, childSelectorY - 4 * selectorRowH, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblPrograma", rect(mainLabelX, childSelectorY - 5 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_txtESO_Programa", rect(mainFieldX, childSelectorY - 5 * selectorRowH, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblEsoItinerario", rect(mainLabelX, childSelectorY - 6 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_txtESO_Itinerario", rect(mainFieldX, childSelectorY - 6 * selectorRowH, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblBachItinerario", rect(mainLabelX, childSelectorY - 7 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_txtBACH_Itinerario", rect(mainFieldX, childSelectorY - 7 * selectorRowH, selectorFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblMatriculaCompleta", rect(mainLabelX, childSelectorY - 8 * selectorRowH, selectorLabelW, 12), 9);
            addCombo(form, page2, "child_matriculaCompleta", rect(mainFieldX, childSelectorY - 8 * selectorRowH, 90, 12), 9, false, true);

            float childFreeTopY = 288;
            for (int i = 1; i <= 10; i++) {
                float freeY = childFreeTopY - (i - 1) * selectorRowH;
                addLabel(form, page2, "child_lblLibre" + i, rect(mainFreeLabelX, freeY, 22, 12), 9);
                addText(form, page2, "child_txtOptLibre_" + i, rect(mainFreeFieldX, freeY, freeFieldW, 12), 9, false, true);
            }

            float childGridY = 150;
            addLabel(form, page2, "child_lblTr1", rect(mainGridLabelX[0], childGridY, 20, 12), 9);
            addCombo(form, page2, "child_optTroncal_1", rect(mainGridFieldX[0], childGridY, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblTr2", rect(mainGridLabelX[1], childGridY, 20, 12), 9);
            addCombo(form, page2, "child_optTroncal_2", rect(mainGridFieldX[1], childGridY, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblTr3", rect(mainGridLabelX[2], childGridY, 20, 12), 9);
            addCombo(form, page2, "child_optTroncal_3", rect(mainGridFieldX[2], childGridY, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblC1", rect(mainGridLabelX[0], childGridY - gridRowH, 20, 12), 9);
            addCombo(form, page2, "child_optComun_1", rect(mainGridFieldX[0], childGridY - gridRowH, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblC2", rect(mainGridLabelX[1], childGridY - gridRowH, 20, 12), 9);
            addCombo(form, page2, "child_optComun_2", rect(mainGridFieldX[1], childGridY - gridRowH, gridFieldW, 12), 9, false, true);
            addLabel(form, page2, "child_lblC3", rect(mainGridLabelX[2], childGridY - gridRowH, 20, 12), 9);
            addCombo(form, page2, "child_optComun_3", rect(mainGridFieldX[2], childGridY - gridRowH, gridFieldW, 12), 9, false, true);
            for (int i = 1; i <= 11; i++) {
                int zeroBased = i - 1;
                int row = zeroBased / 3;
                int col = zeroBased % 3;
                float optY = childGridY - (row + 2) * gridRowH;
                addLabel(form, page2, "child_lblOpt" + i, rect(mainGridLabelX[col], optY, 20, 12), 9);
                addCombo(form, page2, "child_optativa_" + i, rect(mainGridFieldX[col], optY, gridFieldW, 12), 9, false, true);
            }

            // Signature buttons (import image).
            addButton(form, page2, "sig_alumno", inset(cell(17.5f, 202.2f, 340.9f, 416.6f), 2, 2), "Firma alumno/a", true);
            addButton(form, page2, "sig_tutor1", inset(cell(206.0f, 390.7f, 340.9f, 416.6f), 2, 2), "Firma tutor 1", true);
            addButton(form, page2, "sig_tutor2", inset(cell(394.4f, 579.1f, 340.9f, 416.6f), 2, 2), "Firma tutor 2", true);

            // --- JS: document-level scripts and actions ---
            String js = buildJavaScript(modelJson);

            // Document-level JS lives in the Names -> JavaScript name tree.
            PDDocumentNameDictionary names = doc.getDocumentCatalog().getNames();
            if (names == null) {
                names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
            }
            PDJavascriptNameTreeNode jsTree = names.getJavaScript();
            if (jsTree == null) {
                jsTree = new PDJavascriptNameTreeNode();
            }
            Map<String, PDActionJavaScript> jsNames = new LinkedHashMap<>();
            jsNames.put("matricula", new PDActionJavaScript(js));
            jsTree.setNames(jsNames);
            names.setJavascript(jsTree);
            doc.getDocumentCatalog().setNames(names);

            // Init on open.
            doc.getDocumentCatalog().setOpenAction(new PDActionJavaScript("try{initMatricula();}catch(e){app.alert('Error init PDF: '+e);} "));

            // Attach per-field actions.
            attachScripts(form);
            reorderPage2WidgetsForDefaultState(form, page2);

            doc.save(args.out.toFile());
        }
    }

    private static void drawBackground(PDDocument doc, PDPage page, Path pngPath) throws IOException {
        BufferedImage img = ImageIO.read(pngPath.toFile());
        PDImageXObject ximg = LosslessFactory.createFromImage(doc, img);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            cs.drawImage(ximg, 0, 0, PAGE_W, PAGE_H);
        }
    }

    private static PDRectangle rect(float x, float y, float w, float h) {
        return new PDRectangle(x, y, w, h);
    }

    private static PDRectangle cell(float x0, float x1, float y0, float y1) {
        return rect(x0, y0, x1 - x0, y1 - y0);
    }

    private static PDRectangle inset(PDRectangle r, float ix, float iy) {
        return rect(r.getLowerLeftX() + ix, r.getLowerLeftY() + iy, r.getWidth() - 2 * ix, r.getHeight() - 2 * iy);
    }

    private static void drawPage2AcademicScaffold(PDDocument doc, PDPage page) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            cs.setStrokingColor(0.53f, 0.78f, 0.88f);
            cs.setNonStrokingColor(0.30f, 0.30f, 0.30f);
            cs.setLineWidth(0.8f);

            drawRuledBlock(cs, 16, 522, 282, 124, "SELECCION ACADEMICA", 10, 12);
            drawRuledBlock(cs, 304, 522, 275, 124, "MATERIAS / OBSERVACIONES", 10, 12);

            strokeRect(cs, 16, 432, 563, 84);
            drawBandLabel(cs, 16, 500, 563, 16, "ASIGNATURAS ESPECIFICAS");
            drawBandLabel(cs, 16, 484, 181, 16, "TRONCALES");
            drawBandLabel(cs, 199, 484, 181, 16, "COMUNES");
            drawBandLabel(cs, 382, 484, 197, 16, "OPTATIVAS");

            float[] columnX = new float[]{16, 199, 382, 579};
            for (float x : columnX) {
                moveTo(cs, x, 432);
                lineTo(cs, x, 500);
            }
            for (int row = 0; row < 5; row++) {
                float y = 432 + row * 12;
                moveTo(cs, 16, y);
                lineTo(cs, 579, y);
            }
            cs.stroke();

            drawBandLabel(cs, 16, 418, 563, 12, "SELECCION PARA IMPRESION Y FIRMAS");
        }
    }

    private static void drawRuledBlock(PDPageContentStream cs, float x, float y, float w, float h, String title, int rows, float rowHeight) throws IOException {
        strokeRect(cs, x, y, w, h);
        drawBandLabel(cs, x, y + h - 16, w, 16, title);
        for (int row = 1; row < rows; row++) {
            float lineY = y + h - 16 - row * rowHeight;
            if (lineY <= y) {
                break;
            }
            moveTo(cs, x, lineY);
            lineTo(cs, x + w, lineY);
        }
        cs.stroke();
    }

    private static void drawBandLabel(PDPageContentStream cs, float x, float y, float w, float h, String label) throws IOException {
        strokeRect(cs, x, y, w, h);
        cs.stroke();
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 7f);
        cs.newLineAtOffset(x + 4, y + 4);
        cs.showText(label);
        cs.endText();
    }

    private static void strokeRect(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.addRect(x, y, w, h);
    }

    private static void moveTo(PDPageContentStream cs, float x, float y) throws IOException {
        cs.moveTo(x, y);
    }

    private static void lineTo(PDPageContentStream cs, float x, float y) throws IOException {
        cs.lineTo(x, y);
    }

    private static void reorderPage2WidgetsForDefaultState(PDAcroForm form, PDPage page) throws IOException {
        Map<Object, String> fieldNamesByWidget = new HashMap<>();
        for (PDField field : form.getFieldTree()) {
            for (PDAnnotationWidget widget : field.getWidgets()) {
                fieldNamesByWidget.put(widget.getCOSObject(), field.getFullyQualifiedName());
            }
        }

        List<PDAnnotation> reordered = new ArrayList<>(page.getAnnotations());
        reordered.sort(Comparator.comparingInt(annotation -> page2AnnotationPriority(annotation, fieldNamesByWidget)));
        page.setAnnotations(reordered);
    }

    private static int page2AnnotationPriority(PDAnnotation annotation, Map<Object, String> fieldNamesByWidget) {
        if (!(annotation instanceof PDAnnotationWidget)) {
            return 10;
        }

        String name = fieldNamesByWidget.get(annotation.getCOSObject());
        if (name == null) {
            return 10;
        }

        if ("ResumenAcademico".equals(name)) {
            return 0;
        }

        if (name.startsWith("child_")) {
            return 1;
        }

        if (Set.of("txtBACH_Cursos", "txtCICLOS_Cursos", "txtCICLOS_Grados", "txtESO_Itinerario",
                "txtBACH_Itinerario", "matriculaCompleta").contains(name)) {
            return 2;
        }

        if (name.startsWith("sig_")) {
            return 4;
        }

        if (Set.of("uiModeLabel", "btnTogglePreview", "btnValidate", "txtEstudios", "txtESO_Cursos",
                "txtESO_Programa", "txtDobleMatricula").contains(name)) {
            return 5;
        }

        return 3;
    }

    private static PDTextField addText(PDAcroForm form, PDPage page, String name, PDRectangle r, int fontSize, boolean print, boolean noPrintUi) throws IOException {
        PDTextField f = new PDTextField(form);
        f.setPartialName(name);
        f.setDefaultAppearance(String.format(Locale.ROOT, "/Helv %d Tf 0 g", fontSize));
        PDAnnotationWidget w = new PDAnnotationWidget();
        w.setRectangle(r);
        w.setPage(page);
        w.setPrinted(print);
        w.setBorderStyle(borderNone());
        w.setParent(f);
        f.setWidgets(List.of(w));
        page.getAnnotations().add(w);
        form.getFields().add(f);

        // For UI-only fields that must not print, keep printed=false.
        if (noPrintUi) {
            w.setPrinted(false);
        }
        return f;
    }

    private static PDComboBox addCombo(PDAcroForm form, PDPage page, String name, PDRectangle r, int fontSize, boolean print, boolean noPrintUi) throws IOException {
        PDComboBox f = new PDComboBox(form);
        f.setPartialName(name);
        f.setDefaultAppearance(String.format(Locale.ROOT, "/Helv %d Tf 0 g", fontSize));
        f.setEdit(false);
        f.setCommitOnSelChange(true);
        PDAnnotationWidget w = new PDAnnotationWidget();
        w.setRectangle(r);
        w.setPage(page);
        w.setPrinted(print);
        w.setBorderStyle(borderNone());
        w.setParent(f);
        f.setWidgets(List.of(w));
        page.getAnnotations().add(w);
        form.getFields().add(f);
        if (noPrintUi) {
            w.setPrinted(false);
        }
        return f;
    }

    private static PDCheckBox addCheck(PDAcroForm form, PDPage page, String name, PDRectangle r, boolean print) throws IOException {
        PDCheckBox f = new PDCheckBox(form);
        f.setPartialName(name);
        PDAnnotationWidget w = new PDAnnotationWidget();
        w.setRectangle(r);
        w.setPage(page);
        w.setPrinted(print);
        w.setBorderStyle(borderNone());
        w.setParent(f);
        f.setWidgets(List.of(w));
        page.getAnnotations().add(w);
        form.getFields().add(f);
        // default Off
        return f;
    }

    private static PDPushButton addButton(PDAcroForm form, PDPage page, String name, PDRectangle r, String caption, boolean print) throws IOException {
        PDPushButton b = new PDPushButton(form);
        b.setPartialName(name);
        PDAnnotationWidget w = new PDAnnotationWidget();
        w.setRectangle(r);
        w.setPage(page);
        w.setPrinted(print);
        w.setBorderStyle(borderThin());
        w.setParent(b);
        b.setWidgets(List.of(w));
        page.getAnnotations().add(w);
        form.getFields().add(b);
        return b;
    }

    private static PDTextField addLabel(PDAcroForm form, PDPage page, String name, PDRectangle r, int fontSize) throws IOException {
        PDTextField f = addText(form, page, name, r, fontSize, false, true);
        f.setReadOnly(true);
        return f;
    }

    private static PDBorderStyleDictionary borderThin() {
        PDBorderStyleDictionary bs = new PDBorderStyleDictionary();
        bs.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
        bs.setWidth(0.5f);
        return bs;
    }

    private static PDBorderStyleDictionary borderNone() {
        PDBorderStyleDictionary bs = new PDBorderStyleDictionary();
        bs.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
        bs.setWidth(0);
        return bs;
    }

    private static void attachScripts(PDAcroForm form) throws IOException {
        // Make yes/no checkboxes exclusive for authorizations.
        setMouseUpJs(form, "txtAutoriza1_si", "onYesNo('txtAutoriza1',true);");
        setMouseUpJs(form, "txtAutoriza1_no", "onYesNo('txtAutoriza1',false);");
        for (int i = 2; i <= 5; i++) {
            setMouseUpJs(form, "txtAutoriza" + i + "_si", "onYesNo('txtAutoriza" + i + "',true);");
            setMouseUpJs(form, "txtAutoriza" + i + "_no", "onYesNo('txtAutoriza" + i + "',false);");
        }

        // Academic fields trigger update.
        String[] academicFields = new String[]{
                "txtEstudios", "txtESO_Cursos", "txtBACH_Cursos", "txtCICLOS_Cursos", "txtCICLOS_Grados",
                "txtESO_Programa", "txtESO_Itinerario", "txtBACH_Itinerario", "matriculaCompleta", "txtDobleMatricula",
                "child_txtEstudios", "child_txtESO_Cursos", "child_txtBACH_Cursos", "child_txtCICLOS_Cursos", "child_txtCICLOS_Grados",
                "child_txtESO_Programa", "child_txtESO_Itinerario", "child_txtBACH_Itinerario", "child_matriculaCompleta"
        };
        String deferredCommitUpdateAll = "if(event.willCommit && !__suspendCascade){try{app.setTimeOut('try{scheduleUpdateAll();}catch(e){}',1);}catch(e){try{scheduleUpdateAll();}catch(e2){}}}";
        String deferredResumen = "try{app.setTimeOut('try{scheduleResumen();}catch(e){}',1);}catch(e){try{scheduleResumen();}catch(e2){}}";
        for (String f : academicFields) {
            setKeystrokeJs(form, f, deferredCommitUpdateAll);
        }
        for (int i = 1; i <= 11; i++) {
            setBlurJs(form, "optativa_" + i, "try{scheduleResumen();}catch(e){}");
            setBlurJs(form, "child_optativa_" + i, "try{scheduleResumen();}catch(e){}");
            setMouseUpJs(form, "optativa_" + i, deferredResumen);
            setMouseUpJs(form, "child_optativa_" + i, deferredResumen);
        }
        for (int i = 1; i <= 3; i++) {
            setBlurJs(form, "optTroncal_" + i, "try{scheduleResumen();}catch(e){}");
            setBlurJs(form, "optComun_" + i, "try{scheduleResumen();}catch(e){}");
            setBlurJs(form, "child_optTroncal_" + i, "try{scheduleResumen();}catch(e){}");
            setBlurJs(form, "child_optComun_" + i, "try{scheduleResumen();}catch(e){}");
            setMouseUpJs(form, "optTroncal_" + i, deferredResumen);
            setMouseUpJs(form, "optComun_" + i, deferredResumen);
            setMouseUpJs(form, "child_optTroncal_" + i, deferredResumen);
            setMouseUpJs(form, "child_optComun_" + i, deferredResumen);
        }
        for (int i = 1; i <= 10; i++) {
            setBlurJs(form, "txtOptLibre_" + i, "try{scheduleResumen();}catch(e){}");
            setBlurJs(form, "child_txtOptLibre_" + i, "try{scheduleResumen();}catch(e){}");
        }

        // Validate button.
        setMouseUpJs(form, "btnValidate", "try{validateAll();}catch(e){app.alert(e);}");
        setMouseUpJs(form, "btnTogglePreview", "try{togglePreview();}catch(e){}");
        setMouseUpJs(form, "uiModeLabel", "try{toggleUiMode();}catch(e){}");

        // Signature import buttons.
        setMouseUpJs(form, "sig_alumno", "try{event.target.buttonImportIcon();}catch(e){app.alert('No se pudo importar la firma.');}");
        setMouseUpJs(form, "sig_tutor1", "try{event.target.buttonImportIcon();}catch(e){app.alert('No se pudo importar la firma.');}");
        setMouseUpJs(form, "sig_tutor2", "try{event.target.buttonImportIcon();}catch(e){app.alert('No se pudo importar la firma.');}");

        // Numeric keystroke restrictions.
        setKeystrokeJs(form, "txtCodPostal", "ksDigits(5);");
        setKeystrokeJs(form, "txtDiaNac", "ksDigits(2);");
        setKeystrokeJs(form, "txtMesNac", "ksDigits(2);");
        setKeystrokeJs(form, "txtAnioNac", "ksDigits(4);");
    }

    private static void setMouseUpJs(PDAcroForm form, String fieldName, String js) {
        PDField f = form.getField(fieldName);
        if (f == null || f.getWidgets().isEmpty()) return;
        PDAnnotationWidget w = f.getWidgets().get(0);
        PDAnnotationAdditionalActions aa = Optional.ofNullable(w.getActions()).orElse(new PDAnnotationAdditionalActions());
        aa.setU(new PDActionJavaScript(js));
        w.setActions(aa);
    }

    private static void setBlurJs(PDAcroForm form, String fieldName, String js) {
        PDField f = form.getField(fieldName);
        if (f == null || f.getWidgets().isEmpty()) return;
        PDAnnotationWidget w = f.getWidgets().get(0);
        PDAnnotationAdditionalActions aa = Optional.ofNullable(w.getActions()).orElse(new PDAnnotationAdditionalActions());
        aa.setBl(new PDActionJavaScript(js));
        w.setActions(aa);
    }

    private static void setValidateJs(PDAcroForm form, String fieldName, String js) {
        PDField f = form.getField(fieldName);
        if (f == null) return;
        PDFormFieldAdditionalActions aa = Optional.ofNullable(f.getActions()).orElse(new PDFormFieldAdditionalActions());
        aa.setV(new PDActionJavaScript(js));
        f.getCOSObject().setItem(COSName.AA, aa.getCOSObject());
    }

    private static void setKeystrokeJs(PDAcroForm form, String fieldName, String js) {
        PDField f = form.getField(fieldName);
        if (f == null) return;
        PDFormFieldAdditionalActions aa = Optional.ofNullable(f.getActions()).orElse(new PDFormFieldAdditionalActions());
        aa.setK(new PDActionJavaScript(js));
        f.getCOSObject().setItem(COSName.AA, aa.getCOSObject());
    }

    private static String buildJavaScript(String modelJson) {
        // Keep JS ASCII-only.
        StringBuilder sb = new StringBuilder();
        sb.append("var MODEL_DATA = ").append(modelJson).append(";\n");
        sb.append("var SELECTORS = MODEL_DATA.selectors || {};\n");
        sb.append("var SCENARIOS = MODEL_DATA.scenarios || {};\n");
        sb.append("var __preview = false;\n");
        sb.append("var __uiMode = 'main';\n");
        sb.append("var __suspendCascade = false;\n");
        sb.append("var __updateAllQueued = false;\n");
        sb.append("var __resumenQueued = false;\n");
        sb.append("var __choiceItemsSig = {};\n");
        sb.append("function f(name){ try { return this.getField(name); } catch(e){ return null; } }\n");
        sb.append("function sval(name){ var x=f.call(this,name); return x?String(x.value):''; }\n");
        sb.append("function isChecked(name){ var x=f.call(this,name); if(!x) return false; return String(x.value) !== 'Off'; }\n");
        sb.append("function setChecked(name,on){ var x=f.call(this,name); if(!x) return; try{ x.checkThisBox(0,on); }catch(e){ x.value = on ? 'Yes' : 'Off'; } }\n");
        sb.append("function setItems(fieldName, items){ var x=f.call(this,fieldName); if(!x) return; try{ x.clearItems(); }catch(e){}; if(!items) items=[]; try{ if(x.setItems){ var arr=[]; for(var i=0;i<items.length;i++){ arr.push([items[i].t, items[i].v]); } x.setItems(arr); return; } }catch(e2){}; try{ if(x.insertItemAt){ for(var j=0;j<items.length;j++){ x.insertItemAt(items[j].t, items[j].v, j); } } }catch(e3){} }\n");
        sb.append("function ensureValue(fieldName, items){ var x=f.call(this,fieldName); if(!x) return; var v=String(x.value||''); var ok=false; for(var i=0;i<items.length;i++){ if(String(items[i].v)===v){ ok=true; break; } } if(!ok && items.length>0){ x.value = items[0].v; } }\n");
        sb.append("function itemsSignature(items){ if(!items) return ''; var parts=[]; for(var i=0;i<items.length;i++){ var it=items[i]||{}; parts.push(String(it.t||'')+'\\u0001'+String(it.v||'')); } return parts.join('\\u0002'); }\n");
        sb.append("function syncChoiceItems(fieldName, items){ var sig=itemsSignature(items); if(__choiceItemsSig[fieldName]!==sig){ setItems.call(this,fieldName, items); __choiceItemsSig[fieldName]=sig; } ensureValue.call(this,fieldName, items||[]); }\n");
        sb.append("function clearChoice(fieldName){ var x=f.call(this,fieldName); if(!x) return; __choiceItemsSig[fieldName]=''; try{ x.clearItems(); }catch(e){} try{ x.currentValueIndices = -1; }catch(e2){} }\n");
        sb.append("function showUiField(fieldName, show){ var x=f.call(this,fieldName); if(x){ x.display = show ? display.visible : display.hidden; } }\n");
        sb.append("function showUiLabel(fieldName, show){ var x=f.call(this,fieldName); if(x){ x.display = show ? display.noPrint : display.hidden; } }\n");
        sb.append("function scheduleUpdateAll(){ if(__suspendCascade || __updateAllQueued) return; __updateAllQueued = true; try{ app.setTimeOut(\"try{__updateAllQueued=false;updateAll();}catch(e){__updateAllQueued=false;}\", 0); }catch(e){ __updateAllQueued=false; try{ updateAll(); }catch(e2){} } }\n");
        sb.append("function scheduleResumen(){ if(__suspendCascade || __resumenQueued) return; __resumenQueued = true; try{ app.setTimeOut(\"try{__resumenQueued=false;updateResumen();}catch(e){__resumenQueued=false;}\", 0); }catch(e){ __resumenQueued=false; try{ updateResumen(); }catch(e2){} } }\n");
        sb.append("function ksDigits(maxLen){ try{ var ch = event.change || ''; ch = ch.replace(/[^0-9]/g,''); var cur = event.value || ''; var sel = (event.selEnd - event.selStart); var newLen = cur.length - sel + ch.length; if(maxLen && newLen>maxLen){ ch = ch.substring(0, ch.length - (newLen - maxLen)); } event.change = ch; }catch(e){} }\n");
        sb.append("function onYesNo(base, yes){ var si = base+'_si'; var no = base+'_no'; if(yes){ if(isChecked.call(this, si)){ setChecked.call(this,no,false); } else { setChecked.call(this,no,true); } } else { if(isChecked.call(this, no)){ setChecked.call(this,si,false); } else { setChecked.call(this,si,true); } } }\n");
        sb.append("function setupCombos(){\n");
        sb.append("  // Sexo\n");
        sb.append("  if(SELECTORS.txtSexo){ var sexoItems = SELECTORS.txtSexo.map(function(it){return {v:it.v,t:it.v};}); setItems.call(this,'txtSexo', sexoItems); ensureValue.call(this,'txtSexo', sexoItems); }\n");
        sb.append("  // Alumno nuevo\n");
        sb.append("  var primeraMatriculaItems=[{v:'0',t:'No'},{v:'1',t:'Si'}]; setItems.call(this,'txtPrimeraMatricula',primeraMatriculaItems); ensureValue.call(this,'txtPrimeraMatricula',primeraMatriculaItems);\n");
        sb.append("  // Hermanos\n");
        sb.append("  var hs=[]; for(var i=0;i<=9;i++){ hs.push({v:String(i),t:String(i)}); } setItems.call(this,'txtNumHermanos',hs); ensureValue.call(this,'txtNumHermanos',hs);\n");
        sb.append("  // Familia numerosa\n");
        sb.append("  var fNumerosaItems=[{v:'No',t:'No'},{v:'Si, General',t:'Si, General'},{v:'Si, Especial',t:'Si, Especial'}]; setItems.call(this,'txtFNumerosa',fNumerosaItems); ensureValue.call(this,'txtFNumerosa',fNumerosaItems);\n");
        sb.append("  // Estudios + cursos\n");
        sb.append("  if(SELECTORS.txtEstudios){ setItems.call(this,'txtEstudios',SELECTORS.txtEstudios); ensureValue.call(this,'txtEstudios',SELECTORS.txtEstudios); }\n");
        sb.append("  if(SELECTORS.txtESO_Cursos){ setItems.call(this,'txtESO_Cursos',SELECTORS.txtESO_Cursos); ensureValue.call(this,'txtESO_Cursos',SELECTORS.txtESO_Cursos); }\n");
        sb.append("  if(SELECTORS.txtBACH_Cursos){ setItems.call(this,'txtBACH_Cursos',SELECTORS.txtBACH_Cursos); ensureValue.call(this,'txtBACH_Cursos',SELECTORS.txtBACH_Cursos); }\n");
        sb.append("  if(SELECTORS.txtCICLOS_Cursos){ setItems.call(this,'txtCICLOS_Cursos',SELECTORS.txtCICLOS_Cursos); ensureValue.call(this,'txtCICLOS_Cursos',SELECTORS.txtCICLOS_Cursos); }\n");
        sb.append("  if(SELECTORS.txtCICLOS_Grados){ setItems.call(this,'txtCICLOS_Grados',SELECTORS.txtCICLOS_Grados); ensureValue.call(this,'txtCICLOS_Grados',SELECTORS.txtCICLOS_Grados); }\n");
        sb.append("  if(SELECTORS.matriculaCompleta){ setItems.call(this,'matriculaCompleta',SELECTORS.matriculaCompleta); ensureValue.call(this,'matriculaCompleta',SELECTORS.matriculaCompleta); }\n");
        sb.append("  var dobleMatriculaItems=[{v:'0',t:'No'},{v:'1',t:'Si'}]; setItems.call(this,'txtDobleMatricula',dobleMatriculaItems); ensureValue.call(this,'txtDobleMatricula',dobleMatriculaItems);\n");
        sb.append("  // Child\n");
        sb.append("  if(SELECTORS.txtEstudios){ setItems.call(this,'child_txtEstudios',SELECTORS.txtEstudios); ensureValue.call(this,'child_txtEstudios',SELECTORS.txtEstudios); }\n");
        sb.append("  if(SELECTORS.txtESO_Cursos){ setItems.call(this,'child_txtESO_Cursos',SELECTORS.txtESO_Cursos); ensureValue.call(this,'child_txtESO_Cursos',SELECTORS.txtESO_Cursos); }\n");
        sb.append("  if(SELECTORS.txtBACH_Cursos){ setItems.call(this,'child_txtBACH_Cursos',SELECTORS.txtBACH_Cursos); ensureValue.call(this,'child_txtBACH_Cursos',SELECTORS.txtBACH_Cursos); }\n");
        sb.append("  if(SELECTORS.txtCICLOS_Cursos){ setItems.call(this,'child_txtCICLOS_Cursos',SELECTORS.txtCICLOS_Cursos); ensureValue.call(this,'child_txtCICLOS_Cursos',SELECTORS.txtCICLOS_Cursos); }\n");
        sb.append("  if(SELECTORS.txtCICLOS_Grados){ setItems.call(this,'child_txtCICLOS_Grados',SELECTORS.txtCICLOS_Grados); ensureValue.call(this,'child_txtCICLOS_Grados',SELECTORS.txtCICLOS_Grados); }\n");
        sb.append("  if(SELECTORS.matriculaCompleta){ setItems.call(this,'child_matriculaCompleta',SELECTORS.matriculaCompleta); ensureValue.call(this,'child_matriculaCompleta',SELECTORS.matriculaCompleta); }\n");
        sb.append("}\n");
        sb.append("function applySelectorVisibility(prefix){\n");
        sb.append("  var est = sval.call(this,prefix+'txtEstudios');\n");
        sb.append("  var labelPrefix = prefix ? 'child_' : '';\n");
        sb.append("  if(est==='txtESO' && SELECTORS.txtESO_Cursos){ ensureValue.call(this,prefix+'txtESO_Cursos', SELECTORS.txtESO_Cursos); }\n");
        sb.append("  if(est==='txtBachillerato' && SELECTORS.txtBACH_Cursos){ ensureValue.call(this,prefix+'txtBACH_Cursos', SELECTORS.txtBACH_Cursos); }\n");
        sb.append("  if(est==='txtCiclosFormativos' && SELECTORS.txtCICLOS_Cursos){ ensureValue.call(this,prefix+'txtCICLOS_Cursos', SELECTORS.txtCICLOS_Cursos); }\n");
        sb.append("  if(est==='txtCiclosFormativos' && SELECTORS.txtCICLOS_Grados){ ensureValue.call(this,prefix+'txtCICLOS_Grados', SELECTORS.txtCICLOS_Grados); }\n");
        sb.append("  showUiField.call(this,prefix+'txtESO_Cursos', est==='txtESO');\n");
        sb.append("  showUiLabel.call(this,labelPrefix+'lblEsoCurso', est==='txtESO');\n");
        sb.append("  showUiField.call(this,prefix+'txtBACH_Cursos', est==='txtBachillerato');\n");
        sb.append("  showUiLabel.call(this,labelPrefix+'lblBachCurso', est==='txtBachillerato');\n");
        sb.append("  showUiField.call(this,prefix+'txtCICLOS_Cursos', est==='txtCiclosFormativos');\n");
        sb.append("  showUiLabel.call(this,labelPrefix+'lblCiclosCurso', est==='txtCiclosFormativos');\n");
        sb.append("  showUiField.call(this,prefix+'txtCICLOS_Grados', est==='txtCiclosFormativos');\n");
        sb.append("  showUiLabel.call(this,labelPrefix+'lblGrado', est==='txtCiclosFormativos');\n");
        sb.append("  showUiField.call(this,prefix+'txtESO_Programa', est==='txtESO');\n");
        sb.append("  showUiLabel.call(this,labelPrefix+'lblPrograma', est==='txtESO');\n");
        sb.append("  showUiField.call(this,prefix+'txtESO_Itinerario', false);\n");
        sb.append("  showUiLabel.call(this,labelPrefix+'lblEsoItinerario', false);\n");
        sb.append("  showUiField.call(this,prefix+'txtBACH_Itinerario', est==='txtBachillerato');\n");
        sb.append("  showUiLabel.call(this,labelPrefix+'lblBachItinerario', est==='txtBachillerato');\n");
        sb.append("  showUiField.call(this,prefix+'matriculaCompleta', false);\n");
        sb.append("  showUiLabel.call(this,labelPrefix+'lblMatriculaCompleta', false);\n");
        sb.append("  if(est==='txtBachillerato' && String(sval.call(this,prefix+'txtBACH_Cursos'))==='2' && SELECTORS.matriculaCompleta){ ensureValue.call(this,prefix+'matriculaCompleta', SELECTORS.matriculaCompleta); }\n");
        sb.append("}\n");
        sb.append("function getScenarioKey(prefix){\n");
        sb.append("  var est = sval.call(this,prefix+'txtEstudios');\n");
        sb.append("  if(est==='txtESO'){\n");
        sb.append("    var curso = sval.call(this,prefix+'txtESO_Cursos')||'1';\n");
        sb.append("    // program options depend on course\n");
        sb.append("    var progField = f.call(this,prefix+'txtESO_Programa');\n");
        sb.append("    var opts = (SELECTORS.txtESO_Programas && SELECTORS.txtESO_Programas[String(curso)]) ? SELECTORS.txtESO_Programas[String(curso)] : [];\n");
        sb.append("    if(progField){ syncChoiceItems.call(this,prefix+'txtESO_Programa', opts); }\n");
        sb.append("    var programa = sval.call(this,prefix+'txtESO_Programa')|| (opts[0]?opts[0].v:'1');\n");
        sb.append("    var itinerario='0';\n");
        sb.append("    if(String(curso)==='4' && String(programa)!=='4'){\n");
        sb.append("      var itOpts = (SELECTORS.txtESO_C4_Itinerarios && SELECTORS.txtESO_C4_Itinerarios[String(programa)]) ? SELECTORS.txtESO_C4_Itinerarios[String(programa)] : (SELECTORS.txtESO_C4_Itinerarios?SELECTORS.txtESO_C4_Itinerarios['1']:[]);\n");
        sb.append("      var itField = f.call(this,prefix+'txtESO_Itinerario');\n");
        sb.append("      if(itField){ itField.display = display.visible; showUiLabel.call(this,(prefix?'child_':'')+'lblEsoItinerario', true); syncChoiceItems.call(this,prefix+'txtESO_Itinerario', itOpts); }\n");
        sb.append("      itinerario = sval.call(this,prefix+'txtESO_Itinerario') || (itOpts[0]?itOpts[0].v:'1');\n");
        sb.append("    } else {\n");
        sb.append("      var itField2 = f.call(this,prefix+'txtESO_Itinerario'); if(itField2){ itField2.display = display.hidden; showUiLabel.call(this,(prefix?'child_':'')+'lblEsoItinerario', false); clearChoice.call(this,prefix+'txtESO_Itinerario'); }\n");
        sb.append("    }\n");
        sb.append("    return (prefix?prefix:'') + 'ESO_C'+curso+'_P'+programa+'_It'+itinerario;\n");
        sb.append("  }\n");
        sb.append("  if(est==='txtBachillerato'){\n");
        sb.append("    var cursoB = sval.call(this,prefix+'txtBACH_Cursos')||'1';\n");
        sb.append("    var itOptsB = (SELECTORS.txtBACH_Itinerarios && SELECTORS.txtBACH_Itinerarios[String(cursoB)]) ? SELECTORS.txtBACH_Itinerarios[String(cursoB)] : [];\n");
        sb.append("    var itF = f.call(this,prefix+'txtBACH_Itinerario');\n");
        sb.append("    if(itF){ syncChoiceItems.call(this,prefix+'txtBACH_Itinerario', itOptsB); }\n");
        sb.append("    var itB = sval.call(this,prefix+'txtBACH_Itinerario') || (itOptsB[0]?itOptsB[0].v:'1');\n");
        sb.append("    // matriculaCompleta only for 2nd\n");
        sb.append("    var mcF = f.call(this,prefix+'matriculaCompleta');\n");
        sb.append("    if(mcF){ var showMc = (String(cursoB)==='2'); mcF.display = showMc ? display.visible : display.hidden; showUiLabel.call(this,(prefix?'child_':'')+'lblMatriculaCompleta', showMc); }\n");
        sb.append("    return (prefix?prefix:'') + 'BACH_C'+cursoB+'_P0_It'+itB;\n");
        sb.append("  }\n");
        sb.append("  if(est==='txtCiclosFormativos'){\n");
        sb.append("    var cursoC = sval.call(this,prefix+'txtCICLOS_Cursos')||'1';\n");
        sb.append("    var grado = sval.call(this,prefix+'txtCICLOS_Grados')||'1';\n");
        sb.append("    if(String(grado)==='1') return (prefix?prefix:'') + 'CICLOS_C'+cursoC+'_Grado_Medio_Pres';\n");
        sb.append("    if(String(grado)==='2') return (prefix?prefix:'') + 'CICLOS_C'+cursoC+'_Grado_Superior';\n");
        sb.append("    return (prefix?prefix:'') + 'CICLOS_C'+cursoC+'_Grado_Basico';\n");
        sb.append("  }\n");
        sb.append("  return '';\n");
        sb.append("}\n");
        sb.append("function applyScenario(prefix){\n");
        sb.append("  var key = getScenarioKey.call(this,prefix);\n");
        sb.append("  var data = key && SCENARIOS[key] ? SCENARIOS[key] : null;\n");
        sb.append("  var labelPrefix = prefix ? 'child_' : '';\n");
        sb.append("  for(var i=1;i<=10;i++){ var tf=f.call(this,prefix+'txtOptLibre_'+i); if(tf){ tf.display = display.hidden; tf.value=''; } showUiLabel.call(this,labelPrefix+'lblLibre'+i, false); }\n");
        sb.append("  if(data===null){\n");
        sb.append("    for(var j=1;j<=11;j++){ var of=f.call(this,prefix+'optativa_'+j); if(of){ of.display=display.hidden; clearChoice.call(this,prefix+'optativa_'+j); } showUiLabel.call(this,labelPrefix+'lblOpt'+j, false); }\n");
        sb.append("    for(var k=1;k<=3;k++){ var tr=f.call(this,prefix+'optTroncal_'+k); if(tr){ tr.display=display.hidden; clearChoice.call(this,prefix+'optTroncal_'+k); } showUiLabel.call(this,labelPrefix+'lblTr'+k, false); var co=f.call(this,prefix+'optComun_'+k); if(co){ co.display=display.hidden; clearChoice.call(this,prefix+'optComun_'+k); } showUiLabel.call(this,labelPrefix+'lblC'+k, false); }\n");
        sb.append("    return;\n");
        sb.append("  }\n");
        sb.append("  var est = sval.call(this,prefix+'txtEstudios');\n");
        sb.append("  var isBach = (est==='txtBachillerato');\n");
        sb.append("  var cursoB = isBach ? sval.call(this,prefix+'txtBACH_Cursos') : '';\n");
        sb.append("  var mc = isBach && String(cursoB)==='2' ? sval.call(this, prefix+'matriculaCompleta') : '1';\n");
        sb.append("  var useFree = isBach && String(cursoB)==='2' && String(mc)==='0';\n");
        sb.append("  if(useFree){\n");
        sb.append("    for(var i2=1;i2<=11;i2++){ var of2=f.call(this,prefix+'optativa_'+i2); if(of2){ of2.display=display.hidden; clearChoice.call(this,prefix+'optativa_'+i2); } showUiLabel.call(this,labelPrefix+'lblOpt'+i2, false); }\n");
        sb.append("    for(var k2=1;k2<=3;k2++){ var tr2=f.call(this,prefix+'optTroncal_'+k2); if(tr2){ tr2.display=display.hidden; clearChoice.call(this,prefix+'optTroncal_'+k2); } showUiLabel.call(this,labelPrefix+'lblTr'+k2, false); var co2=f.call(this,prefix+'optComun_'+k2); if(co2){ co2.display=display.hidden; clearChoice.call(this,prefix+'optComun_'+k2); } showUiLabel.call(this,labelPrefix+'lblC'+k2, false); }\n");
        sb.append("    for(var i3=1;i3<=10;i3++){ var tf2=f.call(this,prefix+'txtOptLibre_'+i3); if(tf2){ tf2.display = display.visible; } showUiLabel.call(this,labelPrefix+'lblLibre'+i3, true); }\n");
        sb.append("    return;\n");
        sb.append("  }\n");
        sb.append("  for(var t=1;t<=3;t++){ var trf=f.call(this,prefix+'optTroncal_'+t); var tro=data.tron && data.tron[String(t)] ? data.tron[String(t)] : null; if(trf){ if(tro && tro.length>0){ trf.display=display.visible; setItems.call(this,prefix+'optTroncal_'+t, tro); ensureValue.call(this,prefix+'optTroncal_'+t, tro); showUiLabel.call(this,labelPrefix+'lblTr'+t, true);} else { trf.display=display.hidden; showUiLabel.call(this,labelPrefix+'lblTr'+t, false); clearChoice.call(this,prefix+'optTroncal_'+t); } } }\n");
        sb.append("  for(var c=1;c<=3;c++){ var cof=f.call(this,prefix+'optComun_'+c); var coo=data.com && data.com[String(c)] ? data.com[String(c)] : null; if(cof){ if(coo && coo.length>0){ cof.display=display.visible; setItems.call(this,prefix+'optComun_'+c, coo); ensureValue.call(this,prefix+'optComun_'+c, coo); showUiLabel.call(this,labelPrefix+'lblC'+c, true);} else { cof.display=display.hidden; showUiLabel.call(this,labelPrefix+'lblC'+c, false); clearChoice.call(this,prefix+'optComun_'+c); } } }\n");
        sb.append("  for(var o=1;o<=11;o++){ var of=f.call(this,prefix+'optativa_'+o); var oo=data.opt && data.opt[String(o)] ? data.opt[String(o)] : null; if(of){ if(oo && oo.length>0){ of.display=display.visible; setItems.call(this,prefix+'optativa_'+o, oo); ensureValue.call(this,prefix+'optativa_'+o, oo); showUiLabel.call(this,labelPrefix+'lblOpt'+o, true);} else { of.display=display.hidden; showUiLabel.call(this,labelPrefix+'lblOpt'+o, false); clearChoice.call(this,prefix+'optativa_'+o); } } }\n");
        sb.append("}\n");
        sb.append("function buildHeader(){\n");
        sb.append("  var est = sval.call(this,'txtEstudios');\n");
        sb.append("  var line1=''; var line2=''; var cursoTxt='';\n");
        sb.append("  if(est==='txtESO'){ cursoTxt = sval.call(this,'txtESO_Cursos'); line1 = 'EDUCACION SECUNDARIA OBLIGATORIA (ESO)'; var prog=sval.call(this,'txtESO_Programa'); var it=sval.call(this,'txtESO_Itinerario'); line2 = (prog?('PROGRAMA '+prog+' '):'') + (it?('ITINERARIO '+it):''); }\n");
        sb.append("  if(est==='txtBachillerato'){ cursoTxt = sval.call(this,'txtBACH_Cursos'); line1 = (cursoTxt? (cursoTxt+' DE BACHILLERATO'): 'BACHILLERATO'); var itb=sval.call(this,'txtBACH_Itinerario'); line2 = itb?('ITINERARIO '+itb):''; }\n");
        sb.append("  if(est==='txtCiclosFormativos'){ cursoTxt = sval.call(this,'txtCICLOS_Cursos'); var g=sval.call(this,'txtCICLOS_Grados'); line1='CICLOS FORMATIVOS DE FP'; line2='CURSO '+cursoTxt+' / GRADO '+g; }\n");
        sb.append("  var f1=f.call(this,'printEstudio1'); if(f1) f1.value=line1; var f2=f.call(this,'printEstudio2'); if(f2) f2.value=line2; var fc=f.call(this,'printCurso'); if(fc) fc.value=cursoTxt;\n");
        sb.append("}\n");
        sb.append("function syncCopies(){ var ss=sval.call(this,'txtNumSS'); var em=sval.call(this,'txtEmailAlumno'); var ss2=f.call(this,'txtNumSS2'); if(ss2) ss2.value=ss; var em2=f.call(this,'txtEmailAlumno2'); if(em2) em2.value=em; }\n");
        sb.append("function updateResumen(){\n");
        sb.append("  var prevSuspend = __suspendCascade; __suspendCascade = true;\n");
        sb.append("  try{\n");
        sb.append("  var lines=[];\n");
        sb.append("  function add(prefix,label){ var key=getScenarioKey.call(this,prefix); if(!key) return; var est=sval.call(this,prefix+'txtEstudios'); lines.push(label+':'); lines.push('  Estudios: '+est); if(est==='txtESO'){ lines.push('  Curso: '+sval.call(this,prefix+'txtESO_Cursos')); lines.push('  Programa: '+sval.call(this,prefix+'txtESO_Programa')); var it=sval.call(this,prefix+'txtESO_Itinerario'); if(it) lines.push('  Itinerario: '+it); }");
        sb.append(" if(est==='txtBachillerato'){ lines.push('  Curso: '+sval.call(this,prefix+'txtBACH_Cursos')); lines.push('  Itinerario: '+sval.call(this,prefix+'txtBACH_Itinerario')); var mc=sval.call(this,prefix+'matriculaCompleta'); if(String(sval.call(this,prefix+'txtBACH_Cursos'))==='2') lines.push('  Matricula completa: '+(String(mc)==='0'?'No':'Si')); }");
        sb.append(" if(est==='txtCiclosFormativos'){ lines.push('  Curso: '+sval.call(this,prefix+'txtCICLOS_Cursos')); lines.push('  Grado: '+sval.call(this,prefix+'txtCICLOS_Grados')); }");
        sb.append("  var isBach=(est==='txtBachillerato'); var isB2=(String(sval.call(this,prefix+'txtBACH_Cursos'))==='2'); var useFree=isBach && isB2 && String(sval.call(this,prefix+'matriculaCompleta'))==='0';\n");
        sb.append("  if(useFree){ lines.push('  Asignaturas (texto libre):'); for(var i=1;i<=10;i++){ var v=sval.call(this,prefix+'txtOptLibre_'+i); if(v) lines.push('    - '+v); } } else {\n");
        sb.append("    var any=false; for(var t=1;t<=3;t++){ var v1=sval.call(this,prefix+'optTroncal_'+t); if(v1){ if(!any){ lines.push('  Seleccion:'); any=true;} lines.push('    - Troncal '+t+': '+v1);} }\n");
        sb.append("    for(var c=1;c<=3;c++){ var v2=sval.call(this,prefix+'optComun_'+c); if(v2){ if(!any){ lines.push('  Seleccion:'); any=true;} lines.push('    - Comun '+c+': '+v2);} }\n");
        sb.append("    for(var o=1;o<=11;o++){ var v3=sval.call(this,prefix+'optativa_'+o); if(v3){ if(!any){ lines.push('  Seleccion:'); any=true;} lines.push('    - Optativa '+o+': '+v3);} }\n");
        sb.append("  }\n");
        sb.append("  lines.push(''); }\n");
        sb.append("  add.call(this,'','Matricula principal');\n");
        sb.append("  if(String(sval.call(this,'txtDobleMatricula'))==='1'){ add.call(this,'child_','Repeticion'); }\n");
        sb.append("  var r=f.call(this,'ResumenAcademico'); if(r) r.value = lines.join('\\n');\n");
        sb.append("  } finally { __suspendCascade = prevSuspend; }\n");
        sb.append("}\n");
        sb.append("function setUiMode(mode){ __uiMode = mode; var lbl=f.call(this,'uiModeLabel'); if(lbl) lbl.value = (mode==='child')?'Edicion: Repeticion':'Edicion: Principal';\n");
        sb.append("  if(__preview) return;\n");
        sb.append("  var mainFields=['txtEstudios','txtESO_Cursos','txtBACH_Cursos','txtCICLOS_Cursos','txtCICLOS_Grados','txtESO_Programa','txtESO_Itinerario','txtBACH_Itinerario','matriculaCompleta','txtDobleMatricula'];\n");
        sb.append("  var childFields=['child_txtEstudios','child_txtESO_Cursos','child_txtBACH_Cursos','child_txtCICLOS_Cursos','child_txtCICLOS_Grados','child_txtESO_Programa','child_txtESO_Itinerario','child_txtBACH_Itinerario','child_matriculaCompleta'];\n");
        sb.append("  var mainLabels=['lblEstudios','lblEsoCurso','lblBachCurso','lblCiclosCurso','lblGrado','lblPrograma','lblEsoItinerario','lblBachItinerario','lblMatriculaCompleta','lblDoble'];\n");
        sb.append("  var childLabels=['child_lblEstudios','child_lblEsoCurso','child_lblBachCurso','child_lblCiclosCurso','child_lblGrado','child_lblPrograma','child_lblEsoItinerario','child_lblBachItinerario','child_lblMatriculaCompleta'];\n");
        sb.append("  for(var i=1;i<=11;i++){ mainFields.push('optativa_'+i); childFields.push('child_optativa_'+i); mainLabels.push('lblOpt'+i); childLabels.push('child_lblOpt'+i); }\n");
        sb.append("  for(var t=1;t<=3;t++){ mainFields.push('optTroncal_'+t); mainFields.push('optComun_'+t); childFields.push('child_optTroncal_'+t); childFields.push('child_optComun_'+t); mainLabels.push('lblTr'+t); mainLabels.push('lblC'+t); childLabels.push('child_lblTr'+t); childLabels.push('child_lblC'+t); }\n");
        sb.append("  for(var j=1;j<=10;j++){ mainFields.push('txtOptLibre_'+j); childFields.push('child_txtOptLibre_'+j); mainLabels.push('lblLibre'+j); childLabels.push('child_lblLibre'+j); }\n");
        sb.append("  function showFieldList(lst, show){ for(var k=0;k<lst.length;k++){ showUiField.call(this,lst[k], show); } }\n");
        sb.append("  function showLabelList(lst, show){ for(var k=0;k<lst.length;k++){ showUiLabel.call(this,lst[k], show); } }\n");
        sb.append("  if(mode==='child'){ showFieldList.call(this, mainFields, false); showLabelList.call(this, mainLabels, false); showFieldList.call(this, childFields, true); showLabelList.call(this, childLabels, true); }\n");
        sb.append("  else { showFieldList.call(this, childFields, false); showLabelList.call(this, childLabels, false); showFieldList.call(this, mainFields, true); showLabelList.call(this, mainLabels, true); }\n");
        sb.append("  var dm = String(sval.call(this,'txtDobleMatricula'))==='1';\n");
        sb.append("  if(!dm){ showFieldList.call(this, childFields, false); showLabelList.call(this, childLabels, false); }\n");
        sb.append("}\n");
        sb.append("function toggleUiMode(){\n");
        sb.append("  if(String(sval.call(this,'txtDobleMatricula'))!=='1'){ setUiMode.call(this,'main'); return; }\n");
        sb.append("  var next = (__uiMode==='child') ? 'main' : 'child';\n");
        sb.append("  setUiMode.call(this,next);\n");
        sb.append("}\n");
        sb.append("function setPreview(on){ __preview=on; var r=f.call(this,'ResumenAcademico'); if(r){ r.display = on ? display.visible : display.hidden; }\n");
        sb.append("  var mainFields=['txtEstudios','txtESO_Cursos','txtBACH_Cursos','txtCICLOS_Cursos','txtCICLOS_Grados','txtESO_Programa','txtESO_Itinerario','txtBACH_Itinerario','matriculaCompleta','txtDobleMatricula'];\n");
        sb.append("  var childFields=['child_txtEstudios','child_txtESO_Cursos','child_txtBACH_Cursos','child_txtCICLOS_Cursos','child_txtCICLOS_Grados','child_txtESO_Programa','child_txtESO_Itinerario','child_txtBACH_Itinerario','child_matriculaCompleta'];\n");
        sb.append("  var labels=['uiModeLabel','lblEstudios','lblEsoCurso','lblBachCurso','lblCiclosCurso','lblGrado','lblPrograma','lblEsoItinerario','lblBachItinerario','lblMatriculaCompleta','lblDoble','child_lblEstudios','child_lblEsoCurso','child_lblBachCurso','child_lblCiclosCurso','child_lblGrado','child_lblPrograma','child_lblEsoItinerario','child_lblBachItinerario','child_lblMatriculaCompleta'];\n");
        sb.append("  for(var i=1;i<=11;i++){ mainFields.push('optativa_'+i); childFields.push('child_optativa_'+i); labels.push('lblOpt'+i); labels.push('child_lblOpt'+i); }\n");
        sb.append("  for(var t=1;t<=3;t++){ mainFields.push('optTroncal_'+t); mainFields.push('optComun_'+t); childFields.push('child_optTroncal_'+t); childFields.push('child_optComun_'+t); labels.push('lblTr'+t); labels.push('lblC'+t); labels.push('child_lblTr'+t); labels.push('child_lblC'+t); }\n");
        sb.append("  for(var j=1;j<=10;j++){ mainFields.push('txtOptLibre_'+j); childFields.push('child_txtOptLibre_'+j); labels.push('lblLibre'+j); labels.push('child_lblLibre'+j); }\n");
        sb.append("  if(on){\n");
        sb.append("    for(var a=0;a<mainFields.length;a++){ showUiField.call(this,mainFields[a], false); }\n");
        sb.append("    for(var b=0;b<childFields.length;b++){ showUiField.call(this,childFields[b], false); }\n");
        sb.append("    for(var c=0;c<labels.length;c++){ showUiLabel.call(this,labels[c], false); }\n");
        sb.append("    showUiField.call(this,'txtLugar', false);\n");
        sb.append("    showUiField.call(this,'txtFecha', false);\n");
        sb.append("    showUiField.call(this,'btnValidate', false);\n");
        sb.append("  } else {\n");
        sb.append("    showUiField.call(this,'txtLugar', true);\n");
        sb.append("    showUiField.call(this,'txtFecha', true);\n");
        sb.append("    showUiField.call(this,'btnValidate', true);\n");
        sb.append("    setUiMode.call(this, __uiMode);\n");
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("function togglePreview(){ setPreview.call(this,!__preview); var b=f.call(this,'btnTogglePreview'); if(b){ try{ b.buttonSetCaption(__preview?'Editar':'Vista previa'); }catch(e){} } }\n");
        sb.append("function updateAll(){\n");
        sb.append("  var prevSuspend = __suspendCascade; __suspendCascade = true;\n");
        sb.append("  try{\n");
        sb.append("  applySelectorVisibility.call(this,''); applyScenario.call(this,'');\n");
        sb.append("  // child scenario only if double\n");
        sb.append("  if(String(sval.call(this,'txtDobleMatricula'))==='1'){ applySelectorVisibility.call(this,'child_'); applyScenario.call(this,'child_'); }\n");
        sb.append("  buildHeader.call(this); syncCopies.call(this); updateResumen.call(this);\n");
        sb.append("  // Labels\n");
        sb.append("  var L = {lblEstudios:'Estudios', lblEsoCurso:'Curso ESO', lblBachCurso:'Curso Bach.', lblCiclosCurso:'Curso CFG', lblGrado:'Grado', lblPrograma:'Programa', lblEsoItinerario:'Itin. ESO', lblBachItinerario:'Itin. Bach.', lblMatriculaCompleta:'Curso completo', lblDoble:'Doble matricula'};\n");
        sb.append("  var prefixes=['','child_'];\n");
        sb.append("  for(var p=0;p<prefixes.length;p++){ var px=prefixes[p]; for(var k in L){ var lf=f.call(this,px+k); if(lf) lf.value=L[k]; } }\n");
        sb.append("  for(var i=1;i<=11;i++){ for(var p2=0;p2<prefixes.length;p2++){ var lf2=f.call(this,prefixes[p2]+'lblOpt'+i); if(lf2) lf2.value='O'+i; } }\n");
        sb.append("  for(var i2=1;i2<=3;i2++){ for(var p3=0;p3<prefixes.length;p3++){ var pf=prefixes[p3]; var lt=f.call(this,pf+'lblTr'+i2); if(lt) lt.value='T'+i2; var lc=f.call(this,pf+'lblC'+i2); if(lc) lc.value='C'+i2; } }\n");
        sb.append("  for(var i3=1;i3<=10;i3++){ for(var p4=0;p4<prefixes.length;p4++){ var lf3=f.call(this,prefixes[p4]+'lblLibre'+i3); if(lf3) lf3.value='L'+i3; } }\n");
        sb.append("  } finally { __suspendCascade = prevSuspend; }\n");
        sb.append("}\n");
        sb.append("function validateAll(){\n");
        sb.append("  var req=['txtApellido1','txtNombre','txtDireccion','txtPoblacion','txtCodPostal','txtProvincia','txtTfnoCasa','txtPoblacionNac','txtPais','txtDiaNac','txtMesNac','txtAnioNac','txtNacionalidad'];\n");
        sb.append("  var labels={'txtApellido1':'1er Apellido','txtNombre':'Nombre','txtDireccion':'Domicilio','txtPoblacion':'Localidad','txtCodPostal':'C.P.','txtProvincia':'Provincia','txtTfnoCasa':'Tfno. Casa','txtPoblacionNac':'Municipio nacimiento','txtPais':'Pais nacimiento','txtDiaNac':'Dia nac.','txtMesNac':'Mes nac.','txtAnioNac':'Anio nac.','txtNacionalidad':'Nacionalidad'};\n");
        sb.append("  for(var i=0;i<req.length;i++){ var n=req[i]; var v=sval.call(this,n); if(!v){ app.alert('El campo '+labels[n]+' es obligatorio'); var ff=f.call(this,n); if(ff) ff.setFocus(); return false; } }\n");
        sb.append("  function numLen(n,len,msg){ var v=sval.call(this,n); if(v && (!/^\\d+$/.test(v) || v.length!=len)){ app.alert(msg); var ff=f.call(this,n); if(ff) ff.setFocus(); return false; } return true; }\n");
        sb.append("  if(!numLen.call(this,'txtCodPostal',5,'El C.P. debe tener 5 numeros')) return false;\n");
        sb.append("  if(!numLen.call(this,'txtDiaNac',2,'El DIA debe tener 2 numeros')) return false;\n");
        sb.append("  if(!numLen.call(this,'txtMesNac',2,'El MES debe tener 2 numeros')) return false;\n");
        sb.append("  if(!numLen.call(this,'txtAnioNac',4,'El ANIO debe tener 4 numeros')) return false;\n");
        sb.append("  if(!sval.call(this,'txtSexo')){ app.alert('Debe seleccionar una opcion en Genero'); var ff=f.call(this,'txtSexo'); if(ff) ff.setFocus(); return false; }\n");
        sb.append("  if(!isChecked.call(this,'leidoPolitica') || !isChecked.call(this,'aceptoPolitica')){ app.alert('Se debe LEER y ACEPTAR la Politica de Privacidad'); return false; }\n");
        sb.append("  function dupCheck(prefix){ var arr=[]; var names=[]; for(var t=1;t<=3;t++){ names.push(prefix+'optTroncal_'+t); names.push(prefix+'optComun_'+t);} for(var o=1;o<=11;o++){ names.push(prefix+'optativa_'+o);} for(var i=0;i<names.length;i++){ var v=sval.call(this,names[i]); if(v && v!=='NINGUNA'){ arr.push(v); } }\n");
        sb.append("    var seen={}; for(var j=0;j<arr.length;j++){ var x=arr[j]; if(seen[x]){ app.alert('Obligatorio seleccionar asignaturas sin repetir: '+x); return false; } seen[x]=true; } return true; }\n");
        sb.append("  if(!dupCheck.call(this,'')) return false;\n");
        sb.append("  if(String(sval.call(this,'txtDobleMatricula'))==='1'){ if(!dupCheck.call(this,'child_')) return false; }\n");
        sb.append("  app.alert('Validacion correcta'); return true;\n");
        sb.append("}\n");
        sb.append("function initMatricula(){\n");
        sb.append("  var prevSuspend = __suspendCascade; __suspendCascade = true;\n");
        sb.append("  try{\n");
        sb.append("  try{ app.runtimeHighlight = false; }catch(e){}\n");
        sb.append("  setPreview.call(this,false); setUiMode.call(this,'main');\n");
        sb.append("  setupCombos.call(this);\n");
        sb.append("  var b=f.call(this,'btnTogglePreview'); if(b){ try{ b.buttonSetCaption('Vista previa'); }catch(e){} }\n");
        sb.append("  var v=f.call(this,'btnValidate'); if(v){ try{ v.buttonSetCaption('Validar'); }catch(e){} }\n");
        sb.append("  } finally { __suspendCascade = prevSuspend; }\n");
        sb.append("  updateAll.call(this);\n");
        sb.append("}\n");
        return sb.toString();
    }
}
