package finos.traderx.tradeservice.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DocsControllerTest {

    private final DocsController docsController = new DocsController();

    @Test
    void index_ReturnsRedirectToSwaggerUi() {
        String result = docsController.index();
        
        assertEquals("redirect:swagger-ui.html", result);
    }

    @Test
    void index_ReturnsNonNullValue() {
        String result = docsController.index();
        
        assertNotNull(result);
    }

    @Test
    void index_ReturnsStringContainingSwagger() {
        String result = docsController.index();
        
        assertTrue(result.contains("swagger"));
    }
}
