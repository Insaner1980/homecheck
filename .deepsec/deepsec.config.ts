import { type DeepsecPlugin, defineConfig } from "deepsec/config";
import { androidExportedComponent } from "./matchers/android-exported-component.js";
import { androidUriShareWithoutClipData } from "./matchers/android-uri-share-without-clipdata.js";
import { backupRestoreDatabaseFile } from "./matchers/backup-restore-database-file.js";
import { fileproviderBroadPath } from "./matchers/fileprovider-broad-path.js";
import { sensitiveAndroidLog } from "./matchers/sensitive-android-log.js";

function homecheckPlugin(): DeepsecPlugin {
  return {
    name: "homecheck-android",
    matchers: [
      androidExportedComponent,
      fileproviderBroadPath,
      androidUriShareWithoutClipData,
      backupRestoreDatabaseFile,
      sensitiveAndroidLog,
    ],
  };
}

export default defineConfig({
  projects: [
    {
      id: "homecheck",
      root: "..",
      promptAppend:
        "Prioritize exported Android component trust boundaries, narrow FileProvider grants, attachment URI sharing, logical backup and restore archive validation and rollback, and sensitive Android logging.",
      priorityPaths: [
        "app/src/main/AndroidManifest.xml",
        "app/src/main/res/xml/",
        "app/src/main/java/com/finnvek/homecheck/backup/",
        "app/src/main/java/com/finnvek/homecheck/data/files/",
        "app/src/main/java/com/finnvek/homecheck/ui/HomeCheckApp.kt",
      ],
    },
  ],
  plugins: [homecheckPlugin()],
});
