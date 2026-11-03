const test = require('node:test');
const assert = require('node:assert/strict');
const { parseTitle, upgradeNotes, prNumberOf } = require('./pr-text');

test('parses a breaking refactor', () => assert.deepEqual(
  parseTitle('refactor!: Move DeprecatedRule to the root package'),
  { type: 'refactor', scope: null, breaking: true, subject: 'Move DeprecatedRule to the root package' }));
test('parses a scoped title', () => assert.equal(parseTitle('fix(sdk): Read UTF-8').scope, 'sdk'));
test('rejects a non-conventional title', () => assert.equal(parseTitle('update review-rule-proposal command'), null));
test('rejects the release PR title', () => assert.equal(parseTitle('Release Corral 0.4.0'), null));
test('extracts notes up to the next h2', () => assert.equal(
  upgradeNotes('## What and why\nx\n## Upgrade notes\n- Change the import.\n## Checks\n- [x] ok'),
  '- Change the import.'));
test('keeps h3 subheadings inside notes', () => assert.equal(
  upgradeNotes('## Upgrade notes\n### Imports\nfix\n## Checks'), '### Imports\nfix'));
test('normalises CRLF', () => assert.equal(upgradeNotes('## Upgrade notes\r\nA\r\nB\r\n'), 'A\nB'));
test('comment-only section is empty', () => assert.equal(
  upgradeNotes('## Upgrade notes\n<!-- Delete if nothing breaks. -->\n\n## Checks'), null));
test('null body', () => assert.equal(upgradeNotes(null), null));
test('pr number from squash subject', () => assert.equal(prNumberOf('docs: Serve the badge (#198)'), 198));
test('no pr number on release commit', () => assert.equal(prNumberOf('Release Corral 0.3.0'), null));
