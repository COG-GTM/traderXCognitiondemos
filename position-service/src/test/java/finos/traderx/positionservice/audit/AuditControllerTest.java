package finos.traderx.positionservice.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import finos.traderx.positionservice.controller.AuditController;
import finos.traderx.positionservice.model.audit.AuditPage;
import finos.traderx.positionservice.model.audit.DecisionOutcome;
import finos.traderx.positionservice.service.AuditQueryService;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditQueryService auditQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditController(auditQueryService)).build();
    }

    @Test
    void passesFiltersThroughAndReturnsAPagedEnvelope() throws Exception {
        when(auditQueryService.isEnabled()).thenReturn(true);
        when(auditQueryService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new AuditPage(List.of(), 2, 25, 120L, 5));

        mockMvc.perform(get("/audit/decisions")
                .param("accountId", "11413")
                .param("security", "AAPL")
                .param("decision", "rejected")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-02-01T00:00:00Z")
                .param("page", "2")
                .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(120))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.page").value(2));

        verify(auditQueryService).search(eq(11413), eq("AAPL"), eq(DecisionOutcome.REJECTED),
                eq(Instant.parse("2026-01-01T00:00:00Z")), eq(Instant.parse("2026-02-01T00:00:00Z")), eq(2), eq(25));
    }

    @Test
    void unfilteredQueryReachesTheServiceWithNoBounds() throws Exception {
        when(auditQueryService.isEnabled()).thenReturn(true);
        when(auditQueryService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new AuditPage(List.of(), 0, 50, 0L, 0));

        mockMvc.perform(get("/audit/decisions")).andExpect(status().isOk());

        verify(auditQueryService).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void rejectsAnUnknownDecisionRatherThanWideningTheQuery() throws Exception {
        when(auditQueryService.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/audit/decisions").param("decision", "MAYBE")).andExpect(status().isBadRequest());

        verify(auditQueryService, never()).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void reportsAnUnparseableFilterAsABadRequestRatherThanAnOutage() throws Exception {
        mockMvc.perform(get("/audit/decisions").param("from", "last Tuesday")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/audit/decisions").param("accountId", "all")).andExpect(status().isBadRequest());

        verify(auditQueryService, never()).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void returnsServiceUnavailableAndQueriesNothingWhenTheFeatureIsOff() throws Exception {
        when(auditQueryService.isEnabled()).thenReturn(false);

        mockMvc.perform(get("/audit/decisions")).andExpect(status().isServiceUnavailable());

        verify(auditQueryService, never()).search(any(), any(), any(), any(), any(), any(), any());
    }
}
