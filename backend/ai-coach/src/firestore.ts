import { AuthenticationError } from "./auth";
import type { FirebaseUser } from "./auth";

// Let the request handler return a useful error without exposing database details.
export class FirestoreReadError extends Error {
    constructor(
        message = "Saved wellness records are temporarily unavailable. Please try again.",
        public readonly status = 503,
    ) {
        super(message);
        this.name = "FirestoreReadError";
    }
}

// Read only the fields needed to calculate coaching summaries.
const ENTRY_FIELDS = {
    meals: ["date", "calories"],
    water: ["date", "amountOz"],
    workouts: ["date", "workoutType", "durationMinutes"],
    sleep: ["date", "hoursSlept"],
    checkIns: ["date", "createdAt", "stressLevel", "mood", "energyLevel"],
    steps: ["date", "steps"],
} as const;

export type EntryCollection = keyof typeof ENTRY_FIELDS;

export interface SavedEntry {
    id: string;
    fields: Record<string, unknown>;
}

// Firestore's REST API wraps each value with its type.
interface FirestoreValue {
    stringValue?: string;
    integerValue?: string;
    doubleValue?: number;
    booleanValue?: boolean;
    nullValue?: null;
    arrayValue?: { values?: FirestoreValue[] };
    mapValue?: { fields?: Record<string, FirestoreValue> };
}

interface FirestoreDocument {
    name: string;
    fields?: Record<string, FirestoreValue>;
}

// Convert Firestore's wrapped values into ordinary JavaScript values.
function decodeValue(value: FirestoreValue): unknown {
    if ("nullValue" in value) return null;
    if (typeof value.stringValue === "string") return value.stringValue;
    if (typeof value.booleanValue === "boolean") return value.booleanValue;

    if (typeof value.integerValue === "string") {
        const number = Number(value.integerValue);
        if (/^-?\d+$/.test(value.integerValue) && Number.isSafeInteger(number)) {
            return number;
        }
    }

    if (typeof value.doubleValue === "number" && Number.isFinite(value.doubleValue)) {
        return value.doubleValue;
    }

    if (value.arrayValue) {
        return (value.arrayValue.values ?? []).map(decodeValue);
    }

    if (value.mapValue) {
        return decodeFields(value.mapValue.fields);
    }

    throw new FirestoreReadError("A saved wellness value could not be read.", 502);
}

function decodeFields(
    fields: Record<string, FirestoreValue> = {},
): Record<string, unknown> {
    return Object.fromEntries(
        Object.entries(fields).map(([name, value]) => [name, decodeValue(value)]),
    );
}

// Validate real calendar dates, including cases such as February 30.
export function dateToMillis(date: string): number {
    const milliseconds = Date.parse(`${date}T00:00:00.000Z`);

    if (
        !/^\d{4}-\d{2}-\d{2}$/.test(date) ||
        !Number.isFinite(milliseconds) ||
        new Date(milliseconds).toISOString().slice(0, 10) !== date
    ) {
        throw new FirestoreReadError("Use a valid date in yyyy-MM-dd format.", 400);
    }

    return milliseconds;
}

export class FirestoreReader {
    private readonly userUrl: string;
    private readonly idToken: string;

    constructor(projectId: string, user: FirebaseUser) {
        // Bind this reader to the account verified by auth.ts.
        // Individual read methods do not accept a different account's UID.
        this.userUrl =
            `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(projectId)}` +
            `/databases/(default)/documents/users/${encodeURIComponent(user.uid)}`;
        this.idToken = user.idToken;
    }

    async getProfile(): Promise<Record<string, unknown> | null> {
        const url = new URL(this.userUrl);

        // Request goals and category choices used by the coach.
        const fields = [
            "uid",
            "wellnessGoals",
            "selectedCategories",
            "dailyCalorieGoal",
            "dailyWaterGoalOz",
            "dailyStepGoal",
            "sleepGoalHours",
        ];

        for (const field of fields) {
            url.searchParams.append("mask.fieldPaths", field);
        }

        const document = await this.request<FirestoreDocument>(
            url,
            undefined,
            true,
        );

        // Null means this account does not have a saved profile.
        return document ? decodeFields(document.fields) : null;
    }

