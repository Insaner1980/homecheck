import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import {
  candidate,
  findBalancedDelimiterEnd,
  findCallOpeningParenthesis,
  isTestFile,
} from "./utils.js";

const sensitiveWords =
  "(?:homecheck|asset|attachment|maintenance|task|history|backup|restore|uri|fileprovider|camera|photo|image|address|notes?|purchase|billing|token|database)";
const sensitiveWordPattern = new RegExp(sensitiveWords, "i");
const logCallStart = /\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e|wtf)\s*\(/g;

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose home assets, attachments, maintenance history, backup paths, billing state, or file URIs",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return [...content.matchAll(logCallStart)].flatMap((match) => {
      const start = match.index ?? 0;
      const callIdentifier = match[0].slice(0, match[0].lastIndexOf("(")).trim();
      const openParenthesis = findCallOpeningParenthesis(
        content,
        callIdentifier,
        start + match[0].length - 1,
      );
      if (openParenthesis === null) return [];

      const end = findBalancedDelimiterEnd(content, openParenthesis, "(", ")");
      if (end === null || !sensitiveWordPattern.test(content.slice(openParenthesis + 1, end))) return [];

      return candidate("sensitive-android-log", content, start, "Sensitive term in Android log call");
    });
  },
};
