import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import {
  candidate,
  findBalancedDelimiterEnd,
  findCallOpeningParenthesis,
  findEnclosingBlockEnds,
  isCodeCharacterIndex,
  isTestFile,
} from "./utils.js";

const sendAction = /\bIntent\.ACTION_SEND(?:_MULTIPLE)?\b/g;

export const androidUriShareWithoutClipData: MatcherPlugin = {
  slug: "android-uri-share-without-clipdata",
  description:
    "ACTION_SEND content URI shares that should pair EXTRA_STREAM with read grants and ClipData",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!content.includes("Intent.ACTION_SEND") && !content.includes("Intent.ACTION_SEND_MULTIPLE")) return [];
    if (!content.includes("Intent.EXTRA_STREAM")) return [];

    return shareScopes(content).flatMap(({ start, text }) => {
      if (!text.includes("Intent.EXTRA_STREAM")) return [];

      const hasReadGrant = text.includes("FLAG_GRANT_READ_URI_PERMISSION");
      const hasClipData = /\bclipData\b|ClipData\./.test(text);
      if (hasReadGrant && hasClipData) return [];

      return candidate(
        "android-uri-share-without-clipdata",
        content,
        start,
        hasReadGrant
          ? "EXTRA_STREAM content URI share without ClipData"
          : "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
      );
    });
  },
};

function shareScopes(content: string): Array<{ start: number; text: string }> {
  const starts = [...content.matchAll(sendAction)]
    .map((match) => match.index ?? 0)
    .filter((start) => isCodeCharacterIndex(content, start));
  return starts.map((start, index) => {
    const nextShareStart = starts[index + 1] ?? content.length;
    const end = findShareScopeEnd(content, start, nextShareStart);
    return { start, text: content.slice(start, end) };
  });
}

function findShareScopeEnd(content: string, start: number, nextShareStart: number): number {
  const builderEnd = findIntentBuilderEnd(content, start);
  if (builderEnd !== null) {
    const boundedBuilderEnd = Math.min(nextShareStart, builderEnd + 1);
    if (content.slice(start, boundedBuilderEnd).includes("Intent.EXTRA_STREAM")) {
      return boundedBuilderEnd;
    }
  }

  for (const enclosingBlockEnd of findEnclosingBlockEnds(content, start)) {
    const boundedBlockEnd = Math.min(nextShareStart, enclosingBlockEnd + 1);
    if (content.slice(start, boundedBlockEnd).includes("Intent.EXTRA_STREAM")) {
      return boundedBlockEnd;
    }
  }

  return nextShareStart;
}

function findIntentBuilderEnd(content: string, actionStart: number): number | null {
  const openParenthesis = findCallOpeningParenthesis(content, "Intent", actionStart);
  if (openParenthesis === null) return null;

  const closeParenthesis = findBalancedDelimiterEnd(content, openParenthesis, "(", ")");
  if (closeParenthesis === null) return null;

  const builderMatch = /^\s*\.(?:apply|also|let|run)\s*\{/.exec(content.slice(closeParenthesis + 1));
  if (builderMatch === null) return null;

  const openBrace = closeParenthesis + 1 + builderMatch[0].lastIndexOf("{");
  return findBalancedDelimiterEnd(content, openBrace, "{", "}");
}
