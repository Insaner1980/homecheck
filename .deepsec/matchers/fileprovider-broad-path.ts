import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { regexCandidates } from "./utils.js";

export const fileproviderBroadPath: MatcherPlugin = {
  slug: "fileprovider-broad-path",
  description:
    "Broad Android FileProvider path declarations that can expose more local files than intended",
  noiseTier: "precise",
  filePatterns: ["app/src/main/res/xml/**/*.xml"],
  match(content, filePath): CandidateMatch[] {
    if (!filePath.endsWith("file_paths.xml") && !content.includes("<paths")) return [];

    return regexCandidates("fileprovider-broad-path", content, [
      { regex: /<root-path\b[^>]*>/i, label: "FileProvider root-path exposes filesystem root" },
      { regex: /<external-path\b[^>]*>/i, label: "FileProvider external-path exposes shared storage" },
      {
        regex:
          /<(?:files-path|cache-path|external-files-path|external-cache-path)\b[^>]*\bpath\s*=\s*(["'])[\s.\/]*\1[^>]*>/i,
        label: 'FileProvider path="." or equivalent broad directory',
      },
    ]);
  },
};
