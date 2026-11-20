const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_dev_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validAppPayload(developerId, overrides = {}) {
  const now = new Date(Date.now() - 5000);
  return {
    developerId,
    developerName: "Nova Forge Studios",
    title: "Hyper Drift Arena",
    tagline: "High-speed neon canyon racing",
    description:
      "Race across floating skyways with precision drift physics and global leaderboards.",
    appType: "game",
    category: "Racing",
    versionName: "1.0.0",
    sizeMb: 148,
    ageRating: "Everyone 10+",
    accentColorHex: "#00875A",
    iconPreset: "gamepad",
    tags: ["racing", "arcade", "offline"],
    releaseNotes: "Initial public release with 12 tracks.",
    published: true,
    starterCatalog: false,
    downloadsCount: 0,
    ratingSum: 0,
    ratingCount: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validLibraryPayload(appId, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    appId,
    title: "Hyper Drift Arena",
    developerName: "Nova Forge Studios",
    category: "Racing",
    appType: "game",
    installedVersion: "1.0.0",
    sizeMb: 148,
    accentColorHex: "#00875A",
    iconPreset: "gamepad",
    installedAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are blocked from all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("apps").where("published", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("apps").doc("app_1").set(validAppPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("developer_profiles").doc(ALICE_UID).get()
  );
  await assertFails(unauthDb.collection("developers").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("library").get()
  );
});

test("Developer app creation, schema enforcement, and shadow-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Valid create payload succeeds (mirrors Kotlin AppRepository.publishApp)
  await assertSucceeds(
    aliceDb.collection("apps").doc("hyper_drift").set(validAppPayload(ALICE_UID))
  );

  // Reject @DocumentId 'id' key inside document body
  await assertFails(
    aliceDb
      .collection("apps")
      .doc("app_with_id")
      .set(validAppPayload(ALICE_UID, { id: "app_with_id" }))
  );

  // Reject ghost field (Shadow Update/Create Test)
  await assertFails(
    aliceDb
      .collection("apps")
      .doc("ghost_app")
      .set(validAppPayload(ALICE_UID, { isVerified: true }))
  );

  // Reject spoofed developerId
  await assertFails(
    aliceDb
      .collection("apps")
      .doc("spoof_dev")
      .set(validAppPayload(BOB_UID))
  );

  // Reject non-zero initial downloadsCount
  await assertFails(
    aliceDb
      .collection("apps")
      .doc("fake_downloads")
      .set(validAppPayload(ALICE_UID, { downloadsCount: 999 }))
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("apps")
      .doc("future_ts")
      .set(
        validAppPayload(ALICE_UID, {
          createdAt: new Date(Date.now() + 60000),
          updatedAt: new Date(Date.now() + 60000),
        })
      )
  );
});

test("Storefront list queries vs developer drafts and unfiltered query denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("apps")
      .doc("pub_game")
      .set(validAppPayload(ALICE_UID, { published: true }))
  );
  await assertSucceeds(
    aliceDb
      .collection("apps")
      .doc("draft_game")
      .set(validAppPayload(ALICE_UID, { published: false }))
  );

  // Bob can query published apps (matches StoreRepository.observePublishedApps)
  await assertSucceeds(
    bobDb.collection("apps").where("published", "==", true).get()
  );

  // Alice can query all her own apps including drafts (matches StoreRepository.observeDeveloperApps)
  await assertSucceeds(
    aliceDb.collection("apps").where("developerId", "==", ALICE_UID).get()
  );

  // Bob CANNOT get Alice's unpublished draft
  await assertFails(bobDb.collection("apps").doc("draft_game").get());

  // Unfiltered list query MUST be rejected (Query Trust Test)
  await assertFails(bobDb.collection("apps").get());
});

