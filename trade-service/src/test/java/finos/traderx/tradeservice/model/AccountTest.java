package finos.traderx.tradeservice.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void constructor_WithParameters_SetsAllFields() {
        Account account = new Account(100001, "Test Account");

        assertEquals(100001, account.getid());
        assertEquals("Test Account", account.getdisplayName());
    }

    @Test
    void defaultConstructor_CreatesEmptyObject() {
        Account account = new Account();

        assertNull(account.getid());
        assertNull(account.getdisplayName());
    }

    @Test
    void getid_ReturnsCorrectId() {
        Account account = new Account(200002, "Another Account");

        assertEquals(200002, account.getid());
    }

    @Test
    void getdisplayName_ReturnsCorrectDisplayName() {
        Account account = new Account(100001, "My Trading Account");

        assertEquals("My Trading Account", account.getdisplayName());
    }

    @Test
    void constructor_WithDifferentValues_WorksCorrectly() {
        Account account1 = new Account(1, "Account One");
        Account account2 = new Account(999999, "Account Two");

        assertEquals(1, account1.getid());
        assertEquals("Account One", account1.getdisplayName());
        assertEquals(999999, account2.getid());
        assertEquals("Account Two", account2.getdisplayName());
    }

    @Test
    void constructor_WithEmptyDisplayName_WorksCorrectly() {
        Account account = new Account(100001, "");

        assertEquals(100001, account.getid());
        assertEquals("", account.getdisplayName());
    }

    @Test
    void constructor_WithNullDisplayName_WorksCorrectly() {
        Account account = new Account(100001, null);

        assertEquals(100001, account.getid());
        assertNull(account.getdisplayName());
    }
}
