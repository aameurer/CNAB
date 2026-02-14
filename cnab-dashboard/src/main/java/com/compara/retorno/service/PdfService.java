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
        PdfPTable table = new PdfPTable(8); // Columns: Status, Nosso Numero, Pagador, Data Pag., Data Cred., Valor API,
                                            // Valor Geral, Diferença
        table.setWidthPercentage(100);
        // Adjusted widths to prevent wrapping: Status=1.8, NossoNum=2.7, Pagador=5.5,
        // Dates/Values=1.7
        table.setWidths(new float[] { 1.8f, 2.7f, 5.5f, 1.7f, 1.7f, 1.7f, 1.7f, 1.7f });

        // Header
        addTableHeader(table, "Status", Element.ALIGN_LEFT);
        addTableHeader(table, "Nosso Número", Element.ALIGN_LEFT);
        addTableHeader(table, "Pagador", Element.ALIGN_LEFT);
        addTableHeader(table, "Data Pag.", Element.ALIGN_CENTER);
        addTableHeader(table, "Data Créd.", Element.ALIGN_CENTER);
        addTableHeader(table, "Valor API", Element.ALIGN_RIGHT);
        addTableHeader(table, "Valor Geral", Element.ALIGN_RIGHT);
        addTableHeader(table, "Diferença", Element.ALIGN_RIGHT);

        // Data
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        BigDecimal totalApi = BigDecimal.ZERO;
        BigDecimal totalGeral = BigDecimal.ZERO;
        BigDecimal totalDiff = BigDecimal.ZERO;

        for (TransactionService.ComparisonResult result : results) {
            // Helper to add aligned cells
            // Status
            String status = result.isDivergent() ? "DIVERGENTE" : "CONCILIADO";
            PdfPCell statusCell = createCell(status, cellFont, Element.ALIGN_LEFT);
            if (result.isDivergent()) {
                statusCell.setBackgroundColor(new Color(255, 200, 200)); // Light Red
            } else {
                statusCell.setBackgroundColor(new Color(200, 255, 200)); // Light Green
            }
            table.addCell(statusCell);

            table.addCell(createCell(result.getNossoNumero(), cellFont, Element.ALIGN_LEFT));
            table.addCell(createCell(result.getNomePagador(), cellFont, Element.ALIGN_LEFT));

            LocalDate mainDate = result.getMainDate();
            table.addCell(createCell(mainDate != null ? mainDate.format(DATE_FORMATTER) : "-", cellFont,
                    Element.ALIGN_CENTER));

            LocalDate creditDate = result.getDataCredito();
            table.addCell(createCell(creditDate != null ? creditDate.format(DATE_FORMATTER) : "-", cellFont,
                    Element.ALIGN_CENTER));

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

            totalApi = totalApi.add(valApi);
            totalGeral = totalGeral.add(valGeral);
            totalDiff = totalDiff.add(diff);

            // Add cells
            table.addCell(createCell(result.getApiTransaction() != null ? CURRENCY_FORMATTER.format(valApi) : "-",
                    cellFont, Element.ALIGN_RIGHT));
            table.addCell(createCell(result.getGeralTransaction() != null ? CURRENCY_FORMATTER.format(valGeral) : "-",
                    cellFont, Element.ALIGN_RIGHT));

            PdfPCell diffCell = createCell(CURRENCY_FORMATTER.format(diff), cellFont, Element.ALIGN_RIGHT);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                diffCell.setBackgroundColor(new Color(255, 255, 200)); // Light Yellow
            }
            table.addCell(diffCell);
        }

        // Totals Row
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAIS", boldFont));
        totalLabel.setColspan(5);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalLabel);

        PdfPCell totalApiCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(totalApi), boldFont));
        totalApiCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalApiCell.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalApiCell);

        PdfPCell totalGeralCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(totalGeral), boldFont));
        totalGeralCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalGeralCell.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalGeralCell);

        PdfPCell totalDiffCell = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(totalDiff), boldFont));
        totalDiffCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
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

    public byte[] generateTransactionsReport(List<com.compara.retorno.model.Transacao> transactions)
            throws DocumentException {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        document.setMargins(10, 10, 10, 10);
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Relatório de Transações", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph("\n"));

        // Table
        PdfPTable table = new PdfPTable(7); // Columns: Origem, Nosso Numero, Pagador, Valor, Data Ocorr., Data Cred.,
                                            // Status
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1.5f, 2.5f, 5.0f, 2.0f, 2.0f, 2.0f, 2.0f });

        // Header
        addTableHeader(table, "Origem", Element.ALIGN_LEFT);
        addTableHeader(table, "Nosso Número", Element.ALIGN_LEFT);
        addTableHeader(table, "Pagador", Element.ALIGN_LEFT);
        addTableHeader(table, "Valor", Element.ALIGN_RIGHT);
        addTableHeader(table, "Data Ocorr.", Element.ALIGN_CENTER);
        addTableHeader(table, "Data Créd.", Element.ALIGN_CENTER);
        addTableHeader(table, "Status", Element.ALIGN_LEFT);

        // Data
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        BigDecimal totalValor = BigDecimal.ZERO;

        for (com.compara.retorno.model.Transacao t : transactions) {

            table.addCell(createCell(t.getTipoOrigem().name(), cellFont, Element.ALIGN_LEFT));
            table.addCell(createCell(t.getNossoNumero(), cellFont, Element.ALIGN_LEFT));
            table.addCell(createCell(t.getNomePagador(), cellFont, Element.ALIGN_LEFT));

            BigDecimal valor = t.getValorPago();
            totalValor = totalValor.add(valor != null ? valor : BigDecimal.ZERO);
            table.addCell(
                    createCell(valor != null ? CURRENCY_FORMATTER.format(valor) : "-", cellFont, Element.ALIGN_RIGHT));

            table.addCell(createCell(t.getDataOcorrencia() != null ? t.getDataOcorrencia().format(DATE_FORMATTER) : "-",
                    cellFont, Element.ALIGN_CENTER));
            table.addCell(createCell(t.getDataCredito() != null ? t.getDataCredito().format(DATE_FORMATTER) : "-",
                    cellFont, Element.ALIGN_CENTER));

            String status = t.getStatusConciliacao();
            PdfPCell statusCell = createCell(status, cellFont, Element.ALIGN_LEFT);
            if ("DIVERGENTE".equals(status)) {
                statusCell.setBackgroundColor(new Color(255, 200, 200));
            } else if ("CONCILIADO".equals(status)) {
                statusCell.setBackgroundColor(new Color(200, 255, 200));
            } else {
                statusCell.setBackgroundColor(new Color(255, 255, 200)); // Pending/Warning
            }
            table.addCell(statusCell);
        }

        // Totals
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL", boldFont));
        totalLabel.setColspan(3);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalLabel);

        PdfPCell totalValue = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(totalValor), boldFont));
        totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValue.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(totalValue);

        PdfPCell empty = new PdfPCell(new Phrase(""));
        empty.setColspan(3);
        empty.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(empty);

        document.add(table);

        document.add(new Paragraph("\n"));
        Paragraph summary = new Paragraph(String.format("Total de registros: %d", transactions.size()),
                FontFactory.getFont(FontFactory.HELVETICA, 12));
        document.add(summary);

        document.close();

        return outputStream.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String headerTitle, int alignment) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(Color.LIGHT_GRAY);
        header.setBorderWidth(2);
        header.setPhrase(new Phrase(headerTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        header.setHorizontalAlignment(alignment);
        header.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(header);
    }

    private PdfPCell createCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}
