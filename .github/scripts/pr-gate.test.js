const test = require('node:test');
const assert = require('node:assert/strict');
const { gateErrors } = require('./pr-gate');

const pr = (o) => ({ title: 'feat: Add X', body: '', headRef: 'feature/x', author: 'MilczekT1', ...o });

test('passes a plain feat', () => assert.deepEqual(gateErrors(pr()), []));
test('fails a non-conventional title', () =>
  assert.match(gateErrors(pr({ title: 'Add X' }))[0], /Conventional Commit/));
test('fails a breaking title without notes', () =>
  assert.match(gateErrors(pr({ title: 'refactor!: Move groups', body: null }))[0], /## Upgrade notes/));
test('passes a breaking title with notes', () =>
  assert.deepEqual(gateErrors(pr({ title: 'refactor!: Move groups', body: '## Upgrade notes\nChange the import.' })), []));
test('exempts the release PR', () =>
  assert.deepEqual(gateErrors(pr({ title: 'Release Corral 0.4.0', headRef: 'release/v0.4.0' })), []));
test('exempts dependabot', () =>
  assert.deepEqual(gateErrors(pr({ title: 'Bump x', author: 'dependabot[bot]' })), []));
