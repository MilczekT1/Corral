const ROW = /^\| `([^`]+)` \| `([^`]+)` \|/gm;
const DEPRECATION = /supersededBy\(\s*"([^"]+)"\s*,(?:\s|\/\/[^\n]*\n)*"([^"]+)"/g;

function parseCatalog(rulesMd) {
  const rows = new Map();
  for (const m of rulesMd.matchAll(ROW)) rows.set(m[1], m[2]);
  return rows;
}

function isStandalone(placement) {
  return placement.startsWith('ArchTests.in(');
}

function codeUnitOrder(a, b) {
  if (a < b) return -1;
  return a > b ? 1 : 0;
}

const byId = (a, b) => codeUnitOrder(a.id, b.id);

function diffCatalog(before, after) {
  const knownPlacements = new Set(before.values());
  const added = [];
  const moved = [];
  for (const [id, placement] of after) {
    if (!before.has(id)) {
      const standalone = isStandalone(placement);
      added.push({ id, placement, standalone, newGroup: !standalone && !knownPlacements.has(placement) });
    } else if (before.get(id) !== placement) {
      moved.push({ id, from: before.get(id), to: placement });
    }
  }
  const removed = [...before.keys()].filter((id) => !after.has(id)).toSorted(codeUnitOrder);
  return { added: added.toSorted(byId), moved: moved.toSorted(byId), removed };
}

function parseDeprecations(javaSources) {
  const supersededBy = new Map();
  for (const source of javaSources) {
    for (const m of source.matchAll(DEPRECATION)) supersededBy.set(m[1], m[2]);
  }
  return supersededBy;
}

function consumerKeys(rootPom, artifacts) {
  const keys = new Set(artifacts);
  const wanted = new Set(artifacts);
  for (const block of rootPom.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)) {
    const group = /<groupId>\s*([^<\s]+)\s*<\/groupId>/.exec(block[1]);
    const artifact = /<artifactId>\s*([^<\s]+)\s*<\/artifactId>/.exec(block[1]);
    if (!group || !artifact || !wanted.has(`${group[1]}:${artifact[1]}`)) continue;
    const version = /<version>\s*\$\{([^}]+)\}\s*<\/version>/.exec(block[1]);
    if (version) keys.add(version[1]);
  }
  return keys;
}

function bumpedKey(subject) {
  const m = /^Bump (\S+) from /.exec(subject);
  return m ? m[1] : null;
}

module.exports = { parseCatalog, isStandalone, diffCatalog, parseDeprecations, consumerKeys, bumpedKey };
