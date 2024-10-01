package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeriesCalculatorTest {

    @Test
    void testSmallEpsilon() {
        SeriesCalculator calculator = new SeriesCalculator(1e-6);
        double result = calculator.calculateSum();
        assertTrue(result > 1.0, "Сумма должна быть больше 1.0 для малых значений погрешности");
    }

    @Test
    void testLargeEpsilon() {
        SeriesCalculator calculator = new SeriesCalculator(1e-2);
        double result = calculator.calculateSum();
        assertTrue(result < 1.5, "Сумма должна быть меньше 1.5 для больших значений погрешности");
    }

}
