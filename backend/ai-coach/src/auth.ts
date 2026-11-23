import {
    createRemoteJWKSet,
    customFetch,
    decodeProtectedHeader,
    errors,
    jwtVerify,
} from "jose";

// Distinguish rejected sign-ins (401) from verification-service trouble (503).
export class AuthenticationError extends Error {
    constructor(
        message = "Please sign in again to use the AI coach.",
        public readonly status: 401 | 503 = 401,
    ) {
        super(message);
        this.name = "AuthenticationError";
    }
}

// The verified token will later authorize Firestore reads.
// Never log this token or include it in an HTTP response.
export interface FirebaseUser {
    uid: string;
    idToken: string;
}

// Cache Google's public signing keys, rather than account information.
// Respect Google's cache lifetime and refresh at least every five minutes.
let keysExpireAt = 0;
const firebaseKeys = createRemoteJWKSet(
    new URL(
        "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com",
    ),
    {
        cacheMaxAge: 5 * 60 * 1000,
        timeoutDuration: 5000,
        [customFetch]: async (url, options) => {
            const response = await fetch(url, options);
            const cacheControl = response.headers.get("Cache-Control") ?? "";
            const maxAge = Number(
                cacheControl.match(/(?:^|,)\s*max-age\s*=\s*"?(\d+)/i)?.[1] ?? 0,
            );
            const age = Number(response.headers.get("Age") ?? 0);
            const canCache = !/\b(?:no-cache|no-store)\b/i.test(cacheControl);
            const remaining = canCache && Number.isFinite(age)
                ? Math.max(0, maxAge - age)
                : 0;

            keysExpireAt = Date.now() + remaining * 1000;
            return response;
        },
    },
);

export async function requireFirebaseUser(
    request: Request,
    projectId: string,
): Promise<FirebaseUser> {
    // Android sends its Firebase ID token in this HTTP header.
    const idToken = request.headers.get("Authorization")
        ?.match(/^Bearer[ \t]+(\S+)$/i)?.[1];

    if (!idToken || idToken.length > 16384) {
        throw new AuthenticationError();
    }

    if (!projectId) {
        throw new AuthenticationError(
            "Sign-in verification is not configured.",
            503,
        );
    }

    // These header fields only select the verification key.
    // Account information is trusted only after jwtVerify succeeds.
    let header: ReturnType<typeof decodeProtectedHeader>;

    try {
        header = decodeProtectedHeader(idToken);
    } catch {
        throw new AuthenticationError();
    }

    if (
        header.alg !== "RS256" ||
        typeof header.kid !== "string" ||
        !header.kid
    ) {
        throw new AuthenticationError();
    }

    try {
        if (firebaseKeys.fresh && Date.now() >= keysExpireAt) {
            await firebaseKeys.reload();
        }

        // Check Google's signature, Firebase project, issuer, and expiry.
        // Required fields cannot be silently omitted.
        const { payload } = await jwtVerify(idToken, firebaseKeys, {
            algorithms: ["RS256"],
            audience: projectId,
            issuer: `https://securetoken.google.com/${projectId}`,
            requiredClaims: ["exp", "iat", "auth_time", "sub"],
        });

        // Check the issue/sign-in times and the account's UID.
        const now = Math.floor(Date.now() / 1000);
        const validTime = (value: unknown): value is number =>
            typeof value === "number" &&
            Number.isSafeInteger(value) &&
            value >= 0;

        if (
            payload.aud !== projectId ||
            typeof payload.sub !== "string" ||
            payload.sub.length === 0 ||
            payload.sub.length > 128 ||
            !validTime(payload.exp) || payload.exp <= now ||
            !validTime(payload.iat) || payload.iat > now ||
            !validTime(payload.auth_time) || payload.auth_time > now
        ) {
            throw new AuthenticationError();
        }

        return { uid: payload.sub, idToken };
    } catch (error) {
        if (error instanceof AuthenticationError) {
            throw error;
        }

        // Invalid, expired, or incorrectly signed tokens require sign-in.
        if (
            error instanceof errors.JWTExpired ||
            error instanceof errors.JWTClaimValidationFailed ||
            error instanceof errors.JWTInvalid ||
            error instanceof errors.JWSInvalid ||
            error instanceof errors.JWSSignatureVerificationFailed ||
            error instanceof errors.JOSEAlgNotAllowed ||
            error instanceof errors.JOSENotSupported ||
            error instanceof errors.JWKSNoMatchingKey
        ) {
            throw new AuthenticationError();
        }

        // Stop the request if the key service cannot be reached.
        // Keep tokens and internal error details out of responses and logs.
        throw new AuthenticationError(
            "Sign-in verification is temporarily unavailable. Please try again.",
            503,
        );
    }
}
