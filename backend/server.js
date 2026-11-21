
import "dotenv/config";
import express from "express";
import crypto from "node:crypto";
import nodemailer from "nodemailer";
import { rateLimit } from "express-rate-limit";
import { initializeApp, cert } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const required = [
  "FIREBASE_SERVICE_ACCOUNT_JSON",
  "SMTP_HOST",
  "SMTP_USER",
  "SMTP_PASS",
  "MAIL_FROM",
  "OTP_PEPPER",
  "ENCRYPTION_KEY"
];

for (const key of required) {
  if (!process.env[key]) {
    throw new Error(`Environment ${key} belum diatur`);
  }
}

const encryptionKey = Buffer.from(
  process.env.ENCRYPTION_KEY,
  "hex"
);

if (encryptionKey.length !== 32) {
  throw new Error("ENCRYPTION_KEY wajib 64 karakter hex");
}

const serviceAccount = JSON.parse(
  process.env.FIREBASE_SERVICE_ACCOUNT_JSON
);

initializeApp({
  credential: cert(serviceAccount)
});

const auth = getAuth();
const db = getFirestore();
const app = express();

app.disable("x-powered-by");
app.use(express.json({ limit: "10kb" }));

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT || 465),
  secure: process.env.SMTP_SECURE !== "false",
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS
  }
});

const requestLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 3,
  standardHeaders: true,
  legacyHeaders: false
});

const verifyLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 10,
  standardHeaders: true,
  legacyHeaders: false
});

const normalizeEmail = value =>
  String(value || "").trim().toLowerCase();

const validEmail = value =>
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

const emailId = email =>
  crypto.createHash("sha256").update(email).digest("hex");

const hashOtp = (email, otp) =>
  crypto.createHmac("sha256", process.env.OTP_PEPPER)
    .update(`${email}:${otp}`)
    .digest("hex");

function encryptPassword(password) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv(
    "aes-256-gcm",
    encryptionKey,
    iv
  );

  const data = Buffer.concat([
    cipher.update(password, "utf8"),
    cipher.final()
  ]);

  return {
    iv: iv.toString("hex"),
    data: data.toString("hex"),
    tag: cipher.getAuthTag().toString("hex")
  };
}

function decryptPassword(value) {
  const decipher = crypto.createDecipheriv(
    "aes-256-gcm",
    encryptionKey,
    Buffer.from(value.iv, "hex")
  );

  decipher.setAuthTag(Buffer.from(value.tag, "hex"));

  return Buffer.concat([
    decipher.update(Buffer.from(value.data, "hex")),
    decipher.final()
  ]).toString("utf8");
}

function equalHash(a, b) {
  const x = Buffer.from(a, "hex");
  const y = Buffer.from(b, "hex");

  return x.length === y.length &&
    crypto.timingSafeEqual(x, y);
}

app.get("/health", (req, res) => {
  res.json({
    ok: true,
    service: "ZORA MASSAGE API"
  });
});

