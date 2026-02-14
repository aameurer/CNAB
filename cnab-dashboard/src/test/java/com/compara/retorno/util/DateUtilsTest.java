package com.compara.retorno.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void testFormat() {
        LocalDate date = LocalDate.of(2023, 12, 25);
        assertEquals("25/12/2023", DateUtils.format(date));
        assertEquals("", DateUtils.format(null));
    }

    @Test
    void testValidateAndFixRange_Normal() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 10);
        
        LocalDate[] result = DateUtils.validateAndFixRange(start, end);
        
        assertEquals(start, result[0]);
        assertEquals(end, result[1]);
    }

    @Test
    void testValidateAndFixRange_Swapped() {
        LocalDate start = LocalDate.of(2023, 1, 10);
        LocalDate end = LocalDate.of(2023, 1, 1);
        
        LocalDate[] result = DateUtils.validateAndFixRange(start, end);
        
        assertEquals(end, result[0]); // Should be 1st
        assertEquals(start, result[1]); // Should be 10th
    }

    @Test
    void testValidateAndFixRange_Nulls() {
        LocalDate nowMinus1 = LocalDate.now().minusDays(1);
        
        LocalDate[] result = DateUtils.validateAndFixRange(null, null);
        
        assertEquals(nowMinus1, result[0]);
        assertEquals(nowMinus1, result[1]);
    }
}
