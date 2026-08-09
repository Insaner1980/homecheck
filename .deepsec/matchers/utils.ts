import type { CandidateMatch } from "deepsec/config";

export function isTestFile(filePath: string): boolean {
  return /(?:^|[\\/])(?:test|androidTest|__tests__)(?:[\\/])|[._-](?:test|spec)\./i.test(filePath);
}

export function findBalancedDelimiterEnd(
  content: string,
  openIndex: number,
  opening: "(" | "{",
  closing: ")" | "}",
): number | null {
  if (content[openIndex] !== opening) return null;

  let depth = 0;
  for (const cursor of codeCharacterIndices(content, openIndex, content.length)) {
    if (content[cursor] === opening) {
      depth += 1;
    } else if (content[cursor] === closing) {
      depth -= 1;
      if (depth === 0) return cursor;
    }
  }
  return null;
}

export function findEnclosingBlockEnds(content: string, index: number): number[] {
  const openBraces: number[] = [];
  for (const cursor of codeCharacterIndices(content, 0, index)) {
    if (content[cursor] === "{") {
      openBraces.push(cursor);
    } else if (content[cursor] === "}") {
      openBraces.pop();
    }
  }

  return openBraces
    .reverse()
    .map((openBrace) => findBalancedDelimiterEnd(content, openBrace, "{", "}"))
    .filter((end): end is number => end !== null);
}

export function findCallOpeningParenthesis(
  content: string,
  identifier: string,
  containingIndex: number,
): number | null {
  const escapedIdentifier = identifier.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const identifierPattern = new RegExp(`\\b${escapedIdentifier}\\b`, "g");
  const codeIndices = new Set(codeCharacterIndices(content, 0, containingIndex + 1));
  const matches = [...content.slice(0, containingIndex + 1).matchAll(identifierPattern)];

  for (const match of matches.reverse()) {
    const identifierStart = match.index ?? 0;
    if (!codeIndices.has(identifierStart)) continue;

    const identifierEnd = identifierStart + match[0].length;
    const openParenthesis = firstNonWhitespaceCodeIndex(content, identifierEnd, containingIndex + 1);
    if (openParenthesis === null || content[openParenthesis] !== "(") continue;

    const closeParenthesis = findBalancedDelimiterEnd(content, openParenthesis, "(", ")");
    if (closeParenthesis !== null && closeParenthesis >= containingIndex) return openParenthesis;
  }

  return null;
}

export function lineNumberAt(content: string, index: number): number {
  return content.slice(0, index).split(/\r\n|\r|\n/).length;
}

export function isCodeCharacterIndex(content: string, index: number): boolean {
  for (const cursor of codeCharacterIndices(content, 0, index + 1)) {
    if (cursor === index) return true;
  }
  return false;
}

export function snippetAroundLine(content: string, lineNumber: number, context = 3): string {
  const lines = content.split(/\r\n|\r|\n/);
  const start = Math.max(0, lineNumber - context - 1);
  const end = Math.min(lines.length, lineNumber + context);
  return lines.slice(start, end).join("\n");
}

export function candidate(
  slug: string,
  content: string,
  index: number,
  matchedPattern: string,
): CandidateMatch {
  const lineNumber = lineNumberAt(content, index);
  return {
    vulnSlug: slug,
    lineNumbers: [lineNumber],
    snippet: snippetAroundLine(content, lineNumber),
    matchedPattern,
  };
}

export function regexCandidates(
  slug: string,
  content: string,
  patterns: Array<{ regex: RegExp; label: string }>,
): CandidateMatch[] {
  const matches: CandidateMatch[] = [];

  for (const { regex, label } of patterns) {
    const flags = regex.flags.includes("g") ? regex.flags : `${regex.flags}g`;
    const globalRegex = new RegExp(regex.source, flags);
    for (const match of content.matchAll(globalRegex)) {
      matches.push(candidate(slug, content, match.index ?? 0, label));
    }
  }

  return matches;
}

function* codeCharacterIndices(content: string, start: number, end: number): Generator<number> {
  let quote: "'" | '"' | '"""' | null = null;
  let escaped = false;
  let lineComment = false;
  let blockCommentDepth = 0;

  for (let cursor = start; cursor < end; cursor += 1) {
    if (lineComment) {
      if (content[cursor] === "\n" || content[cursor] === "\r") lineComment = false;
      continue;
    }

    if (blockCommentDepth > 0) {
      if (content.startsWith("/*", cursor)) {
        blockCommentDepth += 1;
        cursor += 1;
      } else if (content.startsWith("*/", cursor)) {
        blockCommentDepth -= 1;
        cursor += 1;
      }
      continue;
    }

    if (quote === '"""') {
      if (content.startsWith('"""', cursor)) {
        quote = null;
        cursor += 2;
      }
      continue;
    }

    if (quote !== null) {
      if (escaped) {
        escaped = false;
      } else if (content[cursor] === "\\") {
        escaped = true;
      } else if (content[cursor] === quote) {
        quote = null;
      }
      continue;
    }

    if (content.startsWith("//", cursor)) {
      lineComment = true;
      cursor += 1;
    } else if (content.startsWith("/*", cursor)) {
      blockCommentDepth = 1;
      cursor += 1;
    } else if (content.startsWith('"""', cursor)) {
      quote = '"""';
      cursor += 2;
    } else if (content[cursor] === "'" || content[cursor] === '"') {
      quote = content[cursor] as "'" | '"';
    } else {
      yield cursor;
    }
  }
}

function firstNonWhitespaceCodeIndex(content: string, start: number, end: number): number | null {
  for (const cursor of codeCharacterIndices(content, start, end)) {
    if (!/\s/.test(content[cursor] ?? "")) return cursor;
  }
  return null;
}
