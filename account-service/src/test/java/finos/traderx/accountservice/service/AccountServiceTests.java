package finos.traderx.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import finos.traderx.accountservice.model.Account;

@SpringBootTest
@TestPropertySource(locations = "/test-application.properties")
class AccountServiceTests {

    @Autowired
    AccountService accountService;

    @Test
    void contextLoads() {
    }

    @Test
    void createsAndReadsBackAnAccount() {
        Account input = new Account();
        input.setDisplayName("test account");

        Account created = accountService.upsertAccount(input);
        assertTrue(created.getId() > 0, "sequence generator should assign an id");

        Account found = accountService.getAccountById(created.getId());
        assertEquals("test account", found.getDisplayName());
    }
}
