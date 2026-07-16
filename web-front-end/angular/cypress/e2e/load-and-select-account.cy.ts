/// <reference types="cypress" />

// True end-to-end test for the "load list of accounts and select an account"
// trader journey. It drives the Angular UI served through the ingress at the
// configured baseUrl and hits the live account-service; nothing is stubbed.
//
// Seed data (database/initialSchema.sql) guarantees these accounts exist.
const SEEDED_ACCOUNTS = ['Big Corporate Fund', 'Hedge Fund TXY1', 'Internal Trading Book'];
const ACCOUNT_TO_SELECT = 'Big Corporate Fund';

describe('Trader journey: load list of accounts and select an account', () => {
    it('loads accounts from the live account-service and updates the UI on selection', () => {
        // Pass-through intercept so we can assert the real network call fired and
        // succeeded, without stubbing the response (keeps this a true e2e test).
        cy.intercept('GET', '**/account/').as('getAccounts');

        cy.visit('/');

        // The real GET /account/ must fire and return 200.
        cy.wait('@getAccounts').its('response.statusCode').should('eq', 200);

        // Open the account dropdown and assert the seeded accounts rendered,
        // confirming the accounts loaded from the backend.
        cy.get('[data-test=dropdown-toggle]').click();
        cy.get('[data-test=dropdown-item]').should('have.length.at.least', SEEDED_ACCOUNTS.length);
        SEEDED_ACCOUNTS.forEach((name) => {
            cy.get('[data-test=dropdown-item]').contains(name).should('exist');
        });

        // Select a specific, known account.
        cy.get(`[data-test=dropdown-item][data-test-value="${ACCOUNT_TO_SELECT}"]`).click();

        // The selection propagated to the trade component state.
        cy.get('[data-test=selected-account]').should('have.attr', 'data-test-value', ACCOUNT_TO_SELECT);
        cy.get('[data-test=dropdown-toggle]').should('contain', ACCOUNT_TO_SELECT);

        // The blotters received the selected account and are rendered.
        cy.get('[data-test=trade-blotter]').should('exist').and('be.visible');
        cy.get('[data-test=position-blotter]').should('exist').and('be.visible');
    });
});
