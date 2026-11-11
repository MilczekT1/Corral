const test = require('node:test');
const assert = require('node:assert/strict');
const { titleProblems, titleOf } = require('./check-pr-title');

test('accepts a specific imperative title', () => assert.deepEqual(
  titleProblems('fix!: Print a source location on every violation line'), []));
test('rejects a non-conventional title', () => assert.equal(titleProblems('Feat/central arch rules').length, 1));
test('rejects a vague subject', () => assert.match(titleProblems('fix: Fix bug').join(), /Too vague/));
test('rejects a past-tense verb', () => assert.match(titleProblems('feat: Added the Spring group').join(), /imperative/));
test('accepts an imperative verb ending in -ed', () => assert.deepEqual(titleProblems('test: Seed the JUnit 4 freeze store'), []));
test('rejects a process subject', () => assert.match(titleProblems('chore: Address review comments on the README').join(), /outcome/));
test('rejects a subject over 72 characters', () => assert.match(titleProblems(`feat: Add ${'x'.repeat(70)}`).join(), /characters/));
test('rejects the squash suffix', () => assert.match(titleProblems('docs: Document freezing (#12)').join(), /\(#N\)/));

test('reads a double-quoted title', () => assert.equal(
  titleOf('gh pr create --title "feat: Add X" --body "y"'), 'feat: Add X'));
test('reads a single-quoted -t title', () => assert.equal(titleOf("gh pr create -t 'fix: Y'"), 'fix: Y'));
test('reads a title after a cd', () => assert.equal(titleOf('cd repo && gh pr create --title "a: b"'), 'a: b'));
test('reads a title after an env assignment', () => assert.equal(titleOf('GH_HOST=x gh pr create --title "a: b"'), 'a: b'));
test('cannot read a title built from a variable', () => assert.equal(titleOf('gh pr create --title "$T"'), null));
test('cannot read a --fill title', () => assert.equal(titleOf('gh pr create --fill'), null));
test('ignores an edit that keeps the title', () => assert.equal(titleOf('gh pr edit 5 --body-file b.md'), undefined));
test('ignores gh pr create inside a grep', () => assert.equal(titleOf('grep -rn "gh pr create --title" .'), undefined));
test('ignores gh pr create inside a commit message', () => assert.equal(
  titleOf('git commit -m "docs: Explain gh pr create --title usage"'), undefined));
