const test = require('node:test');
const assert = require('node:assert/strict');
const { renderNotes } = require('./release-notes');

const PLACEHOLDER = 'No upgrade notes were written for this change.';

function input(over = {}) {
  return { prs: [], added: [], moved: [], removed: [], deprecations: [], consumerKeys: new Set(), ...over };
}
function pr(number, title, over = {}) {
  return { number, title, body: null, author: 'alice', bot: false, ...over };
}

test('breaking PR with notes renders heading and notes verbatim', () => {
  const out = renderNotes(input({
    prs: [pr(7, 'refactor!: Move DeprecatedRule', { body: '## Upgrade notes\n- Change the import.\n## Checks' })],
  }));
  assert.match(out, /## ⚠️ Breaking Changes\n\nCorral is pre-1\.0: a minor version may break\. Each change below says what to do\.\n\n### Move DeprecatedRule \(#7\)\n\n- Change the import\.\n/);
});

test('breaking PR with null body renders the placeholder', () => {
  const out = renderNotes(input({ prs: [pr(7, 'feat!: Drop it')] }));
  assert.ok(out.includes(`### Drop it (#7)\n\n${PLACEHOLDER}`));
});

test('breaking PR that added a rule appears in both Breaking Changes and New Rules', () => {
  const out = renderNotes(input({
    prs: [pr(7, 'feat!: Add rule')],
    added: [{ id: 'corral.a.no-x', placement: 'FooRulesGroup', standalone: false, newGroup: false, pr: 7 }],
  }));
  assert.ok(out.includes('### Add rule (#7)'));
  assert.ok(out.includes('## ⭐ New Rules'));
  assert.ok(out.includes('- `corral.a.no-x` — `FooRulesGroup` #7'));
});

test('feat PR that added a rule is not listed under New Features', () => {
  const out = renderNotes(input({
    prs: [pr(8, 'feat: Add rule')],
    added: [{ id: 'corral.a.no-x', placement: 'FooRulesGroup', standalone: false, newGroup: false, pr: 8 }],
  }));
  assert.ok(!out.includes('New Features'));
  assert.ok(!out.includes('- Add rule #8'));
});

test('feat PR with no derived line is listed under New Features', () => {
  const out = renderNotes(input({ prs: [pr(9, 'feat: Print the id')] }));
  assert.ok(out.includes('## ✨ New Features\n\n- Print the id #9\n'));
});

test('consumer bump goes to Dependency Upgrades, plugin bump to Internal', () => {
  const out = renderNotes(input({
    prs: [pr(1, 'build: Bump archunit.version from 1.5.1 to 1.6.0'), pr(2, 'build: Bump maven-foo-plugin from 1 to 2')],
    consumerKeys: new Set(['archunit.version']),
  }));
  const deps = out.indexOf('## 🔨 Dependency Upgrades');
  const internal = out.indexOf('<summary>Internal (1)</summary>');
  assert.ok(deps >= 0 && internal > deps);
  assert.ok(out.indexOf('Bump archunit.version from 1.5.1 to 1.6.0 #1') > deps);
  assert.ok(out.indexOf('Bump maven-foo-plugin from 1 to 2 #2') > internal);
});

test('legacy non-conventional title goes to Internal with its raw title', () => {
  const out = renderNotes(input({ prs: [pr(3, 'update review-rule-proposal command')] }));
  assert.ok(out.includes('<details>\n<summary>Internal (1)</summary>\n\n- update review-rule-proposal command #3\n\n</details>'));
});

test('bots are not contributors', () => {
  const out = renderNotes(input({
    prs: [pr(1, 'fix: A', { author: 'alice' }), pr(2, 'fix: B', { author: 'dependabot', bot: true }), pr(3, 'fix: C', { author: 'alice' }), pr(4, 'fix: D', { author: 'bob' })],
  }));
  assert.ok(out.includes('## ❤️ Contributors\n\nThank you to everyone who contributed to this release:\n\n- @alice\n- @bob\n'));
  assert.ok(!out.includes('@dependabot'));
});

test('empty sections are omitted and order matches the Global Constraints list', () => {
  const out = renderNotes(input({
    prs: [
      pr(1, 'chore: Tidy'), pr(2, 'build: Bump x.version from 1 to 2'), pr(3, 'docs: Words'),
      pr(4, 'fix: Bug'), pr(5, 'feat: Thing'), pr(6, 'fix!: Break'),
    ],
    added: [{ id: 'corral.a.no-x', placement: 'G', standalone: true, newGroup: false, pr: null }],
    deprecations: [{ oldId: 'corral.a.old', newId: 'corral.a.new', pr: null }],
    consumerKeys: new Set(['x.version']),
  }));
  const headings = ['## ⚠️ Breaking Changes', '## ⭐ New Rules', '## ✨ New Features', '## 🗑️ Deprecations', '## 🐞 Bug Fixes',
    '## 📔 Documentation', '## 🔨 Dependency Upgrades', '<details>', '## ❤️ Contributors'];
  const idx = headings.map((h) => out.indexOf(h));
  assert.ok(idx.every((i) => i >= 0));
  assert.deepEqual([...idx].sort((a, b) => a - b), idx);
  assert.ok(!renderNotes(input({ prs: [pr(1, 'fix: Bug')] })).includes('Breaking Changes'));
  assert.ok(out.endsWith('\n') && !out.endsWith('\n\n'));
});

test('New Rules splits grouped and standalone under their prefaces, marks (new)', () => {
  const added = [
    { id: 'corral.a.no-x', placement: 'FooRulesGroup', standalone: false, newGroup: true, pr: 5 },
    { id: 'corral.a.no-y', placement: 'BarRulesGroup', standalone: false, newGroup: false, pr: null },
    { id: 'corral.a.no-z', placement: 'ZRule', standalone: true, newGroup: false, pr: 6 },
  ];
  const out = renderNotes(input({ added }));
  const g = 'Arrive in a group — they run on your next build if you wire that group. Existing violations are frozen as debt; commit the store change.';
  const s = 'Standalone — nothing changes until you wire them.';
  assert.ok(out.includes(`${g}\n\n- \`corral.a.no-x\` — \`FooRulesGroup\` (new) #5\n- \`corral.a.no-y\` — \`BarRulesGroup\`\n\n${s}\n\n- \`corral.a.no-z\` — \`ZRule\` #6\n`));
  const only = renderNotes(input({ added: [added[2]] }));
  assert.ok(!only.includes('Arrive in a group'));
});
