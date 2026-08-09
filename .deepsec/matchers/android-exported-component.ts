import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { regexCandidates } from "./utils.js";

export const androidExportedComponent: MatcherPlugin = {
  slug: "android-exported-component",
  description:
    "Exported Android activities, aliases, services, receivers, and providers that need explicit trust-boundary review",
  noiseTier: "normal",
  filePatterns: ["app/src/main/AndroidManifest.xml"],
  match(content): CandidateMatch[] {
    return regexCandidates("android-exported-component", content, [
      {
        regex:
          /<(activity|activity-alias|service|receiver|provider)\b[^>]*android:exported\s*=\s*(["'])true\2[^>]*\/?>/,
        label: 'Android component with android:exported="true"',
      },
    ]);
  },
};