    async getEntries(
        collection: EntryCollection,
        startDate: string,
        endDate: string,
    ): Promise<SavedEntry[]> {
        const start = dateToMillis(startDate);
        const end = dateToMillis(endDate);

        if (end < start || end - start > 6 * 86400000) {
            throw new FirestoreReadError("Choose a date range of up to seven days.", 400);
        }

        if (!Object.hasOwn(ENTRY_FIELDS, collection)) {
            throw new FirestoreReadError("Unsupported tracking collection.", 400);
        }

        // Fetch one extra record so we can detect an incomplete result.
        const maximumEntries = 200;
        const rows = await this.request<
            { document?: FirestoreDocument; error?: unknown }[]
        >(`${this.userUrl}:runQuery`, {
            structuredQuery: {
                select: {
                    fields: ENTRY_FIELDS[collection].map(
                        fieldPath => ({ fieldPath }),
                    ),
                },
                from: [{ collectionId: collection, allDescendants: false }],
                where: {
                    compositeFilter: {
                        op: "AND",
                        filters: [
                            {
                                fieldFilter: {
                                    field: { fieldPath: "date" },
                                    op: "GREATER_THAN_OR_EQUAL",
                                    value: { stringValue: startDate },
                                },
                            },
                            {
                                fieldFilter: {
                                    field: { fieldPath: "date" },
                                    op: "LESS_THAN_OR_EQUAL",
                                    value: { stringValue: endDate },
                                },
                            },
                        ],
                    },
                },
                orderBy: [
                    { field: { fieldPath: "date" }, direction: "ASCENDING" },
                ],
                limit: maximumEntries + 1,
            },
        });

        if (!Array.isArray(rows) || rows.some(row => row.error)) {
            // Report error metadata without logging returned records.
            const wire: unknown = rows;
            let reason: unknown = {
                status: "UNEXPECTED_QUERY_RESPONSE",
                message: "Expected query results in a JSON array.",
            };

            if (Array.isArray(wire)) {
                reason = wire.find(row => row?.error)?.error ?? reason;
            } else if (wire && typeof wire === "object" && "error" in wire) {
                reason = wire.error;
            }

            this.logFailure(`query:${collection}`, 200, reason);
            throw new FirestoreReadError();
        }

        // An empty query can return metadata without a document.
        const documents = rows.flatMap(
            row => row.document ? [row.document] : [],
        );

        // Stop instead of presenting a truncated total as a complete total.
        if (documents.length > maximumEntries) {
            throw new FirestoreReadError(
                "Too many records in this date range. Try a shorter range.",
                422,
            );
        }

        return documents.map(document => ({
            id: document.name.split("/").pop() ?? "",
            fields: decodeFields(document.fields),
        }));
    }

    private async request<T>(
        url: string | URL,
        body?: unknown,
        allowMissing = false,
    ): Promise<T | null> {
        let response: Response;

        // Identify the operation without logging an account URL.
        const query = body as {
            structuredQuery?: { from?: { collectionId?: string }[] };
        } | undefined;

        const operation = body === undefined
            ? "profile"
            : `query:${query?.structuredQuery?.from?.[0]?.collectionId ?? "unknown"}`;

        try {
            // Use this user's token so Firestore applies its security rules.
            // Keep account reads uncached and stop stalled requests.
            response = await fetch(url, {
                method: body === undefined ? "GET" : "POST",
                headers: {
                    Authorization: `Bearer ${this.idToken}`,
                    "Content-Type": "application/json",
                },
                body: body === undefined ? undefined : JSON.stringify(body),
                cache: "no-store",
                redirect: "manual",
                signal: AbortSignal.timeout(10000),
            });
        } catch (error) {
            // HTTP status 0 means no HTTP response was received.
            this.logFailure(operation, 0, error);
            throw new FirestoreReadError();
        }

        if (response.status === 404 && allowMissing) return null;

        if (!response.ok) {
            let reason: unknown;

            try {
                const failure = await response.json() as { error?: unknown };
                reason = failure.error ?? {
                    message: "No Firestore error details were returned.",
                };
            } catch {
                reason = {
                    message: "The error response did not contain valid JSON.",
                };
            }

            this.logFailure(operation, response.status, reason);

            if (response.status === 401) {
                throw new AuthenticationError();
            }

            if (response.status === 403) {
                throw new FirestoreReadError(
                    "Access to saved wellness records was denied.",
                    403,
                );
            }

            throw new FirestoreReadError();
        }

        try {
            return await response.json() as T;
        } catch {
            // Avoid logging response contents from a JSON parsing error.
            this.logFailure(operation, response.status, {
                status: "INVALID_JSON",
                message: `Response was not valid JSON. Content type: ${
                    response.headers.get("Content-Type") ?? "unknown"
                }`,
            });

            throw new FirestoreReadError();
        }
    }

    private logFailure(
        operation: string,
        httpStatus: number,
        reason: unknown,
    ): void {
        const detail = reason !== null && typeof reason === "object"
            ? reason as Record<string, unknown>
            : {};

        // Log diagnostic fields only, with the sign-in token redacted.
        const message = typeof detail.message === "string"
            ? detail.message.replaceAll(this.idToken, "[token redacted]")
            : "No error message was returned.";

        console.error("Firestore diagnostic", {
            operation,
            httpStatus,
            status: typeof detail.status === "string" ? detail.status : undefined,
            code: typeof detail.code === "number" ? detail.code : undefined,
            errorType: reason instanceof Error ? reason.name : undefined,
            message,
        });
    }
}
