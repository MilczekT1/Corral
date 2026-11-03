const test = require('node:test');
const assert = require('node:assert/strict');
const { parseCatalog, isStandalone, diffCatalog, parseDeprecations, consumerKeys, bumpedKey } = require('./catalog');

const before = parseCatalog('| `corral.a` | `TestingRulesGroup` | x |\n| `corral.m` | `TestingRulesGroup` | x |');
const after = parseCatalog('| `corral.a` | `TestingRulesGroup` | x |\n| `corral.m` | `ArchTests.in(M.class)` | x |\n| `corral.n` | `SecurityRulesGroup` | x |\n| `corral.s` | `ArchTests.in(S.class)` | x |');

test('diff', () => assert.deepEqual(diffCatalog(before, after), {
  added: [{ id: 'corral.n', placement: 'SecurityRulesGroup', standalone: false, newGroup: true },
          { id: 'corral.s', placement: 'ArchTests.in(S.class)', standalone: true, newGroup: false }],
  moved: [{ id: 'corral.m', from: 'TestingRulesGroup', to: 'ArchTests.in(M.class)' }],
  removed: [] }));
test('removed ids are listed sorted', () => assert.deepEqual(
  diffCatalog(parseCatalog('| `b` | `G` | x |\n| `a` | `G` | x |'), new Map()).removed, ['a', 'b']));
test('a group already present is not new', () => assert.equal(
  diffCatalog(before, parseCatalog('| `corral.a` | `TestingRulesGroup` | x |\n| `corral.z` | `TestingRulesGroup` | x |')).added[0].newGroup, false));
test('standalone placement', () => {
  assert.equal(isStandalone('ArchTests.in(X.class)'), true);
  assert.equal(isStandalone('SpringRulesGroup'), false);
});
test('ignores header and prose rows', () => assert.equal(parseCatalog('| Rule id | Group | What |\n|---|---|---|').size, 0));
test('deprecation across lines with comments', () => assert.deepEqual(
  [...parseDeprecations(['DeprecatedRule.supersededBy(\n  "acme.old",          // retired\n  "acme.new",  // replacement\n  "why")'])],
  [['acme.old', 'acme.new']]));
test('consumer keys include the version property', () => assert.deepEqual(
  [...consumerKeys('<dependency><groupId>com.tngtech.archunit</groupId><artifactId>archunit-junit5</artifactId><version>${archunit.version}</version></dependency>',
                   ['com.tngtech.archunit:archunit-junit5'])].sort(),
  ['archunit.version', 'com.tngtech.archunit:archunit-junit5']));
test('consumer keys ignore other dependencies and literal versions', () => assert.deepEqual(
  [...consumerKeys('<dependency><groupId>g</groupId><artifactId>a</artifactId><version>${a.v}</version></dependency><dependency><groupId>g</groupId><artifactId>b</artifactId><version>1.0</version></dependency>', ['g:b'])],
  ['g:b']));
test('bumped key', () => assert.equal(bumpedKey('Bump spring-boot.version from 4.0.8 to 4.1.2'), 'spring-boot.version'));
test('grouped dependabot bump has no key', () => assert.equal(bumpedKey('Bump the maven group with 3 updates'), null));
