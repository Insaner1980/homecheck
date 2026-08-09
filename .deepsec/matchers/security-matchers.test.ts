import assert from "node:assert/strict";
import { test } from "node:test";
import { androidExportedComponent } from "./android-exported-component.js";
import { androidUriShareWithoutClipData } from "./android-uri-share-without-clipdata.js";
import { backupRestoreDatabaseFile } from "./backup-restore-database-file.js";
import { fileproviderBroadPath } from "./fileprovider-broad-path.js";
import { sensitiveAndroidLog } from "./sensitive-android-log.js";

test("exported component matcher covers single-quoted attributes", () => {
  const matches = androidExportedComponent.match(
    "<manifest><application><service android:name='.SyncService' android:exported='true'/></application></manifest>",
    "app/src/main/AndroidManifest.xml",
  );

  assert.equal(matches.length, 1);
});

test("exported component matcher does not cross from a private activity into a public service", () => {
  const matches = androidExportedComponent.match(
    `<manifest><application>
      <activity android:name='.PrivateActivity' />
      <service android:name='.PublicService' android:exported='true' />
    </application></manifest>`,
    "app/src/main/AndroidManifest.xml",
  );

  assert.equal(matches.length, 1);
  assert.deepEqual(matches[0]?.lineNumbers, [3]);
});

test("exported component matcher reports public components separately", () => {
  const matches = androidExportedComponent.match(
    `<manifest><application>
      <activity android:name='.PublicActivity' android:exported='true' />
      <receiver android:name='.PublicReceiver' android:exported="true" />
    </application></manifest>`,
    "app/src/main/AndroidManifest.xml",
  );

  assert.equal(matches.length, 2);
  assert.deepEqual(matches.map((match) => match.lineNumbers[0]), [2, 3]);
});

test("exported component matcher covers public providers", () => {
  const matches = androidExportedComponent.match(
    "<manifest><application><provider android:name='.SharedProvider' android:exported='true'/></application></manifest>",
    "app/src/main/AndroidManifest.xml",
  );

  assert.equal(matches.length, 1);
});

test("FileProvider matcher covers single-quoted broad paths", () => {
  const matches = fileproviderBroadPath.match(
    "<paths><cache-path name='cache' path='.'/></paths>",
    "app/src/main/res/xml/file_paths.xml",
  );

  assert.equal(matches.length, 1);
});

test("URI share matcher evaluates safe and unsafe builders independently", () => {
  const content = `
fun safe(uri: Uri): Intent =
  Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_STREAM, uri)
    clipData = ClipData.newUri(resolver, "safe", uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }

fun unsafe(uri: Uri): Intent =
  Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_STREAM, uri)
  }
`;

  const matches = androidUriShareWithoutClipData.match(
    content,
    "app/src/main/java/com/finnvek/homecheck/ui/ShareFactory.kt",
  );

  assert.equal(matches.length, 1);
  assert.equal(
    matches[0]?.matchedPattern,
    "EXTRA_STREAM content URI share without FLAG_GRANT_READ_URI_PERMISSION",
  );
});

test("URI share matcher reports missing ClipData when the read grant exists", () => {
  const matches = androidUriShareWithoutClipData.match(
    `fun share(uri: Uri) = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
      putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }`,
    "app/src/main/java/com/finnvek/homecheck/ui/ShareFactory.kt",
  );

  assert.equal(matches.length, 1);
  assert.equal(matches[0]?.matchedPattern, "EXTRA_STREAM content URI share without ClipData");
});

test("URI share matcher ignores ACTION_SEND inside a comment", () => {
  const matches = androidUriShareWithoutClipData.match(
    `// Intent(Intent.ACTION_SEND).apply {
//   putExtra(Intent.EXTRA_STREAM, uri)
// }`,
    "app/src/main/java/com/finnvek/homecheck/ui/ShareFactory.kt",
  );

  assert.deepEqual(matches, []);
});

test("URI share matcher ignores ACTION_SEND inside a string", () => {
  const matches = androidUriShareWithoutClipData.match(
    'val example = "Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri)"',
    "app/src/main/java/com/finnvek/homecheck/ui/ShareFactory.kt",
  );

  assert.deepEqual(matches, []);
});

test("backup matcher covers archive validation and database replacement", () => {
  const matches = backupRestoreDatabaseFile.match(
    `suspend fun restoreFrom(input: InputStream) {
      ZipInputStream(input).use { zip ->
        if (!BackupPathValidator.isSafe(zip.nextEntry.name)) return
        val path = target.canonicalPath
        repository.replaceAll(restored)
      }
    }`,
    "app/src/main/java/com/finnvek/homecheck/backup/BackupManager.kt",
  );

  assert.ok(matches.some((match) => match.matchedPattern === "Backup or restore entry point"));
  assert.ok(matches.some((match) => match.matchedPattern === "Backup archive processing boundary"));
  assert.ok(matches.some((match) => match.matchedPattern === "Backup archive path validation"));
  assert.ok(matches.some((match) => match.matchedPattern === "Database snapshot replacement boundary"));
});

test("backup matcher covers attachment rollback and ignores unrelated Kotlin", () => {
  const rollbackMatches = backupRestoreDatabaseFile.match(
    `suspend fun rollbackReplacement(handle: ReplacementHandle) {
      restoreRollback(handle.rollbackDirectory)
    }`,
    "app/src/main/java/com/finnvek/homecheck/data/files/AttachmentStore.kt",
  );
  const unrelatedMatches = backupRestoreDatabaseFile.match(
    "fun calculateTotal(values: List<Int>) = values.sum()",
    "app/src/main/java/com/finnvek/homecheck/data/files/AttachmentStore.kt",
  );

  assert.ok(rollbackMatches.length >= 1);
  assert.deepEqual(unrelatedMatches, []);
});

test("sensitive log matcher covers multiline calls and ignores ordinary or commented logs", () => {
  const sensitive = sensitiveAndroidLog.match(
    `Log.i(
      TAG,
      "Backup URI: $uri",
    )`,
    "app/src/main/java/com/finnvek/homecheck/backup/BackupLogger.kt",
  );
  const ordinary = sensitiveAndroidLog.match(
    'Log.i(TAG, "sync complete")',
    "app/src/main/java/com/finnvek/homecheck/SyncLogger.kt",
  );
  const commented = sensitiveAndroidLog.match(
    '// Log.i(TAG, "Backup URI: $uri")',
    "app/src/main/java/com/finnvek/homecheck/SyncLogger.kt",
  );

  assert.equal(sensitive.length, 1);
  assert.deepEqual(ordinary, []);
  assert.deepEqual(commented, []);
});
