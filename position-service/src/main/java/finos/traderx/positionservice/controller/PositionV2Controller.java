package finos.traderx.positionservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.positionservice.api.v2.PositionCursor;
import finos.traderx.positionservice.api.v2.PositionPageV2;
import finos.traderx.positionservice.service.PositionService;

/**
 * Approved replacement contract for the positions consumer:
 * GET /v2/positions?accountId=&cursor=&limit= -> { items: [...], nextCursor: string|null }
 * See techfest-migration/CONTRACT_BRIEF.md.
 */
@CrossOrigin("*")
@RestController
@RequestMapping(value = "/v2/positions", produces = "application/json")
public class PositionV2Controller {

	public static final int DEFAULT_LIMIT = 10;
	public static final int MAX_LIMIT = 100;

	@Autowired
	PositionService positionService;

	@GetMapping
	public ResponseEntity<PositionPageV2> getPage(
			@RequestParam int accountId,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer limit) {
		int pageSize = limit == null ? DEFAULT_LIMIT : limit;
		if (pageSize < 1 || pageSize > MAX_LIMIT) {
			throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
		}
		return ResponseEntity.ok(this.positionService.getPositionsPageV2(accountId, cursor, pageSize));
	}

	@ExceptionHandler({ IllegalArgumentException.class, PositionCursor.InvalidCursorException.class })
	public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
	}
}
