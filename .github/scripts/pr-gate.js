const fs = require('node:fs');
const { parseTitle, upgradeNotes } = require('./pr-text');

const DEPENDABOT = 'dependabot[bot]';

function gateErrors({ title, body, headRef, author }) {
  if ((headRef || '').startsWith('release/') || author === DEPENDABOT) return [];
  const parsed = parseTitle(title);
  if (!parsed) {
    return [`PR title is not a Conventional Commit (type(scope)!: Subject): "${title}"`];
  }
  if (parsed.breaking && !upgradeNotes(body)) {
    return ['A breaking PR (title has "!") needs a non-empty "## Upgrade notes" section in its description, saying what a consumer must do on upgrade.'];
  }
  return [];
}

if (require.main === module) {
  const { pull_request: p } = JSON.parse(fs.readFileSync(process.env.GITHUB_EVENT_PATH, 'utf8'));
  const errors = gateErrors({ title: p.title, body: p.body, headRef: p.head.ref, author: p.user.login });
  errors.forEach((e) => console.log(`::error::${e}`));
  process.exit(errors.length ? 1 : 0);
}

module.exports = { gateErrors };
