const { execFileSync } = require('node:child_process');
const { prNumberOf } = require('./pr-text');
const { parseCatalog, diffCatalog, parseDeprecations, consumerKeys } = require('./catalog');
const { renderNotes } = require('./release-notes');

const USAGE =
  'usage: release-notes-cli.js --from <ref> --to <ref> --repo <owner/name> [--consumer-artifact g:a]...';

function parseArgs(argv) {
  const args = { consumerArtifacts: [] };
  for (let i = 0; i < argv.length; i++) {
    const value = argv[i + 1];
    switch (argv[i]) {
      case '--from': args.from = value; i++; break;
      case '--to': args.to = value; i++; break;
      case '--repo': args.repo = value; i++; break;
      case '--consumer-artifact': args.consumerArtifacts.push(value); i++; break;
      default: return null;
    }
  }
  return args.from && args.to && args.repo ? args : null;
}

function run(cmd, args) {
  return execFileSync(cmd, args, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
}

const git = (...args) => run('git', args);

const lines = (text) => text.split('\n').filter((line) => line !== '');

// git grep -l exits 1 when nothing matches.
function grepFiles(ref, pattern, path) {
  try {
    return lines(git('grep', '-l', pattern, ref, '--', path)).map((l) => l.slice(ref.length + 1));
  } catch (e) {
    if (e.status === 1) return [];
    throw e;
  }
}

const escapeRegex = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

function prOf(subject) {
  return subject === undefined ? null : prNumberOf(subject);
}

function main(args) {
  const { from, to, repo } = args;
  const range = `${from}..${to}`;

  const subjects = lines(git('log', '--reverse', '--format=%s', range));
  const numbers = [...new Set(subjects.map(prNumberOf).filter((n) => n !== null))];
  const prs = numbers.map((n) => {
    const pr = JSON.parse(run('gh', ['api', `repos/${repo}/pulls/${n}`]));
    return { number: pr.number, title: pr.title, body: pr.body || '', author: pr.user.login, bot: pr.user.type === 'Bot' };
  });

  const catalogAt = (ref) => parseCatalog(git('show', `${ref}:docs/rules.md`));
  const diff = diffCatalog(catalogAt(from), catalogAt(to));
  const firstAdding = (id) =>
    prOf(lines(git('log', '--reverse', '--format=%s', `-S\`${id}\``, range, '--', 'docs/rules.md'))[0]);
  const lastTouching = (id) =>
    prOf(lines(git('log', '--format=%s', `-G^\\| \`${escapeRegex(id)}\` \\|`, range, '--', 'docs/rules.md'))[0]);
  const lastRemoving = (id) =>
    prOf(lines(git('log', '--format=%s', `-S\`${id}\``, range, '--', 'docs/rules.md'))[0]);

  const deprecationsAt = (ref) => {
    const found = new Map();
    for (const file of grepFiles(ref, 'supersededBy', 'corral-rules/src/main')) {
      for (const [oldId, newId] of parseDeprecations([git('show', `${ref}:${file}`)])) {
        found.set(oldId, { newId, file });
      }
    }
    return found;
  };
  const before = deprecationsAt(from);
  const deprecations = [...deprecationsAt(to)]
    .filter(([oldId]) => !before.has(oldId))
    .map(([oldId, { newId, file }]) => ({
      oldId,
      newId,
      pr: prOf(lines(git('log', '--reverse', '--format=%s', '-GsupersededBy', range, '--', file))[0]),
    }));

  return renderNotes({
    prs,
    added: diff.added.map((r) => ({ ...r, pr: firstAdding(r.id) })),
    moved: diff.moved.map((r) => ({ ...r, pr: lastTouching(r.id) })),
    removed: diff.removed.map((id) => ({ id, pr: lastRemoving(id) })),
    deprecations,
    consumerKeys: consumerKeys(git('show', `${to}:pom.xml`), args.consumerArtifacts),
  });
}

if (require.main === module) {
  const args = parseArgs(process.argv.slice(2));
  if (!args) {
    process.stderr.write(`${USAGE}\n`);
    process.exit(2);
  }
  process.stdout.write(main(args));
}
