package finos.traderx.accountservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import finos.traderx.accountservice.exceptions.AccountUserConflictException;
import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountUserService {

	@Autowired
	AccountUserRepository accountUserRepository;

	@Autowired
	AccountRepository accountRepository;

	public List<AccountUser> getAllAccountUsers() {
		List<AccountUser> accountUsers = new ArrayList<AccountUser>();
		this.accountUserRepository.findAll().forEach(accountUser -> accountUsers.add(accountUser));
		return accountUsers;
	}

	public AccountUser getAccountUserById(int id) throws ResourceNotFoundException {
		Optional<AccountUser> accountUser = this.accountUserRepository.findById(Integer.valueOf(id));
		if (accountUser.isEmpty()) {
			throw new ResourceNotFoundException("AccountUser with id " + id + "not found");
		}
		return accountUser.get();
	}

	public AccountUser createAccountUser(Integer accountId, String username) {
		requireAccount(accountId);
		if (this.accountUserRepository.existsByAccountIdAndUsername(accountId, username)) {
			throw new AccountUserConflictException(username + " is already mapped to account " + accountId);
		}
		return this.accountUserRepository.save(newAccountUser(accountId, username));
	}

	public AccountUser upsertAccountUser(Integer accountId, String username) {
		requireAccount(accountId);
		return this.accountUserRepository.save(newAccountUser(accountId, username));
	}

	private void requireAccount(Integer accountId) {
		Optional<Account> account = this.accountRepository.findById(accountId);
		if (account.isEmpty()) {
			throw new ResourceNotFoundException("Account with id " + accountId + " not found");
		}
	}

	private static AccountUser newAccountUser(Integer accountId, String username) {
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(accountId);
		accountUser.setUsername(username);
		return accountUser;
	}

}
