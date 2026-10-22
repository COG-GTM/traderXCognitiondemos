package finos.traderx.accountservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import finos.traderx.accountservice.dto.AccountRequest;
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

	public Account createAccount(String displayName) {
		Account account = new Account();
		account.setDisplayName(validateDisplayName(displayName));
		return this.accountRepository.save(account);
	}

	public Account updateAccount(int id, String displayName) throws ResourceNotFoundException {
		String validDisplayName = validateDisplayName(displayName);
		Account account = getAccountById(id);
		account.setDisplayName(validDisplayName);
		return this.accountRepository.save(account);
	}

	private static String validateDisplayName(String displayName) {
		String trimmed = displayName == null ? "" : displayName.trim();
		if (trimmed.isEmpty()) {
			throw new InvalidRequestException("displayName is required");
		}
		if (trimmed.length() > AccountRequest.MAX_DISPLAY_NAME_LENGTH) {
			throw new InvalidRequestException(
					"displayName must be at most " + AccountRequest.MAX_DISPLAY_NAME_LENGTH + " characters");
		}
		return trimmed;
	}
}