test("Developer metadata update succeeds while protecting immutable & counter fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("apps").doc("app_edit").set(validAppPayload(ALICE_UID))
  );

  // Owner updates version & release notes
  await assertSucceeds(
    aliceDb
      .collection("apps")
      .doc("app_edit")
      .update({
        versionName: "1.1.0",
        releaseNotes: "Added time trial mode and 60fps boost.",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Owner CANNOT inflate downloadsCount directly
  await assertFails(
    aliceDb
      .collection("apps")
      .doc("app_edit")
      .update({
        downloadsCount: 500,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Owner CANNOT mutate createdAt
  await assertFails(
    aliceDb
      .collection("apps")
      .doc("app_edit")
      .update({
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Bob CANNOT edit Alice's app metadata
  await assertFails(
    bobDb
      .collection("apps")
      .doc("app_edit")
      .update({
        title: "Hijacked Game",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Atomic user download installs into user library and increments downloadsCount by 1", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("apps").doc("drift_1").set(validAppPayload(ALICE_UID))
  );

  // Non-atomic increment without library document fails
  await assertFails(
    bobDb
      .collection("apps")
      .doc("drift_1")
      .update({
        downloadsCount: 1,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Atomic batch: create users/bob/library/drift_1 + increment downloadsCount by 1 succeeds
  const batch = bobDb.batch();
  const libRef = bobDb
    .collection("users")
    .doc(BOB_UID)
    .collection("library")
    .doc("drift_1");
  const appRef = bobDb.collection("apps").doc("drift_1");
  batch.set(libRef, validLibraryPayload("drift_1"));
  batch.update(appRef, {
    downloadsCount: 1,
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(batch.commit());

  // Bob can list his own library; Alice cannot read Bob's library
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("library").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("library").get()
  );

  // Bob can update installedVersion and uninstall
  await assertSucceeds(
    libRef.update({
      installedVersion: "1.1.0",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(libRef.delete());
});

test("Atomic user reviews create, edit, delete, and prevent cross-user tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("apps").doc("rev_app").set(validAppPayload(ALICE_UID))
  );

  const reviewRef = bobDb
    .collection("apps")
    .doc("rev_app")
    .collection("reviews")
    .doc(BOB_UID);
  const appRef = bobDb.collection("apps").doc("rev_app");
  const now = new Date(Date.now() - 2000);

  // Atomic create review + update ratingSum/ratingCount
  const createBatch = bobDb.batch();
  createBatch.set(reviewRef, {
    appId: "rev_app",
    userId: BOB_UID,
    authorName: "Bob Player",
    rating: 5,
    comment: "Silky smooth frame rate and crisp track design!",
    createdAt: now,
    updatedAt: now,
  });
  createBatch.update(appRef, {
    ratingSum: 5,
    ratingCount: 1,
    updatedAt: now,
  });
  await assertSucceeds(createBatch.commit());

  // Filtered list query succeeds (matches StoreRepository.observeAppReviews)
  await assertSucceeds(
    aliceDb
      .collection("apps")
      .doc("rev_app")
      .collection("reviews")
      .where("appId", "==", "rev_app")
      .get()
  );

  // Alice cannot edit or delete Bob's review
  await assertFails(
    aliceDb
      .collection("apps")
      .doc("rev_app")
      .collection("reviews")
      .doc(BOB_UID)
      .delete()
  );

  // Atomic edit review rating 5 -> 4
  const editBatch = bobDb.batch();
  editBatch.update(reviewRef, {
    rating: 4,
    comment: "Great game, slight battery drain on high settings.",
    updatedAt: new Date(Date.now() - 1000),
  });
  editBatch.update(appRef, {
    ratingSum: 4,
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(editBatch.commit());

  // Atomic delete review + decrement ratingSum/ratingCount
  const deleteBatch = bobDb.batch();
  deleteBatch.delete(reviewRef);
  deleteBatch.update(appRef, {
    ratingSum: 0,
    ratingCount: 0,
    updatedAt: new Date(Date.now() - 500),
  });
  await assertSucceeds(deleteBatch.commit());
});

test("Developer account PII isolation between /developers and /developer_profiles", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Alice registers developer account (both private PII doc + public studio profile)
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("developers").doc(ALICE_UID), {
    userId: ALICE_UID,
    studioName: "Nova Forge Studios",
    contactEmail: "alice@novaforge.dev",
    termsAccepted: true,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("developer_profiles").doc(ALICE_UID), {
    userId: ALICE_UID,
    studioName: "Nova Forge Studios",
    tagline: "Crafting tactile arcade games",
    bio: "Independent studio focused on 120Hz mobile arcade titles.",
    specialty: "Indie Games",
    avatarPreset: "rocket",
    websiteUrl: "https://novaforge.example.com",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Bob CAN read Alice's public studio profile (no PII)
  await assertSucceeds(
    bobDb.collection("developer_profiles").doc(ALICE_UID).get()
  );

  // Bob CANNOT read or modify Alice's private developer account (PII Blanket Test)
  await assertFails(bobDb.collection("developers").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("developer_profiles")
      .doc(ALICE_UID)
      .update({
        studioName: "Hacked Studio",
        updatedAt: new Date(Date.now() - 500),
      })
  );
});
