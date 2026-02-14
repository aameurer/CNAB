package com.compara.retorno.service;

import com.compara.retorno.model.Contribuinte;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ContribuinteReportService {

    public byte[] generatePdf(List<Contribuinte> list) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate());
            doc.setMargins(10, 10, 10, 10);
            PdfWriter.getInstance(doc, out);
            doc.open();

            com.lowagie.text.Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph p = new Paragraph("Relatório de Contribuintes", title);
            p.setAlignment(Element.ALIGN_CENTER);
            doc.add(p);
            doc.add(new Paragraph("\n"));

            PdfPTable t = new PdfPTable(7);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{3f, 5f, 3f, 5f, 2.5f, 3f, 2.5f}); // cpf, nome, insc, endereço, bairro, atividade, situacao

            addHeader(t, "CPF/CNPJ");
            addHeader(t, "Nome");
            addHeader(t, "Inscrição");
            addHeader(t, "Endereço");
            addHeader(t, "Bairro");
            addHeader(t, "Atividade");
            addHeader(t, "Situação");

            com.lowagie.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (Contribuinte c : list) {
                t.addCell(new Phrase(maskCpfCnpj(c.getCpfCnpj()), cellFont));
                t.addCell(new Phrase(nvl(c.getNome()), cellFont));
                t.addCell(new Phrase(nvl(c.getInscricaoMunicipal()), cellFont));
                t.addCell(new Phrase(formatEndereco(c), cellFont));
                t.addCell(new Phrase(nvl(c.getBairro()), cellFont));
                t.addCell(new Phrase(nvl(c.getAtividadeEconomica()), cellFont));
                PdfPCell sit = new PdfPCell(new Phrase(nvl(c.getSituacaoCadastral()), cellFont));
                if ("ATIVO".equalsIgnoreCase(c.getSituacaoCadastral())) sit.setBackgroundColor(new Color(200,255,200));
                if ("BAIXA".equalsIgnoreCase(c.getSituacaoCadastral())) sit.setBackgroundColor(new Color(255,220,220));
                t.addCell(sit);
            }
            doc.add(t);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] generateExcel(List<Contribuinte> list) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("Contribuintes");
            int r = 0;
            org.apache.poi.ss.usermodel.Row header = s.createRow(r++);
            String[] cols = {"CPF/CNPJ", "Nome", "Inscrição", "Endereço", "Bairro", "Atividade", "Situação"};
            for (int i = 0; i < cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
            }
            for (Contribuinte c : list) {
                org.apache.poi.ss.usermodel.Row row = s.createRow(r++);
                row.createCell(0).setCellValue(maskCpfCnpj(c.getCpfCnpj()));
                row.createCell(1).setCellValue(nvl(c.getNome()));
                row.createCell(2).setCellValue(nvl(c.getInscricaoMunicipal()));
                row.createCell(3).setCellValue(formatEndereco(c));
                row.createCell(4).setCellValue(nvl(c.getBairro()));
                row.createCell(5).setCellValue(nvl(c.getAtividadeEconomica()));
                row.createCell(6).setCellValue(nvl(c.getSituacaoCadastral()));
            }
            for (int i = 0; i < 7; i++) s.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addHeader(PdfPTable t, String text) {
        PdfPCell h = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        h.setBackgroundColor(Color.LIGHT_GRAY);
        t.addCell(h);
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String formatEndereco(Contribuinte c) {
        StringBuilder sb = new StringBuilder();
        if (c.getLogradouro() != null) sb.append(c.getLogradouro());
        if (c.getNumero() != null) sb.append(", ").append(c.getNumero());
        if (c.getComplemento() != null) sb.append(" ").append(c.getComplemento());
        if (c.getCidade() != null) sb.append(" - ").append(c.getCidade());
        if (c.getEstado() != null) sb.append("/").append(c.getEstado());
        if (c.getCep() != null) sb.append(" CEP: ").append(c.getCep());
        return sb.toString().trim();
    }

    private String maskCpfCnpj(String digits) {
        if (digits == null) return "";
        String d = digits.replaceAll("\\D", "");
        if (d.length() == 11) {
            return d.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        if (d.length() == 14) {
            return d.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return digits;
    }
}
