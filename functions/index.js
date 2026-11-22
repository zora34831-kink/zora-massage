
const admin = require("firebase-admin");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");

admin.initializeApp();

const db = admin.firestore();

const OWNER_UID = process.env.ZORA_OWNER_UID;

function checkOwner(request) {
  if (!request.auth) {
    throw new HttpsError(
      "unauthenticated",
      "Login terlebih dahulu."
    );
  }

  if (!OWNER_UID || request.auth.uid !== OWNER_UID) {
    throw new HttpsError(
      "permission-denied",
      "Khusus Owner."
    );
  }
}

async function audit(actor, action, target, details = {}) {
  await db.collection("adminAudit").add({
    actor,
    action,
    target,
    details,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
}

// OWNER BOOTSTRAP
exports.bootstrapOwner = onCall(async (request) => {
  checkOwner(request);

  const user = await admin.auth().getUser(request.auth.uid);

  await admin.auth().setCustomUserClaims(request.auth.uid, {
    ...(user.customClaims || {}),
    owner: true
  });

  await audit(
    request.auth.uid,
    "OWNER_BOOTSTRAP",
    request.auth.uid
  );

  return { success: true };
});

// LIST USERS
exports.ownerListUsers = onCall(async (request) => {
  checkOwner(request);

  const result = await admin.auth().listUsers(1000);

  const users = await Promise.all(
    result.users.map(async (user) => {
      const snap = await db.collection("users")
        .doc(user.uid)
        .get();

      const data = snap.data() || {};

      return {
        uid: user.uid,
        email: user.email || "",
        name: user.displayName || "",
        disabled: user.disabled,
        verified: data.verified === true,
        suspended: data.suspended === true,
        suspensionType: data.suspensionType || ""
      };
    })
  );

  return { users };
});

// VERIFY USER
exports.ownerSetVerified = onCall(async (request) => {
  checkOwner(request);

  const { uid, verified } = request.data || {};

  if (
    typeof uid !== "string" ||
    !uid ||
    uid === request.auth.uid ||
    typeof verified !== "boolean"
  ) {
    throw new HttpsError(
      "invalid-argument",
      "Data tidak valid."
    );
  }

  await db.collection("users").doc(uid).set({
    verified,
    verifiedAt: verified
      ? admin.firestore.FieldValue.serverTimestamp()
      : admin.firestore.FieldValue.delete()
  }, { merge: true });

  await audit(
    request.auth.uid,
    verified ? "VERIFY_USER" : "UNVERIFY_USER",
    uid
  );

  return { success: true };
});

// SUSPEND USER
exports.ownerSuspendUser = onCall(async (request) => {
  checkOwner(request);

  const { uid, durationHours } = request.data || {};

  if (
    typeof uid !== "string" ||
    !uid ||
    uid === request.auth.uid
  ) {
    throw new HttpsError(
      "invalid-argument",
      "Target tidak valid."
    );
  }

  const temporary =
    Number.isFinite(durationHours) && durationHours > 0;

  const suspendUntil = temporary
    ? admin.firestore.Timestamp.fromMillis(
        Date.now() + durationHours * 3600000
      )
    : null;

  await admin.auth().updateUser(uid, {
    disabled: true
  });

  await db.collection("users").doc(uid).set({
    suspended: true,
    suspendUntil,
    suspensionType: temporary ? "temporary" : "permanent",
    suspendedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  await audit(
    request.auth.uid,
    temporary ? "TEMPORARY_SUSPEND" : "PERMANENT_SUSPEND",
    uid
  );

  return { success: true };
});

// UNSUSPEND USER
exports.ownerUnsuspendUser = onCall(async (request) => {
  checkOwner(request);

  const { uid } = request.data || {};

  if (
    typeof uid !== "string" ||
    !uid ||
    uid === request.auth.uid
  ) {
    throw new HttpsError(
      "invalid-argument",
      "Target tidak valid."
    );
  }

  await admin.auth().updateUser(uid, {
    disabled: false
  });

  await db.collection("users").doc(uid).set({
    suspended: false,
    suspendUntil: admin.firestore.FieldValue.delete(),
    suspensionType: admin.firestore.FieldValue.delete()
  }, { merge: true });

  await audit(
    request.auth.uid,
    "UNSUSPEND_USER",
    uid
  );

  return { success: true };
});

// AUTOMATIC EXPIRED SUSPENSION
exports.releaseExpiredSuspensions = onSchedule(
  "every 15 minutes",
  async () => {
    const now = admin.firestore.Timestamp.now();

    const expired = await db.collection("users")
      .where("suspended", "==", true)
      .where("suspendUntil", "<=", now)
      .get();

    for (const doc of expired.docs) {
      await admin.auth().updateUser(doc.id, {
        disabled: false
      });

      await doc.ref.update({
        suspended: false,
        suspendUntil: admin.firestore.FieldValue.delete(),
        suspensionType: admin.firestore.FieldValue.delete()
      });
    }
  }
);
