const TITLE = /^(feat|fix|docs|build|ci|test|chore|refactor|perf|style|revert)(\(([^)]+)\))?(!)?: (\S.*)$/;

function parseTitle(title) {
  const m = TITLE.exec(title);
  if (!m) return null;
  return { type: m[1], scope: m[3] ?? null, breaking: m[4] === '!', subject: m[5] };
}

function upgradeNotes(body) {
  if (!body) return null;
  const lines = body.replaceAll('\r\n', '\n').split('\n');
  const start = lines.findIndex((l) => /^##\s+upgrade notes\s*$/i.test(l));
  if (start < 0) return null;
  let fenced = false;
  let end = -1;
  for (let i = start + 1; i < lines.length; i++) {
    if (/^(```|~~~)/.test(lines[i])) fenced = !fenced;
    else if (!fenced && lines[i].startsWith('## ')) { end = i; break; }
  }
  const section = lines.slice(start + 1, end < 0 ? undefined : end).join('\n');
  return section.replace(/<!--[\s\S]*?-->/g, '').trim() || null;
}

function prNumberOf(commitSubject) {
  const m = /\(#(\d+)\)\s*$/.exec(commitSubject);
  return m ? Number(m[1]) : null;
}

module.exports = { parseTitle, upgradeNotes, prNumberOf };
