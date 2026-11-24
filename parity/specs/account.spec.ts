import { test, expect } from '@playwright/test';
import { gotoApp, snap, runId, createAccountViaApi } from './helpers';

const accountRows = (page) => page.locator('#accountgrid .ag-center-cols-container .ag-row');
const userRows = (page) => page.locator('#usergrid .ag-center-cols-container .ag-row');
// AG Grid virtualises rows; accounts created by a test have the highest ids and sort last.
async function scrollAccountsToEnd(page) {
  await expect(page.locator('#accountgrid').getByText('Internal Trading Book')).toBeVisible();
  await page.locator('#accountgrid .ag-body-viewport').evaluate((el) => el.scrollTo(0, el.scrollHeight));
}

test.describe('Account page', () => {
  test('lists seeded accounts with an Update button per row', async ({ page }, testInfo) => {
    await gotoApp(page, '/account');
    await expect(page.getByRole('link', { name: 'Account' })).toHaveClass(/active/);
    await expect(page.getByRole('heading', { name: 'Account List' })).toBeVisible();
    await expect(page.locator('#accountgrid').getByText('Internal Trading Book')).toBeVisible();
    await expect(page.locator('#accountgrid').getByText('Big Corporate Fund')).toBeVisible();
    expect(await accountRows(page).count()).toBeGreaterThanOrEqual(7);
    await expect(page.locator('#accountgrid').getByRole('button', { name: 'Update' }).first()).toBeVisible();
    await expect(page.getByRole('heading', { name: /^Users List\s*$/ })).toBeVisible();
    await expect(page.locator('#usergrid').getByText('No Rows To Show')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Add Account' })).toBeVisible();
    await snap(page, testInfo, '05-account');
  });

  test('empty account name is rejected, a new account can be added', async ({ page }, testInfo) => {
    await gotoApp(page, '/account');
    await expect(page.locator('#accountgrid').getByText('Internal Trading Book')).toBeVisible();
    const before = await page.request.get('http://localhost:18088/account/').then((r) => r.json());

    await page.getByRole('button', { name: 'Add Account' }).click();
    await expect(page.locator('alert')).toHaveCount(0);
    await page.locator('#new-account').fill('   ');
    await page.getByRole('button', { name: 'Add Account' }).click();
    await expect(page.locator('alert')).toHaveCount(0);
    const afterEmpty = await page.request.get('http://localhost:18088/account/').then((r) => r.json());
    expect(afterEmpty.length).toBe(before.length);

    const name = `Parity add ${testInfo.project.name} ${runId()}`;
    await page.locator('#new-account').fill(name);
    await page.getByRole('button', { name: 'Add Account' }).click();
    await expect(page.locator('alert .alert-success')).toContainText('Account added successfully!');
    await expect(page.locator('#new-account')).toHaveValue('');
    await expect(async () => {
      await scrollAccountsToEnd(page);
      await expect(page.locator('#accountgrid').getByText(name)).toBeVisible({ timeout: 1000 });
    }).toPass();
  });

  test('Update loads an account into the form and renames it; Reset clears it', async ({ page }, testInfo) => {
    const name = `Parity edit ${runId()}`;
    await createAccountViaApi(page, name);
    await gotoApp(page, '/account');
    const row = accountRows(page).filter({ hasText: name });
    await scrollAccountsToEnd(page);
    await row.getByRole('button', { name: 'Update' }).click();
    await expect(page.locator('#new-account')).toHaveValue(name);
    await expect(page.getByRole('button', { name: 'Update Account' })).toBeVisible();

    await page.getByRole('button', { name: 'Reset' }).first().click();
    await expect(page.locator('#new-account')).toHaveValue('');
    await expect(page.getByRole('button', { name: 'Add Account' })).toBeVisible();

    await scrollAccountsToEnd(page);
    await row.getByRole('button', { name: 'Update' }).click();
    const renamed = `${name} renamed`;
    await page.locator('#new-account').fill(renamed);
    await page.getByRole('button', { name: 'Update Account' }).click();
    await expect(page.locator('alert .alert-success')).toContainText('Account updated successfully!');
    await expect(async () => {
      await scrollAccountsToEnd(page);
      await expect(page.locator('#accountgrid').getByText(renamed)).toBeVisible({ timeout: 1000 });
    }).toPass();
  });

  test('selecting an account shows its users; a user can be assigned via typeahead', async ({ page }, testInfo) => {
    const name = `Parity users ${runId()}`;
    await createAccountViaApi(page, name);
    await gotoApp(page, '/account');
    await page.locator('#accountgrid').getByText('Internal Trading Book').click();
    await expect(page.getByRole('heading', { name: 'Users List (Internal Trading Book)' })).toBeVisible();
    await expect(userRows(page).first()).toContainText('10031');
    await snap(page, testInfo, '06-account-users');

    const row = accountRows(page).filter({ hasText: name });
    await scrollAccountsToEnd(page);
    await row.locator('[col-id="displayName"]').click();
    await expect(page.getByRole('heading', { name: `Users List (${name})` })).toBeVisible();
    await expect(page.locator('#usergrid').getByText('No Rows To Show')).toBeVisible();

    // Add User with nothing selected does nothing
    await page.getByRole('button', { name: 'Add User' }).click();
    await expect(page.locator('alert')).toHaveCount(0);

    await page.locator('#account-user').pressSequentially('Alic', { delay: 50 });
    const option = page.locator('typeahead-container').getByText('Alice Johnson');
    await expect(option).toBeVisible();
    await option.click();
    await page.locator('app-assign-user').getByRole('button', { name: 'Select an account' }).click();
    await page.locator('app-assign-user').getByRole('menuitem', { name }).click();
    await page.getByRole('button', { name: 'Add User' }).click();
    await expect(page.locator('alert .alert-success')).toContainText('User added successfully!');
    await expect(page.getByRole('heading', { name: `Users List (${name})` })).toBeVisible();
    await expect(userRows(page).first()).toContainText('user01');
    await expect(page.locator('#account-user')).toHaveValue('');
  });
});
