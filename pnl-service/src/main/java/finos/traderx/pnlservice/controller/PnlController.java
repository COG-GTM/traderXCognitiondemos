package finos.traderx.pnlservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.pnlservice.model.PnlSummary;
import finos.traderx.pnlservice.model.RealizedPnlDetail;
import finos.traderx.pnlservice.model.UnrealizedPnlDetail;
import finos.traderx.pnlservice.service.PnlService;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/pnl", produces="application/json")
public class PnlController {

    @Autowired
    private PnlService pnlService;

    @GetMapping("/{accountId}")
    public ResponseEntity<PnlSummary> getPnlSummary(@PathVariable Integer accountId) {
        PnlSummary summary = pnlService.getPnlSummary(accountId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{accountId}/realized")
    public ResponseEntity<List<RealizedPnlDetail>> getRealizedPnl(@PathVariable Integer accountId) {
        List<RealizedPnlDetail> details = pnlService.getRealizedPnlDetails(accountId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/{accountId}/unrealized")
    public ResponseEntity<List<UnrealizedPnlDetail>> getUnrealizedPnl(@PathVariable Integer accountId) {
        List<UnrealizedPnlDetail> details = pnlService.getUnrealizedPnlDetails(accountId);
        return ResponseEntity.ok(details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> generalError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
