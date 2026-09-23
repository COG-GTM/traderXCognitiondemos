package finos.traderx.positionservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import finos.traderx.positionservice.api.v2.PositionCursor;
import finos.traderx.positionservice.api.v2.PositionPageV2;
import finos.traderx.positionservice.api.v2.PositionV2;
import finos.traderx.positionservice.model.*;
import finos.traderx.positionservice.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PositionService {

	@Autowired
	PositionRepository positionRepository;

	public List<Position> getAllPositions() {
		List<Position> positions = new ArrayList<Position>();
		this.positionRepository.findAll().forEach(account -> positions.add(account));
		return positions;
	}

	public List<Position> getPositionsByAccountID(int id) {
		return this.positionRepository.findByAccountId(id);
	}

	/** v2 keyset pagination ordered by SECURITY; fetches limit+1 rows to decide whether a next page exists. */
	public PositionPageV2 getPositionsPageV2(int accountId, String cursor, int limit) {
		Pageable window = PageRequest.of(0, limit + 1);
		List<Position> rows = cursor == null || cursor.isBlank()
				? this.positionRepository.findByAccountIdOrderBySecurityAsc(accountId, window)
				: this.positionRepository.findByAccountIdAndSecurityGreaterThanOrderBySecurityAsc(
						accountId, PositionCursor.decode(cursor), window);
		boolean hasMore = rows.size() > limit;
		List<Position> page = hasMore ? rows.subList(0, limit) : rows;
		List<PositionV2> items = page.stream().map(PositionV2::from).toList();
		String next = hasMore ? PositionCursor.encode(page.get(page.size() - 1).getSecurity()) : null;
		return new PositionPageV2(items, next);
	}

}
