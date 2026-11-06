package finos.traderx.accountservice.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.model.Person;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AccountUserService {

	private static final Logger logger = LoggerFactory.getLogger(AccountUserService.class);

	private RestTemplate restTemplate = new RestTemplate();

	@Value("${people.service.url}")
	private String peopleServiceAddress;

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

	public AccountUser upsertAccountUser(AccountUser accountUser) {
		Optional<Account> account = this.accountRepository.findById(accountUser.getAccountId());
		if (account.isEmpty()) {
			throw new ResourceNotFoundException("Account with id " + accountUser.getAccountId() + "not found");
		}
		if (!validatePerson(accountUser.getUsername())) {
			throw new ResourceNotFoundException(accountUser.getUsername() + " not found in People service.");
		}
		return this.accountUserRepository.save(accountUser);
	}

	private boolean validatePerson(String username) {
		if (username == null || username.isBlank()) {
			return false;
		}
		URI uri = UriComponentsBuilder.fromUriString(this.peopleServiceAddress)
				.path("/People/GetPerson")
				.queryParam("LogonId", "{logonId}")
				.encode()
				.buildAndExpand(username)
				.toUri();

		try {
			Person person = this.restTemplate.getForObject(uri, Person.class);
			if (person == null || !username.equals(person.getLogonId())) {
				logger.info(username + " does not match the person returned by People service.");
				return false;
			}
			logger.info("Validated person " + person);
			return true;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				logger.info(username + " not found in People service.");
			}
			else {
				logger.error(ex.getMessage());
			}
			return false;
		}
	}

}
