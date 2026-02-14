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

    public void generateAnaliseReport(List<TransactionService.ComparisonResult> results, 
                                      LocalDate startDate, 
                                      LocalDate endDate, 
                                      OutputStream outputStream) throws DocumentException, IOException {
        
        Document document = new Document(PageSize.A4.rotate());
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
        PdfPTable table = new PdfPTable(6); // Columns: Status, Nosso Numero, Pagador, Data Pag., Data Cred., Valor
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2f, 4f, 2f, 2f, 2f});
        
        // Header
        addTableHeader(table, "Status");
        addTableHeader(table, "Nosso Número");
        addTableHeader(table, "Pagador");
        addTableHeader(table, "Data Pag.");
        addTableHeader(table, "Data Créd.");
        addTableHeader(table, "Valor Pago");
        
        // Data
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        
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
            
            BigDecimal valor = result.getValorPago();
            table.addCell(new Phrase(valor != null ? CURRENCY_FORMATTER.format(valor) : "-", cellFont));
        }
        
        document.add(table);
        
        // Summary
        document.add(new Paragraph("\n"));
        Paragraph summary = new Paragraph(String.format("Total de registros: %d", results.size()), subTitleFont);
        document.add(summary);
        
        document.close();
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
