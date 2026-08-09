import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

export const backupRestoreDatabaseFile: MatcherPlugin = {
  slug: "backup-restore-database-file",
  description:
    "Logical backup archives, validated restore paths, database replacement, attachment staging, and rollback boundaries",
  noiseTier: "normal",
  filePatterns: [
    "app/src/main/java/com/finnvek/homecheck/backup/**/*.kt",
    "app/src/main/java/com/finnvek/homecheck/data/files/AttachmentStore.kt",
    "app/src/main/res/xml/backup_rules.xml",
    "app/src/main/res/xml/data_extraction_rules.xml",
  ],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!/backup|restore|rollback|ZipInputStream|ZipOutputStream|<full-backup-content|<data-extraction-rules/i.test(content)) {
      return [];
    }

    return regexCandidates("backup-restore-database-file", content, [
      {
        regex: /\b(?:suspend\s+)?fun\s+(?:export|exportTo|restore|restoreFrom)\s*\(/,
        label: "Backup or restore entry point",
      },
      {
        regex: /\b(?:ZipInputStream|ZipOutputStream)\s*\(/,
        label: "Backup archive processing boundary",
      },
      {
        regex: /\bBackupPathValidator\.isSafe\s*\(|\bcanonicalPath\b/,
        label: "Backup archive path validation",
      },
      {
        regex: /\brepository\.replaceAll\s*\(/,
        label: "Database snapshot replacement boundary",
      },
      {
        regex: /\b(?:replaceAllFrom|rollbackReplacement|restoreRollback)\s*\(|\brollbackDirectory\b/,
        label: "Attachment staging or rollback boundary",
      },
      {
        regex: /<exclude\b[^>]*\bdomain\s*=\s*(["'])(?:root|file|database|sharedpref|external|device_[^"']+)\1[^>]*>/i,
        label: "Android backup exclusion rule",
      },
    ]);
  },
};
