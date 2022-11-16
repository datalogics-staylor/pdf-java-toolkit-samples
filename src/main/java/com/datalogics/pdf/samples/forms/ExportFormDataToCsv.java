/*
 * Copyright 2015 Datalogics, Inc.
 */

package com.datalogics.pdf.samples.forms;

import com.adobe.pdfjt.core.exceptions.PDFIOException;
import com.adobe.pdfjt.core.exceptions.PDFInvalidDocumentException;
import com.adobe.pdfjt.core.exceptions.PDFSecurityException;
import com.adobe.pdfjt.core.exceptions.PDFUnableToCompleteOperationException;
import com.adobe.pdfjt.core.license.LicenseManager;
import com.adobe.pdfjt.pdf.document.PDFDocument;
import com.adobe.pdfjt.pdf.interactive.forms.PDFField;
import com.adobe.pdfjt.pdf.interactive.forms.PDFInteractiveForm;

import com.datalogics.pdf.samples.util.DocumentUtils;
import com.datalogics.pdf.samples.util.IoUtils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

/**
 * This sample demonstrates exporting data from PDF form fields with formatting to a CSV file.
 */
public final class ExportFormDataToCsv {
    public static final String DEFAULT_INPUT = "filled_acroform.pdf";
    public static final String CSV_OUTPUT = "exported-form-data.csv";

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * This is a utility class, and won't be instantiated.
     */
    private ExportFormDataToCsv() {}

    /**
     * Export the form data from a PDF as a CSV file.
     *
     * @param args path to an input PDF and a path to write the CSV output to
     * @throws IOException an I/O operation failed or was interrupted
     * @throws PDFSecurityException some general security issue occurred during the processing of the request
     * @throws PDFIOException there was an error reading or writing a PDF file or temporary caches
     * @throws PDFInvalidDocumentException a general problem with the PDF document, which may now be in an invalid state
     * @throws PDFUnableToCompleteOperationException the operation was unable to be completed
     * @throws URISyntaxException a string could not be parsed as a URI reference
     */
    public static void main(final String[] args) throws PDFInvalidDocumentException, PDFIOException,
                    PDFSecurityException, IOException, PDFUnableToCompleteOperationException, URISyntaxException {
        // If you are using an evaluation version of the product (License Managed, or LM), set the path to where PDFJT
        // can find the license file.
        //
        // If you are not using an evaluation version of the product you can ignore or remove this code.
        LicenseManager.setLicensePath(".");

        URL inputUrl = null;
        URL outputUrl = null;
        if (args.length > 0) {
            inputUrl = IoUtils.createUrlFromPath(args[0]);
            outputUrl = IoUtils.createUrlFromPath(args[1]);
        } else {
            inputUrl = ExportFormDataToCsv.class.getResource(DEFAULT_INPUT);
            outputUrl = IoUtils.createUrlFromPath(CSV_OUTPUT);
        }

        exportFormFields(inputUrl, outputUrl);
    }

    /**
     * Export the form data to a comma separated form (CSV). The first row that is written out in the CSV contains the
     * qualified field names and the second row contains the field values.
     *
     * @param inputUrl path to the input PDF file
     * @param outputUrl path to write the CSV output file to
     * @throws IOException an I/O operation failed or was interrupted
     * @throws PDFSecurityException some general security issue occurred during the processing of the request
     * @throws PDFIOException there was an error reading or writing a PDF file or temporary caches
     * @throws PDFInvalidDocumentException a general problem with the PDF document, which may now be in an invalid state
     * @throws PDFUnableToCompleteOperationException the operation was unable to be completed
     * @throws URISyntaxException a string could not be parsed as a URI reference
     */
    public static void exportFormFields(final URL inputUrl, final URL outputUrl) throws PDFInvalidDocumentException,
                    PDFIOException, PDFSecurityException, IOException, PDFUnableToCompleteOperationException,
                    URISyntaxException {
        PDFDocument pdfDocument = null;

        try {
            pdfDocument = DocumentUtils.openPdfDocument(inputUrl);

            final PDFInteractiveForm form = pdfDocument.getInteractiveForm();

            final File outputFile = new File(outputUrl.toURI());
            if (outputFile.exists()) {
                Files.delete(outputFile.toPath());
            }

            try (PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL)) {

                exportFieldNames(form, printer);

                exportFieldValues(form, printer);

            }
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    /**
     * Export the form field names in comma separated form.
     *
     * @param form PDFInteractiveForm that will have its field values exported
     * @param printer CSVPrinter that will be used to write out the field names
     * @throws PDFSecurityException some general security issue occurred during the processing of the request
     * @throws PDFIOException there was an error reading or writing a PDF file or temporary caches
     * @throws PDFInvalidDocumentException a general problem with the PDF document, which may now be in an invalid state
     */
    private static void exportFieldNames(final PDFInteractiveForm form, final CSVPrinter printer)
                    throws PDFInvalidDocumentException, PDFIOException, PDFSecurityException, IOException {
        final Iterator<PDFField> fieldIterator = form.iterator();

        while (fieldIterator.hasNext()) {
            final PDFField field = fieldIterator.next();
            printer.print(field.getQualifiedName());
        }
        printer.println();
    }

    /**
     * Export the values in the form fields, using their formatted values, in comma separated form. If no value is found
     * in the field, a space is written instead.
     *
     * @param form PDFInteractiveForm that will have its field values exported
     * @param printer CSVPrinter that will be used to write out the field values
     * @throws PDFInvalidDocumentException a general problem with the PDF document, which may now be in an invalid state
     * @throws PDFIOException there was an error reading or writing a PDF file or temporary caches
     * @throws PDFSecurityException some general security issue occurred during the processing of the request
     */
    private static void exportFieldValues(final PDFInteractiveForm form, final CSVPrinter printer)
                    throws PDFInvalidDocumentException, PDFIOException, PDFSecurityException, IOException {
        final Iterator<PDFField> fieldIterator = form.iterator();
        while (fieldIterator.hasNext()) {
            final PDFField field = fieldIterator.next();

            final String formattedValue = field.getFormattedValue();
            if (formattedValue == null) {
                final List<?> value = field.getValueList();
                if (value == null) {
                    printer.print("");
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(field.getQualifiedName() + " has no value!");
                    }
                } else {
                    // If the list of values is not empty, write out only the first one.
                    // For more complex forms, the field type should be inspected to
                    // determine how to handle extracting the values.
                    printer.print(value.get(0).toString());
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(field.getQualifiedName()
                                    + " has no formatting rules! Writing out non formatted value!");
                    }
                }
            } else {
                printer.print(formattedValue);
            }
        }
    }
}
