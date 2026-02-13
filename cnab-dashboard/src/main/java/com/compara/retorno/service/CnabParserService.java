package com.compara.retorno.service;

import com.compara.retorno.model.Transacao;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CnabParserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    private BigDecimal parseMonetaryValue(String valueStr) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            String cleanStr = valueStr.trim();
            // Check if it contains only digits (and optional sign)
            if (!cleanStr.matches("-?\\d+")) {
                 return BigDecimal.ZERO; 
            }
            return new BigDecimal(cleanStr).divide(new BigDecimal(100));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public List<Transacao> parseFile(MultipartFile file, String tipoOrigem) throws IOException {
        List<Transacao> transactions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            Transacao currentTransaction = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.length() < 240) continue;
                
                try {
                    char segmentType = line.charAt(13);
                    
                    if (segmentType == 'T') {
                        currentTransaction = new Transacao();
                        currentTransaction.setTipoOrigem(tipoOrigem);
                        currentTransaction.setFileSource(file.getOriginalFilename());
                        currentTransaction.setStatusConciliacao("PENDENTE"); // Default
                        
                        // Parse Segment T
                        currentTransaction.setBanco(line.substring(0, 3));
                        currentTransaction.setLote(line.substring(3, 7));
                        currentTransaction.setTipoRegistro(line.substring(7, 8));
                        currentTransaction.setNSeq(line.substring(8, 13));
                        currentTransaction.setSegmento("T");
                        currentTransaction.setMovimento(line.substring(15, 17));
                        currentTransaction.setAgencia(line.substring(17, 22));
                        currentTransaction.setConta(line.substring(23, 35));
                        currentTransaction.setNossoNumero(line.substring(37, 57).trim());
                        currentTransaction.setCarteira(line.substring(57, 58));
                        currentTransaction.setNumeroDocumento(line.substring(58, 73).trim());
                        
                        String vencimentoStr = line.substring(73, 81);
                        if (!vencimentoStr.equals("00000000")) {
                            try {
                                currentTransaction.setVencimento(LocalDate.parse(vencimentoStr, DATE_FORMATTER));
                            } catch (Exception e) { /* Ignore invalid date */ }
                        }
                        
                        currentTransaction.setValorTitulo(parseMonetaryValue(line.substring(81, 96)));
                        currentTransaction.setBancoCobrador(line.substring(96, 99));
                        currentTransaction.setAgenciaCobradora(line.substring(99, 104));
                        currentTransaction.setIdTituloEmpresa(line.substring(105, 130).trim());
                        currentTransaction.setTipoInscricao(line.substring(130, 131));
                        currentTransaction.setNumInscricao(line.substring(131, 148));
                        currentTransaction.setNomePagador(line.substring(148, 188).trim());
                        currentTransaction.setNumContrato(line.substring(188, 198));
                        currentTransaction.setValorTarifa(parseMonetaryValue(line.substring(198, 213)));
                        currentTransaction.setMotivoOcorrencia(line.substring(213, 223).trim());
                        
                    } else if (segmentType == 'U') {
                        if (currentTransaction != null) {
                            // Update with Segment U data
                            currentTransaction.setJurosMulta(parseMonetaryValue(line.substring(17, 32)));
                            currentTransaction.setDesconto(parseMonetaryValue(line.substring(32, 47)));
                            currentTransaction.setAbatimento(parseMonetaryValue(line.substring(47, 62)));
                            currentTransaction.setIof(parseMonetaryValue(line.substring(62, 77)));
                            currentTransaction.setValorPago(parseMonetaryValue(line.substring(77, 92)));
                            currentTransaction.setValorLiquido(parseMonetaryValue(line.substring(92, 107)));
                            currentTransaction.setOutrasDespesas(parseMonetaryValue(line.substring(107, 122)));
                            currentTransaction.setOutrosCreditos(parseMonetaryValue(line.substring(122, 137)));
                            
                            String dataOcorrenciaStr = line.substring(137, 145);
                            try {
                                currentTransaction.setDataOcorrencia(LocalDate.parse(dataOcorrenciaStr, DATE_FORMATTER));
                            } catch (Exception e) { /* Handle error */ }
                            
                            String dataCreditoStr = line.substring(145, 153);
                            if (!dataCreditoStr.equals("00000000")) {
                                try {
                                    currentTransaction.setDataCredito(LocalDate.parse(dataCreditoStr, DATE_FORMATTER));
                                } catch (Exception e) { /* Handle error */ }
                            }
                            
                            // Add completed transaction
                            transactions.add(currentTransaction);
                            currentTransaction = null; // Reset
                        }
                    }
                } catch (Exception e) {
                    // Log error but continue parsing other lines
                    System.err.println("Erro ao ler linha: " + e.getMessage());
                }
            }
        }
        return transactions;
    }
}
