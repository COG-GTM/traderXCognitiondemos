package finos.traderx.accountservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountUserService {

	private static final Logger log = LoggerFactory.getLogger(AccountUserService.class);

	@Autowired
	AccountUserRepository accountUserRepository;

	@Autowired
	AccountRepository accountRepository;

	@WithSpan("accountUser.service.getAll")
	public List<AccountUser> getAllAccountUsers() {
		log.info("Fetching all account users");
		List<AccountUser> accountUsers = new ArrayList<AccountUser>();
		this.accountUserRepository.findAll().forEach(accountUser -> accountUsers.add(accountUser));
		Span.current().setAttribute("accountUser.count", accountUsers.size());
		Span.current().setStatus(StatusCode.OK);
		log.info("Found {} account users", accountUsers.size());
		return accountUsers;
	}

	@WithSpan("accountUser.service.getById")
	public AccountUser getAccountUserById(@SpanAttribute("accountUser.id") int id) throws ResourceNotFoundException {
		log.info("Fetching account user by id={}", id);
		Span span = Span.current();
		Optional<AccountUser> accountUser = this.accountUserRepository.findById(Integer.valueOf(id));
		if (accountUser.isEmpty()) {
			span.setStatus(StatusCode.ERROR, "Account user not found");
			span.addEvent("Account user lookup failed");
			throw new ResourceNotFoundException("AccountUser with id " + id + "not found");
		}
		span.setAttribute("accountUser.username", accountUser.get().getUsername());
		span.setStatus(StatusCode.OK);
		log.info("Account user found: id={}, username={}", id, accountUser.get().getUsername());
		return accountUser.get();
	}

	@WithSpan("accountUser.service.upsert")
	public AccountUser upsertAccountUser(AccountUser accountUser) {
		Span span = Span.current();
		span.setAttribute("accountUser.username", accountUser.getUsername());
		span.setAttribute("accountUser.accountId", accountUser.getAccountId());
		log.info("Upserting account user: username={}, accountId={}", accountUser.getUsername(), accountUser.getAccountId());

		Optional<Account> account = this.accountRepository.findById(accountUser.getAccountId());
		if (account.isEmpty()) {
			span.setStatus(StatusCode.ERROR, "Account not found");
			span.addEvent("Account validation failed for user assignment");
			throw new ResourceNotFoundException("Account with id " + accountUser.getAccountId() + "not found");
		}
		span.addEvent("Account validated for user assignment");
		AccountUser saved = this.accountUserRepository.save(accountUser);
		span.setStatus(StatusCode.OK);
		span.addEvent("Account user persisted");
		log.info("Account user persisted: username={}, accountId={}", saved.getUsername(), saved.getAccountId());
		return saved;
	}

}
