const { parseTitle, upgradeNotes } = require('./pr-text');
const { bumpedKey } = require('./catalog');

const NO_NOTES = 'No upgrade notes were written for this change.';
const BREAKING_OPENING = 'Corral is pre-1.0: a minor version may break. Each change below says what to do.';
const GROUPED_PREFACE = 'Arrive in a group — they run on your next build if you wire that group. Existing violations are frozen as debt; commit the store change.';
const STANDALONE_PREFACE = 'Standalone — nothing changes until you wire them.';

const ref = (pr) => (pr == null ? '' : ` #${pr}`);

function section(heading, body) {
  return body.length ? `${heading}\n\n${body.join('\n')}` : null;
}

function renderNotes({ prs, added, moved, removed, deprecations, consumerKeys }) {
  const derivedPrs = new Set([...added, ...moved, ...removed, ...deprecations].map((e) => e.pr).filter((n) => n != null));
  const parsed = prs.map((p) => {
    const t = parseTitle(p.title);
    return { pr: p, type: t?.type ?? null, breaking: t?.breaking ?? false, subject: t ? t.subject : p.title };
  });

  const breakingPrs = parsed.filter((p) => p.breaking);
  const buckets = { feat: [], fix: [], docs: [], deps: [], internal: [] };
  for (const p of parsed.filter((q) => !q.breaking && !derivedPrs.has(q.pr.number))) {
    const line = `- ${p.subject}${ref(p.pr.number)}`;
    if (p.type === 'feat') buckets.feat.push(line);
    else if (p.type === 'fix') buckets.fix.push(line);
    else if (p.type === 'docs') buckets.docs.push(line);
    else if (p.type === 'build' && consumerKeys.has(bumpedKey(p.subject))) buckets.deps.push(line);
    else buckets.internal.push(line);
  }

  const breaking = [];
  if (breakingPrs.length || moved.length || removed.length) {
    const parts = [BREAKING_OPENING];
    for (const p of breakingPrs) {
      parts.push(`### ${p.subject} (#${p.pr.number})\n\n${upgradeNotes(p.pr.body) ?? NO_NOTES}`);
    }
    const moves = [
      ...moved.map((m) => `- \`${m.id}\`: \`${m.from}\` → \`${m.to}\`${ref(m.pr)}`),
      ...removed.map((r) => `- \`${r.id}\` was removed from the catalog${ref(r.pr)}`),
    ];
    if (moves.length) parts.push(`### Catalog moves\n\n${moves.join('\n')}`);
    breaking.push(parts.join('\n\n'));
  }

  const groupedLine = (a) => `- \`${a.id}\` — \`${a.placement}\`${a.newGroup ? ' (new)' : ''}${ref(a.pr)}`;
  const standaloneLine = (a) => `- \`${a.id}\`${ref(a.pr)}`;
  const newRules = [];
  const grouped = added.filter((a) => !a.standalone).map(groupedLine);
  const standalone = added.filter((a) => a.standalone).map(standaloneLine);
  if (grouped.length) newRules.push(`${GROUPED_PREFACE}\n\n${grouped.join('\n')}`);
  if (standalone.length) newRules.push(`${STANDALONE_PREFACE}\n\n${standalone.join('\n')}`);

  const deprecated = deprecations.map((d) => `- \`${d.oldId}\` → superseded by \`${d.newId}\`${ref(d.pr)}`);

  const authors = [...new Set(prs.filter((p) => !p.bot).map((p) => p.author))];

  const sections = [
    breaking.length ? `## ⚠️ Breaking Changes\n\n${breaking[0]}` : null,
    newRules.length ? `## ⭐ New Rules\n\n${newRules.join('\n\n')}` : null,
    section('## ✨ New Features', buckets.feat),
    section('## 🗑️ Deprecations', deprecated),
    section('## 🐞 Bug Fixes', buckets.fix),
    section('## 📔 Documentation', buckets.docs),
    section('## 🔨 Dependency Upgrades', buckets.deps),
    buckets.internal.length
      ? `<details>\n<summary>Internal (${buckets.internal.length})</summary>\n\n${buckets.internal.join('\n')}\n\n</details>`
      : null,
    authors.length
      ? `## ❤️ Contributors\n\nThank you to everyone who contributed to this release:\n\n${authors.map((a) => `- @${a}`).join('\n')}`
      : null,
  ];
  return `${sections.filter(Boolean).join('\n\n')}\n`;
}

module.exports = { renderNotes };
