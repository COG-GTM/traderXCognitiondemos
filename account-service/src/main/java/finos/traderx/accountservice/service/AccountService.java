package finos.traderx.accountservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import finos.traderx.accountservice.exceptions.InvalidRequestException;
import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.repository.AccountRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

	@Autowired
	AccountRepository accountRepository;

	public List<Account> getAllAccount() {
		List<Account> accounts = new ArrayList<Account>();
		this.accountRepository.findAll().forEach(account -> accounts.add(account));
		return accounts;
	}

	public Account getAccountById(int id) throws ResourceNotFoundException {
		Optional<Account> account = this.accountRepository.findById(id);
		if (account.isEmpty()) {
			throw new ResourceNotFoundException("Account with id " + id + "not found");
		}
		return account.get();
	}

	static final int MAX_DISPLAY_NAME_LENGTH = 50;

	public Account createAccount(String displayName) {
		Account account = new Account();
		account.setDisplayName(validDisplayName(displayName));
		return this.accountRepository.save(account);
	}

	public Account updateAccount(int id, String displayName) throws ResourceNotFoundException {
		String name = validDisplayName(displayName);
		Account account = getAccountById(id);
		account.setDisplayName(name);
		return this.accountRepository.save(account);
	}

	private static String validDisplayName(String displayName) {
		String name = displayName == null ? "" : displayName.trim();
		if (name.isEmpty() || name.length() > MAX_DISPLAY_NAME_LENGTH) {
			throw new InvalidRequestException(
					"displayName must be between 1 and " + MAX_DISPLAY_NAME_LENGTH + " characters");
		}
		return name;
	}
}
