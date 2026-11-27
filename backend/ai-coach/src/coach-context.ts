import { dateToMillis, FirestoreReadError } from "./firestore";
import type {
    EntryCollection,
    FirestoreReader,
    SavedEntry,
} from "./firestore";

// These identifiers match TrackingCategories in the Android app.
const ALL_CATEGORIES = [
    "meals", "hydration", "workouts", "sleep",
    "stress", "mood", "energy", "steps",
];

interface MetricSpec {
    collection: EntryCollection;
    field: string;
    unit: string;
    mode: "sum" | "single" | "latest";
    allDays: boolean;
    goalKey?: string;
    defaultGoal?: number;
}

interface MeasurementDay {
    date: string;
    entries: number;
    value: number | null;
}

// Describe how each category's saved records become a daily measurement.
// Meals/water averages include every calendar day, matching D1.
const METRICS: Record<string, MetricSpec> = {
    meals: {
        collection: "meals", field: "calories", unit: "kcal",
        mode: "sum", allDays: true,
        goalKey: "dailyCalorieGoal", defaultGoal: 2000,
    },
    hydration: {
        collection: "water", field: "amountOz", unit: "US fl oz",
        mode: "sum", allDays: true,
        goalKey: "dailyWaterGoalOz", defaultGoal: 64,
    },
    sleep: {
        collection: "sleep", field: "hoursSlept", unit: "hours",
        mode: "sum", allDays: false,
        goalKey: "sleepGoalHours", defaultGoal: 8,
    },
    steps: {
        collection: "steps", field: "steps", unit: "steps",
        mode: "single", allDays: false,
        goalKey: "dailyStepGoal", defaultGoal: 5000,
    },
    stress: {
        collection: "checkIns", field: "stressLevel", unit: "rating (1-10)",
        mode: "latest", allDays: false,
    },
    mood: {
        collection: "checkIns", field: "mood", unit: "rating (1-10)",
        mode: "latest", allDays: false,
    },
    energy: {
        collection: "checkIns", field: "energyLevel", unit: "rating (1-10)",
        mode: "latest", allDays: false,
    },
};

// Validate saved numbers before calculating summaries.
function readNumber(
    value: unknown,
    minimum = 0,
    maximum = Infinity,
): number {
    if (
        typeof value !== "number" ||
        !Number.isFinite(value) ||
        value < minimum ||
        value > maximum
    ) {
        throw new FirestoreReadError(
            "A saved wellness record contains an invalid number.",
            502,
        );
    }

    return value;
}

function readStrings(value: unknown, fallback: string[]): string[] {
    if (value === undefined) return [...fallback];

    if (
        !Array.isArray(value) ||
        !value.every((item): item is string => typeof item === "string")
    ) {
        throw new FirestoreReadError(
            "Saved profile settings could not be read.",
            502,
        );
    }

    return value;
}

// Build seven daily measurements for one category.
function measurementDays(
    dates: string[],
    entries: SavedEntry[],
    spec: MetricSpec,
): MeasurementDay[] {
    return dates.map(date => {
        const rows = entries.filter(entry => entry.fields.date === date);
        let selectedRows = rows;

        if (spec.mode === "single") {
            // Steps already contain the day's total.
            // Require the expected single document whose ID is the date.
            if (rows.length > 1 || rows.some(entry => entry.id !== date)) {
                throw new FirestoreReadError(
                    "Saved step totals need to be checked for this date.",
                    409,
                );
            }
        }

        if (spec.mode === "latest") {
            // Match D1 by using the newest check-in for this date.
            const latest = rows.reduce<SavedEntry | undefined>(
                (current, entry) => {
                    if (!current) return entry;

                    const entryTime = readNumber(entry.fields.createdAt ?? 0);
                    const currentTime = readNumber(current.fields.createdAt ?? 0);

                    return entryTime > currentTime ? entry : current;
                },
                undefined,
            );

            selectedRows = latest ? [latest] : [];
        }

        const values = selectedRows.map(entry => {
            const raw = entry.fields[spec.field];

            // A check-in can leave individual ratings unrecorded.
            if (spec.mode === "latest") {
                if (raw === null || raw === undefined) return null;

                const rating = readNumber(raw, 1, 10);
                if (!Number.isInteger(rating)) {
                    throw new FirestoreReadError(
                        "A saved check-in contains an invalid rating.",
                        502,
                    );
                }
                return rating;
            }

            return readNumber(raw);
        }).filter((value): value is number => value !== null);

        return {
            date,
            entries: rows.length,
            value: values.length
                ? values.reduce((total, value) => total + value, 0)
                : null,
        };
    });
}

