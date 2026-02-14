package com.compara.retorno.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public byte[] generateAnaliseReport(List<TransactionService.ComparisonResult> results, 
                                      LocalDate startDate, 
                                      LocalDate endDate) throws DocumentException {
        
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        document.setMargins(10, 10, 10, 10);
        PdfWriter.getInstance(document, outputStream);
        
        document.open();
        
        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Relatório de Análise API x Geral", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Period
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        String periodText = String.format("Período: %s a %s", 
            startDate.format(DATE_FORMATTER), 
            endDate.format(DATE_FORMATTER));
        Paragraph period = new Paragraph(periodText, subTitleFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(20);
        document.add(period);
        
        // Table
        PdfPTable table = new PdfPTable(8); // Columns: Status, Nosso Numero, Pagador, Data Pag., Data Cred., Valor API, Valor Geral, Diferença
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2f, 4f, 2f, 2f, 2f, 2f, 2f});
        
        // Header
        addTableHeader(table, "Status");
        addTableHeader(table, "Nosso Número");
        addTableHeader(table, "Pagador");
        addTableHeader(table, "Data Pag.");
        addTableHeader(table, "Data Créd.");
        addTableHeader(table, "Valor API");
        addTableHeader(table, "Valor Geral");
        addTableHeader(table, "Diferença");
        
        // Data
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9); // Reduced font size to fit
        
        BigDecimal totalApi = BigDecimal.ZERO;
        BigDecimal totalGeral = BigDecimal.ZERO;
        BigDecimal totalDiff = BigDecimal.ZERO;
        
        for (TransactionService.ComparisonResult result : results) {
            // Status
            String status = result.isDivergent() ? "DIVERGENTE" : "CONCILIADO";
            PdfPCell statusCell = new PdfPCell(new Phrase(status, cellFont));
            if (result.isDivergent()) {
                statusCell.setBackgroundColor(new Color(255, 200, 200)); // Light Red
            } else {
                statusCell.setBackgroundColor(new Color(200, 255, 200)); // Light Green
            }
            table.addCell(statusCell);
            
            table.addCell(new Phrase(result.getNossoNumero(), cellFont));
            table.addCell(new Phrase(result.getNomePagador(), cellFont));
            
            LocalDate mainDate = result.getMainDate();
            table.addCell(new Phrase(mainDate != null ? mainDate.format(DATE_FORMATTER) : "-", cellFont));
            
            LocalDate creditDate = result.getDataCredito();
            table.addCell(new Phrase(creditDate != null ? creditDate.format(DATE_FORMATTER) : "-", cellFont));
            
            // Values logic
            BigDecimal valApi = BigDecimal.ZERO;
            if (result.getApiTransaction() != null && result.getApiTransaction().getValorPago() != null) {
                valApi = result.getApiTransaction().getValorPago();
            }
            
            BigDecimal valGeral = BigDecimal.ZERO;
            if (result.getGeralTransaction() != null && result.getGeralTransaction().getValorPago() != null) {
                valGeral = result.getGeralTransaction().getValorPago();
            }
            
            BigDecimal diff = valApi.subtract(valGeral);
            if (!result.isDivergent()) {
                 // If not divergent, technically difference is zero, but let's calculate to be sure
                 // Usually for reconciled, it should be exact match
            }
            
            // Accumulate totals
            // Note: Only accumulate if the transaction exists in that context?
            // Yes, if API exists, add to API total. If Geral exists, add to Geral total.
            // If it's "SOMENTE API", Geral is 0. If "SOMENTE GERAL", API is 0.
            
            totalApi = totalApi.add(valApi);
            totalGeral = totalGeral.add(valGeral);
            totalDiff = totalDiff.add(diff);

            // Add cells
            table.addCell(new Phrase(result.getApiTransaction() != null ? CURRENCY_FORMATTER.format(valApi) : "-", cellFont));
            table.addCell(new Phrase(result.getGeralTransaction() != null ? CURRENCY_FORMATTER.format(valGeral) : "-", cellFont));
            
            PdfPCell diffCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(diff), cellFont));
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                 diffCell.setBackgroundColor(new Color(255, 255, 200)); // Light Yellow for difference
            }
            table.addCell(diffCell);
        }
        
        // Totals Row
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAIS", boldFont));
        totalLabel.setColspan(5); // Status + Nosso + Pagador + Data Pag + Data Cred
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalLabel);
        
        PdfPCell totalApiCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(totalApi), boldFont));
        totalApiCell.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalApiCell);
        
        PdfPCell totalGeralCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(totalGeral), boldFont));
        totalGeralCell.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalGeralCell);
        
        PdfPCell totalDiffCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(totalDiff), boldFont));
        totalDiffCell.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalDiffCell);

        document.add(table);
        
        // Summary
        document.add(new Paragraph("\n"));
        Paragraph summary = new Paragraph(String.format("Total de registros: %d", results.size()), subTitleFont);
        document.add(summary);
        
        document.close();
        
        return outputStream.toByteArray();
    }
    
    private void addTableHeader(PdfPTable table, String headerTitle) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(Color.LIGHT_GRAY);
        header.setBorderWidth(2);
        header.setPhrase(new Phrase(headerTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(header);
    }
}