app.post(
  "/api/register/request-otp",
  requestLimiter,
  async (req, res) => {
    try {
      const name = String(req.body.name || "").trim();
      const email = normalizeEmail(req.body.email);
      const password = String(req.body.password || "");

      if (name.length < 2 || name.length > 40) {
        return res.status(400).json({
          error: "Nama harus 2 sampai 40 karakter."
        });
      }

      if (!validEmail(email)) {
        return res.status(400).json({
          error: "Format email tidak valid."
        });
      }

      if (password.length < 8 || password.length > 72) {
        return res.status(400).json({
          error: "Password harus 8 sampai 72 karakter."
        });
      }

      try {
        await auth.getUserByEmail(email);
        return res.status(409).json({
          error: "Email sudah terdaftar. Silakan login."
        });
      } catch (error) {
        if (error.code !== "auth/user-not-found") {
          throw error;
        }
      }

      const id = emailId(email);
      const ref = db.collection("pendingRegistrations").doc(id);
      const existing = await ref.get();

      if (existing.exists &&
          existing.data().nextAllowedAt > Date.now()) {
        return res.status(429).json({
          error: "Tunggu sebelum meminta OTP baru."
        });
      }

      const otp = String(crypto.randomInt(100000, 1000000));

      await ref.set({
        name,
        email,
        otpHash: hashOtp(email, otp),
        encryptedPassword: encryptPassword(password),
        expiresAt: Date.now() + 5 * 60 * 1000,
        nextAllowedAt: Date.now() + 60 * 1000,
        attempts: 0
      });

      try {
        await transporter.sendMail({
          from: process.env.MAIL_FROM,
          to: email,
          subject: "Kode OTP ZORA MASSAGE",
          text:
            `Halo ${name}, kode OTP kamu adalah ${otp}. ` +
            "Kode berlaku 5 menit. Jangan bagikan kode ini.",
          html: `
            <div style="font-family:Arial,sans-serif;padding:24px;color:#171717">
              <h2>ZORA MASSAGE</h2>
              <p>Halo ${name.replace(/[<>&"]/g, "")},</p>
              <p>Kode verifikasi pendaftaran kamu:</p>
              <div style="font-size:32px;font-weight:bold;letter-spacing:8px;background:#fff2bd;padding:20px;text-align:center">
                ${otp}
              </div>
              <p>Kode berlaku selama 5 menit.</p>
              <p>Jangan bagikan kode ini kepada siapa pun.</p>
              <b>ZORA STORE</b>
            </div>
          `
        });
      } catch (mailError) {
        await ref.delete();
        throw mailError;
      }

      return res.json({
        ok: true,
        message: "OTP berhasil dikirim ke email."
      });

    } catch (error) {
      console.error("Request OTP:", error.message);
      return res.status(500).json({
        error: "Gagal mengirim OTP. Periksa konfigurasi SMTP."
      });
    }
  }
);

app.post(
  "/api/register/verify-otp",
  verifyLimiter,
  async (req, res) => {
    try {
      const email = normalizeEmail(req.body.email);
      const otp = String(req.body.otp || "");

      if (!validEmail(email) || !/^\d{6}$/.test(otp)) {
        return res.status(400).json({
          error: "Email atau OTP tidak valid."
        });
      }

      const ref = db.collection("pendingRegistrations")
        .doc(emailId(email));

      const snapshot = await ref.get();

      if (!snapshot.exists) {
        return res.status(400).json({
          error: "OTP tidak ditemukan. Minta kode baru."
        });
      }

      const record = snapshot.data();

      if (Date.now() > record.expiresAt) {
        await ref.delete();
        return res.status(400).json({
          error: "OTP kedaluwarsa. Minta kode baru."
        });
      }

      if (record.attempts >= 5) {
        await ref.delete();
        return res.status(429).json({
          error: "Batas percobaan OTP tercapai."
        });
      }

      const submitted = hashOtp(email, otp);

      if (!equalHash(record.otpHash, submitted)) {
        await ref.update({
          attempts: FieldValue.increment(1)
        });

        return res.status(400).json({
          error: "Kode OTP salah."
        });
      }

      const password = decryptPassword(
        record.encryptedPassword
      );

      let user;

      try {
        user = await auth.createUser({
          email,
          password,
          displayName: record.name,
          emailVerified: true,
          disabled: false
        });
      } catch (error) {
        if (error.code === "auth/email-already-exists") {
          await ref.delete();
          return res.status(409).json({
            error: "Email sudah terdaftar."
          });
        }
        throw error;
      }

      await db.collection("users").doc(user.uid).set({
        uid: user.uid,
        name: record.name,
        email,
        emailVerified: true,
        createdAt: FieldValue.serverTimestamp()
      });

      await ref.delete();

      return res.json({
        ok: true,
        message: "Pendaftaran berhasil.",
        uid: user.uid
      });

    } catch (error) {
      console.error("Verify OTP:", error.message);
      return res.status(500).json({
        error: "Verifikasi gagal. Silakan coba lagi."
      });
    }
  }
);

const port = Number(process.env.PORT || 3000);

app.listen(port, "0.0.0.0", () => {
  console.log(`ZORA MASSAGE API aktif di port ${port}`);
});