export async function buildCoachContext(
    reader: Pick<FirestoreReader, "getProfile" | "getEntries">,
    userId: string,
    endDate: string,
) {
    // The caller supplies the phone's calendar date.
    // Build seven dates without depending on the server's local timezone.
    const end = dateToMillis(endDate);
    const dates = Array.from({ length: 7 }, (_, index) =>
        new Date(end - (6 - index) * 86400000).toISOString().slice(0, 10),
    );
    const startDate = dates[0]!;
    const dateSet = new Set(dates);

    const profile = await reader.getProfile();

    if (!profile) {
        throw new FirestoreReadError(
            "Complete your profile before using the AI coach.",
            409,
        );
    }

    if (profile.uid !== userId) {
        throw new FirestoreReadError(
            "The saved profile does not match the signed-in account.",
            403,
        );
    }

    // Match Android's default categories when the field is absent.
    // An explicitly empty list stays empty.
    const selectedCategories = [...new Set(
        readStrings(profile.selectedCategories, ALL_CATEGORIES)
            .filter(category => ALL_CATEGORIES.includes(category)),
    )];

    const wellnessGoals = readStrings(profile.wellnessGoals, []);
    const collections = new Set<EntryCollection>();

    for (const category of selectedCategories) {
        if (category === "workouts") {
            collections.add("workouts");
        } else {
            const spec = METRICS[category];
            if (spec) collections.add(spec.collection);
        }
    }

    // Load each required collection once.
    // Stress, mood, and energy share the same checkIns collection.
    const records = new Map<EntryCollection, SavedEntry[]>();

    await Promise.all([...collections].map(async collection => {
        const entries = await reader.getEntries(collection, startDate, endDate);

        for (const entry of entries) {
            const date = entry.fields.date;
            if (typeof date !== "string" || !dateSet.has(date)) {
                throw new FirestoreReadError(
                    "A saved wellness record contains an invalid date.",
                    502,
                );
            }
        }

        records.set(collection, entries);
    }));

    const categories: Record<string, unknown> = {};
    let hasRecordedData = false;

    for (const category of selectedCategories) {
        if (category === "workouts") {
            const entries = records.get("workouts") ?? [];

            const days = dates.map(date => {
                const rows = entries.filter(entry => entry.fields.date === date);
                const durations = rows
                    .map(entry => entry.fields.durationMinutes)
                    .filter(value => value !== undefined && value !== null)
                    .map(value => readNumber(value));

                return {
                    date,
                    entries: rows.length,
                    entriesWithDuration: durations.length,
                    recordedMinutes: durations.length
                        ? durations.reduce((total, value) => total + value, 0)
                        : null,
                };
            });

            categories.workouts = {
                days,
                totalEntries: entries.length,
                totalRecordedMinutes: days.reduce(
                    (total, day) => total + (day.recordedMinutes ?? 0), 0,
                ),
                entriesWithDuration: days.reduce(
                    (total, day) => total + day.entriesWithDuration, 0,
                ),
            };

            hasRecordedData ||= entries.length > 0;
            continue;
        }

        const spec = METRICS[category];
        if (!spec) continue;

        const days = measurementDays(
            dates,
            records.get(spec.collection) ?? [],
            spec,
        );

        const recordedValues = days
            .map(day => day.value)
            .filter((value): value is number => value !== null);

        const total = recordedValues.reduce((sum, value) => sum + value, 0);
        const divisor = spec.allDays ? dates.length : recordedValues.length;

        // Use Android's goal defaults when a goal field is absent.
        // A saved zero or null represents no positive goal.
        const savedGoal = spec.goalKey ? profile[spec.goalKey] : undefined;
        const rawGoal = savedGoal === undefined ? spec.defaultGoal : savedGoal;
        const goal = rawGoal === null || rawGoal === undefined
            ? null
            : readNumber(rawGoal);

        categories[category] = {
            unit: spec.unit,
            dailyGoal: goal !== null && goal > 0 ? goal : null,
            goalSource: spec.goalKey
                ? savedGoal === undefined ? "app_default" : "saved"
                : null,
            days,
            daysWithMeasurements: recordedValues.length,
            average: divisor ? total / divisor : null,
            averageBasis: spec.allDays
                ? "all seven calendar days"
                : "days with saved measurements",
        };

        hasRecordedData ||= recordedValues.length > 0;
    }

    // Return a compact report containing the selected categories.
    // Keep identifiers and account credentials out of the AI's context.
    return {
        startDate,
        endDate,
        selectedCategories,
        wellnessGoals: wellnessGoals.slice(0, 5).map(goal => goal.slice(0, 200)),
        wellnessGoalsShortened: wellnessGoals.length > 5 ||
            wellnessGoals.slice(0, 5).some(goal => goal.length > 200),
        hasRecordedData,
        categories,
        notes: [
            "Values describe saved records, not everything the person actually did.",
            "A null daily value means no measurement was saved for that date.",
            "Workout entries are individual exercise records, not necessarily separate sessions.",
            "Workout minutes include only entries with a recorded duration.",
        ],
    };
}
