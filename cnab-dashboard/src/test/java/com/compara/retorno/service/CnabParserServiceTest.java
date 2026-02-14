package com.compara.retorno.service;

import com.compara.retorno.model.Transacao;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CnabParserServiceTest {

    private final CnabParserService parserService = new CnabParserService();

    @Test
    public void testParseSegmentT_Positions() throws IOException {
        String lineT = "1040001300001T 460000004910850000000   14999000002922185812922185        060320260000000000702680010000002922185                  092040125594000181MARTINS ENGENHARIA LTDA                           000000000000000040301                     ";
        // To make it a valid file for parser, we need at least one Segment T. Segment U is optional for just parsing T, 
        // but the parser logic expects U to complete the transaction.
        // Wait, the parser adds transaction ONLY when Segment U is found.
        // So I need a dummy Segment U.
        String lineU = "1040001300002U 46000000000000000000000000000000000000000000000000000000000000000000000070268000000000070268000000000000000000000000000000100220261102202600000000000000000000000000000000000000000000000000000000000000000000000000000000       ";
        
        String content = lineT + "\n" + lineU;
        MockMultipartFile file = new MockMultipartFile("file", "test.ret", "text/plain", content.getBytes(StandardCharsets.UTF_8));

        List<Transacao> transactions = parserService.parseFile(file, "API");

        assertEquals(1, transactions.size());
        Transacao t = transactions.get(0);

        // Verify Fixed Fields
        assertEquals("MARTINS ENGENHARIA LTDA", t.getNomePagador());
        assertEquals("92040125594000181", t.getNumInscricao()); // Captures from 131 to 148
        assertEquals("0", t.getTipoInscricao()); // Based on index 130-131
        
        // Verify other fields to ensure shift didn't break them
        // Motivo: 213-223 -> '040301    ' -> trimmed '040301'
        assertEquals("040301", t.getMotivoOcorrencia());
        
        // Valor Tarifa: 198-213 -> '000000000000000' -> 0.00
        assertEquals(0, t.getValorTarifa().doubleValue());
    }
}
