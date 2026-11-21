const admin = require("firebase-admin");

const {
  onCall,
  HttpsError
} = require("firebase-functions/v2/https");

const {
  onSchedule
} = require("firebase-functions/v2/scheduler");

const {
  defineSecret
} = require("firebase-functions/params");

admin.initializeApp();

const db = admin.firestore();
const OWNER_UID = defineSecret("ZORA_OWNER_UID");

// Memastikan pemanggil adalah Owner yang sudah ditentukan.
function requireOwner(request) {
  if (!request.auth) {
    throw new HttpsError(
      "unauthenticated",
      "Silakan login terlebih dahulu."
    );
  }

  if (request.auth.uid !== OWNER_UID.value()) {
    throw new HttpsError(
      "permission-denied",
      "Akses hanya untuk Owner ZORA."
    );
  }
}

async function writeAudit(actor, action, target, details = {}) {
  await db.collection("adminAudit").add({
    actor,
    action,
    target,
    details,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
}

// Aktivasi Owner pertama kali.
exports.bootstrapOwner = onCall(
  { secrets: [OWNER_UID] },
  async (request) => {
    requireOwner(request);

    const user = await admin.auth().getUser(request.auth.uid);

    await admin.auth().setCustomUserClaims(
      request.auth.uid,
      {
        ...(user.customClaims || {}),
        owner: true
      }
    );

    await writeAudit(
      request.auth.uid,
      "OWNER_BOOTSTRAP",
      request.auth.uid
    );

    return { success: true };
  }
);

// Mengambil daftar pengguna.
exports.ownerListUsers = onCall(
  { secrets: [OWNER_UID] },
  async (request) => {
    requireOwner(request);

    const result = await admin.auth().listUsers(1000);

    const users = await Promise.all(
      result.users.map(async (user) => {
        const profile = await db
          .collection("users")
          .doc(user.uid)
          .get();

        const data = profile.data() || {};

        return {
          uid: user.uid,
          email: user.email || "",
          name: user.displayName || "",
          disabled: user.disabled,
          verified: data.verified === true,
          suspended: data.suspended === true,
          suspendUntil: data.suspendUntil
            ? data.suspendUntil.toMillis()
            : null
        };
      })
    );

    return { users };
  }
);

// Memberi atau mencabut centang biru.
exports.ownerSetVerified = onCall(
  { secrets: [OWNER_UID] },
  async (request) => {
    requireOwner(request);

    const { uid, verified } = request.data || {};

    if (
      typeof uid !== "string" ||
      !uid ||
      typeof verified !== "boolean"
    ) {
      throw new HttpsError(
        "invalid-argument",
        "Data verifikasi tidak valid."
      );
    }

    if (uid === request.auth.uid) {
      throw new HttpsError(
        "failed-precondition",
        "Tidak perlu mengubah verifikasi akun Owner."
      );
    }

    await db.collection("users").doc(uid).set({
      verified,
      verifiedAt: verified
        ? admin.firestore.FieldValue.serverTimestamp()
        : admin.firestore.FieldValue.delete()
    }, { merge: true });

    await writeAudit(
      request.auth.uid,
      verified ? "VERIFY_USER" : "UNVERIFY_USER",
      uid
    );

    return { success: true };
  }
);

// Suspend sementara atau permanen.
exports.ownerSuspendUser = onCall(
  { secrets: [OWNER_UID] },
  async (request) => {
    requireOwner(request);

    const { uid, durationHours } = request.data || {};

    if (
      typeof uid !== "string" ||
      !uid ||
      uid === request.auth.uid
    ) {
      throw new HttpsError(
        "invalid-argument",
        "Target akun tidak valid."
      );
    }

    const temporary =
      Number.isFinite(durationHours) &&
      durationHours > 0;

    const suspendUntil = temporary
      ? admin.firestore.Timestamp.fromMillis(
          Date.now() + durationHours * 60 * 60 * 1000
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

    await writeAudit(
      request.auth.uid,
      temporary ? "TEMPORARY_SUSPEND" : "PERMANENT_SUSPEND",
      uid,
      { durationHours: temporary ? durationHours : null }
    );

    return { success: true };
  }
);

// Membuka kembali akun.
exports.ownerUnsuspendUser = onCall(
  { secrets: [OWNER_UID] },
  async (request) => {
    requireOwner(request);

    const { uid } = request.data || {};

    if (
      typeof uid !== "string" ||
      !uid ||
      uid === request.auth.uid
    ) {
      throw new HttpsError(
        "invalid-argument",
        "Target akun tidak valid."
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

    await writeAudit(
      request.auth.uid,
      "UNSUSPEND_USER",
      uid
    );

    return { success: true };
  }
);

// Membuka otomatis akun yang masa suspend-nya selesai.
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
