const { parseTitle, upgradeNotes } = require('./pr-text');
const { bumpedKey, isStandalone } = require('./catalog');

const NO_NOTES = 'No upgrade notes were written for this change.';
const BREAKING_OPENING = 'Corral is pre-1.0: a minor version may break. Each change below says what to do.';
const GROUPED_PREFACE = 'Arrive in a group — they run on your next build if you wire that group. Existing violations are frozen as debt; commit the store change.';
const STANDALONE_PREFACE = 'Standalone — nothing changes until you wire them.';

const ref = (pr) => (pr == null ? '' : ` #${pr}`);
const placementName = (placement) => (isStandalone(placement) ? 'standalone' : `\`${placement}\``);

function section(heading, body) {
  return body.length ? `${heading}\n\n${body.join('\n')}` : null;
}

function parsePr(pr) {
  const t = parseTitle(pr.title);
  return { pr, type: t?.type ?? null, breaking: t?.breaking ?? false, subject: t ? t.subject : pr.title };
}

function bucketOf({ type, subject }, consumerKeys) {
  if (type === 'feat' || type === 'fix' || type === 'docs') return type;
  if (type === 'build' && consumerKeys.has(bumpedKey(subject))) return 'deps';
  return 'internal';
}

function breakingBody(breakingPrs, moved, removed) {
  if (!breakingPrs.length && !moved.length && !removed.length) return null;
  const parts = [BREAKING_OPENING];
  for (const p of breakingPrs) {
    parts.push(`### ${p.subject} (#${p.pr.number})\n\n${upgradeNotes(p.pr.body) ?? NO_NOTES}`);
  }
  const moves = [
    ...moved.map((m) => `- \`${m.id}\`: ${placementName(m.from)} → ${placementName(m.to)}${ref(m.pr)}`),
    ...removed.map((r) => `- \`${r.id}\` was removed from the catalog${ref(r.pr)}`),
  ];
  if (moves.length) parts.push(`### Catalog moves\n\n${moves.join('\n')}`);
  return parts.join('\n\n');
}

function newRulesBody(added) {
  const groupedLine = (a) => `- \`${a.id}\` — \`${a.placement}\`${a.newGroup ? ' (new)' : ''}${ref(a.pr)}`;
  const standaloneLine = (a) => `- \`${a.id}\`${ref(a.pr)}`;
  const parts = [];
  const grouped = added.filter((a) => !a.standalone).map(groupedLine);
  const standalone = added.filter((a) => a.standalone).map(standaloneLine);
  if (grouped.length) parts.push(`${GROUPED_PREFACE}\n\n${grouped.join('\n')}`);
  if (standalone.length) parts.push(`${STANDALONE_PREFACE}\n\n${standalone.join('\n')}`);
  return parts.length ? parts.join('\n\n') : null;
}

function internalSection(lines) {
  if (!lines.length) return null;
  return `<details>\n<summary>Internal (${lines.length})</summary>\n\n${lines.join('\n')}\n\n</details>`;
}

function contributorsSection(prs) {
  const authors = [...new Set(prs.filter((p) => !p.bot).map((p) => `- @${p.author}`))];
  if (!authors.length) return null;
  return `## ❤️ Contributors\n\nThank you to everyone who contributed to this release:\n\n${authors.join('\n')}`;
}

function renderNotes({ prs, added, moved, removed, deprecations, consumerKeys }) {
  const derivedPrs = new Set([...added, ...moved, ...removed, ...deprecations].map((e) => e.pr).filter((n) => n != null));
  const parsed = prs.map(parsePr);

  const buckets = { feat: [], fix: [], docs: [], deps: [], internal: [] };
  for (const p of parsed.filter((q) => !q.breaking && !derivedPrs.has(q.pr.number))) {
    buckets[bucketOf(p, consumerKeys)].push(`- ${p.subject}${ref(p.pr.number)}`);
  }

  const breaking = breakingBody(parsed.filter((p) => p.breaking), moved, removed);
  const newRules = newRulesBody(added);
  const deprecated = deprecations.map((d) => `- \`${d.oldId}\` → superseded by \`${d.newId}\`${ref(d.pr)}`);

  const sections = [
    breaking && `## ⚠️ Breaking Changes\n\n${breaking}`,
    newRules && `## ⭐ New Rules\n\n${newRules}`,
    section('## ✨ New Features', buckets.feat),
    section('## 🗑️ Deprecations', deprecated),
    section('## 🐞 Bug Fixes', buckets.fix),
    section('## 📔 Documentation', buckets.docs),
    section('## 🔨 Dependency Upgrades', buckets.deps),
    internalSection(buckets.internal),
    contributorsSection(prs),
  ];
  return `${sections.filter(Boolean).join('\n\n')}\n`;
}

module.exports = { renderNotes };
