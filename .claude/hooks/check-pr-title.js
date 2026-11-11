#!/usr/bin/env node
// Blocks `gh pr create` when the title is not a Conventional Commit or would read badly as a
// release-notes line. The notes print only the subject, without the type, so the
// subject alone must say what was delivered or fixed.
const fs = require('node:fs');
const path = require('node:path');

const { parseTitle } = require(path.join(__dirname, '..', '..', '.github', 'scripts', 'pr-text'));

const MAX_SUBJECT = 72;

const SECTION = {
  feat: 'New Features',
  fix: 'Bug Fixes',
  docs: 'Documentation',
};

// Words that, on their own after the verb, leave a reader not knowing what changed.
const GENERIC = new Set([
  'a', 'an', 'the', 'some', 'few', 'more', 'minor', 'small', 'various', 'several', 'other', 'misc',
  'and', 'or', 'of', 'for', 'to', 'in', 'on', 'with', 'after', 'from', 'it', 'its', 'this', 'that',
  'bug', 'bugs', 'issue', 'issues', 'problem', 'problems', 'error', 'errors', 'thing', 'things',
  'code', 'change', 'changes', 'stuff', 'test', 'tests', 'doc', 'docs', 'documentation', 'typo',
  'typos', 'cleanup', 'clean-up', 'build', 'ci', 'dependencies', 'deps', 'version', 'versions',
  'review', 'comments', 'feedback', 'pr', 'logic', 'implementation', 'refactoring', 'formatting',
]);

const PROCESS = /\b(wip|fixup!?|squash!?|tmp|misc|miscellaneous|tweaks?|follow-?up|as discussed|per review|review (comments|feedback|remarks)|pr (comments|feedback))\b/i;

// First words that end like non-imperative forms but are imperative verbs.
const IMPERATIVE_EXCEPTIONS = new Set(['bring', 'ping', 'string', 'embed', 'feed', 'need', 'seed', 'speed', 'shed']);

function nonImperative(word) {
  const w = word.toLowerCase();
  if (IMPERATIVE_EXCEPTIONS.has(w)) return false;
  return /ing$/.test(w) || /ed$/.test(w) || (/s$/.test(w) && !/(ss|us|is|as)$/.test(w));
}

function titleProblems(title) {
  const parsed = parseTitle(title);
  if (!parsed) {
    return ['Not a Conventional Commit. Use `type(scope)!: Subject` with type one of feat, fix, docs, build, ci, test, chore, refactor, perf, style, revert.'];
  }
  const { subject } = parsed;
  const words = subject.split(/\s+/);
  const problems = [];
  if (!/^[A-Z]/.test(subject)) problems.push('Capitalise the subject: "' + subject[0].toUpperCase() + subject.slice(1) + '".');
  if (/[.!?]$/.test(subject)) problems.push('Drop the trailing punctuation.');
  if (/\(#\d+\)\s*$/.test(subject)) problems.push('Drop the "(#N)": the squash merge appends it.');
  if (subject.length > MAX_SUBJECT) {
    problems.push(`Subject is ${subject.length} characters; keep it to ${MAX_SUBJECT} so it scans in a list. Name the main outcome; secondary changes go in the description.`);
  }
  if (nonImperative(words[0])) {
    problems.push(`Start with an imperative verb ("Add", "Fix", "Print"), not "${words[0]}".`);
  }
  if (PROCESS.test(subject)) {
    problems.push('Describe the outcome, not the process (review feedback, follow-up, WIP, tweaks).');
  }
  const specific = words.slice(1).map((w) => w.toLowerCase().replace(/[^\p{L}\p{N}.#@`_-]/gu, '')).filter((w) => w && !GENERIC.has(w));
  if (specific.length === 0) {
    problems.push('Too vague: after the verb, name what changed for a consumer — the rule, the behaviour, the symptom fixed.');
  }
  return problems;
}

function unquote(token) {
  if (token.startsWith("'")) return token.slice(1, -1);
  if (token.startsWith('"')) {
    const inner = token.slice(1, -1);
    if (/(^|[^\\])[$`]/.test(inner)) return null;
    return inner.replace(/\\(["\\$`])/g, '$1');
  }
  if (/[$`]/.test(token)) return null;
  return token.replace(/\\(.)/g, '$1');
}

const TOKEN = String.raw`("(?:[^"\\]|\\.)*"|'[^']*'|[^\s;&|]+)`;

// The `if` filter in settings.json is best-effort: Claude Code still runs the hook when it cannot tell
// which commands a Bash input runs. Only `gh` in command position counts, so a grep, echo or commit
// message mentioning `gh pr create` is not mistaken for one.
const COMMAND_START = String.raw`(?:^|[;&|(\n]|\b(?:then|do|else)\s)\s*(?:\w+=\S*\s+)*`;

// Returns undefined when the command opens no PR, null when it does but the title cannot be read
// literally, and the title otherwise.
function titleOf(command) {
  const start = new RegExp(COMMAND_START + String.raw`gh\s+pr\s+create\b`).exec(command);
  if (!start) return undefined;
  const rest = command.slice(start.index);
  const m = new RegExp(String.raw`\s--title(?:=|\s+)` + TOKEN).exec(rest) ?? new RegExp(String.raw`\s-t\s+` + TOKEN).exec(rest);
  if (!m) return null;
  return unquote(m[1]);
}

function deny(reason) {
  process.stdout.write(JSON.stringify({
    hookSpecificOutput: { hookEventName: 'PreToolUse', permissionDecision: 'deny', permissionDecisionReason: reason },
  }));
}

if (require.main === module) {
  const input = JSON.parse(fs.readFileSync(0, 'utf8'));
  const title = titleOf(input.tool_input?.command ?? '');
  if (title === undefined) process.exit(0);
  if (title === null) {
    deny('Blocked: pass the PR title literally with --title "type: Subject" (no --fill, --web or shell expansion) so it can be checked before the PR opens.');
    process.exit(0);
  }
  const problems = titleProblems(title);
  if (problems.length) {
    const parsed = parseTitle(title);
    const shown = parsed
      ? `\nIn the release notes it would read: "- ${parsed.subject} #N" under ${parsed.breaking ? 'Breaking Changes (as a heading)' : SECTION[parsed.type] ?? 'Internal'}.`
      : '';
    deny(`Blocked: PR title "${title}" would not read well in the release notes.\n- ${problems.join('\n- ')}${shown}\nThe subject alone must tell a reader skimming a long list what this PR delivered or fixed.`);
  }
  process.exit(0);
}

module.exports = { titleProblems, titleOf };
