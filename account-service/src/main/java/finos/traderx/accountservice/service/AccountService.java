package finos.traderx.accountservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.repository.AccountRepository;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

	private static final Logger log = LoggerFactory.getLogger(AccountService.class);

	@Autowired
	AccountRepository accountRepository;

	@WithSpan("account.service.getAll")
	public List<Account> getAllAccount() {
		log.info("Fetching all accounts from repository");
		List<Account> accounts = new ArrayList<Account>();
		this.accountRepository.findAll().forEach(account -> accounts.add(account));
		Span.current().setAttribute("account.count", accounts.size());
		Span.current().setStatus(StatusCode.OK);
		log.info("Found {} accounts", accounts.size());
		return accounts;
	}

	@WithSpan("account.service.getById")
	public Account getAccountById(@SpanAttribute("account.id") int id) throws ResourceNotFoundException {
		log.info("Fetching account by id={}", id);
		Span span = Span.current();
		Optional<Account> account = this.accountRepository.findById(id);
		if (account.isEmpty()) {
			span.setStatus(StatusCode.ERROR, "Account not found");
			span.addEvent("Account lookup failed");
			throw new ResourceNotFoundException("Account with id " + id + "not found");
		}
		span.setAttribute("account.displayName", account.get().getDisplayName());
		span.setStatus(StatusCode.OK);
		log.info("Account found: id={}, displayName={}", id, account.get().getDisplayName());
		return account.get();
	}

	@WithSpan("account.service.upsert")
	public Account upsertAccount(Account account) {
		Span span = Span.current();
		span.setAttribute("account.id", account.getId());
		span.setAttribute("account.displayName", account.getDisplayName());
		log.info("Upserting account: id={}, displayName={}", account.getId(), account.getDisplayName());
		Account saved = this.accountRepository.save(account);
		span.setStatus(StatusCode.OK);
		span.addEvent("Account persisted");
		log.info("Account persisted: id={}", saved.getId());
		return saved;
	}
}
