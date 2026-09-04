// Indian compact number formatting: thousand -> T, lakh -> L, crore -> Cr.
// e.g. 1,000 -> "1T", 25,500 -> "25.5T", 1,00,000 -> "1L", 14,44,455 -> "14.44L", 1,00,00,000 -> "1Cr".
// Values below 1,000 (ones/tens/hundreds) are left as-is, with normal comma grouping.

const CRORE = 1_00_00_000;
const LAKH = 1_00_000;
const THOUSAND = 1_000;

function compactMagnitude(abs: number): { divisor: number; suffix: string } | null {
  if (abs >= CRORE) return { divisor: CRORE, suffix: "Cr" };
  if (abs >= LAKH) return { divisor: LAKH, suffix: "L" };
  if (abs >= THOUSAND) return { divisor: THOUSAND, suffix: "T" };
  return null;
}

function roundedCompactValue(abs: number, divisor: number): string {
  const rounded = Math.round((abs / divisor) * 100) / 100;
  return rounded.toString();
}

/** Compact Indian-style count: 2,10,341 -> "2.1L". Below 1,000, returns the plain comma-grouped number. */
export function formatCompactNumber(value: number): string {
  const sign = value < 0 ? "-" : "";
  const abs = Math.abs(value);
  const magnitude = compactMagnitude(abs);
  if (!magnitude) return `${sign}${Math.round(abs).toLocaleString("en-IN")}`;
  return `${sign}${roundedCompactValue(abs, magnitude.divisor)}${magnitude.suffix}`;
}

/** Compact Indian-style rupee amount: ₹14,44,455 -> "₹14.44L". Below ₹1,000, keeps full paise precision. */
export function formatCompactInr(value: number, smallFractionDigits = 2): string {
  const sign = value < 0 ? "-" : "";
  const abs = Math.abs(value);
  const magnitude = compactMagnitude(abs);
  if (!magnitude) {
    return `${sign}₹${abs.toLocaleString("en-IN", {
      minimumFractionDigits: smallFractionDigits,
      maximumFractionDigits: smallFractionDigits,
    })}`;
  }
  return `${sign}₹${roundedCompactValue(abs, magnitude.divisor)}${magnitude.suffix}`;
}
