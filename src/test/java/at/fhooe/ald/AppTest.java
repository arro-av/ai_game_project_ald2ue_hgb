package at.fhooe.ald;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void projectBaselineIsConfigured() {
        assertEquals("at.fhooe.ald", App.class.getPackageName());
    }
}
