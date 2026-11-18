export type MoneyInputError = "required" | "negative" | "format" | "precision" | "max";

export interface MoneyInputResult {
  value: string | null;
  error: MoneyInputError | null;
}

const MAX_INTEGER = "999999999999";

/** Parses a user-entered decimal without passing through JavaScript floating point. */
export function canonicalMoneyInput(input: string): MoneyInputResult {
  if (input.trim() === "") return { value: null, error: "required" };
  const text = input.trim();
  if (text.startsWith("-")) return { value: null, error: "negative" };
  if (text.includes(",") && text.includes(".")) return { value: null, error: "format" };
  const normalized = text.replace(",", ".");
  if (!/^\d+(?:\.\d*)?$/.test(normalized)) return { value: null, error: "format" };
  const [rawInteger, fraction = ""] = normalized.split(".");
  if (fraction.length > 2) return { value: null, error: "precision" };
  if (normalized.endsWith(".")) return { value: null, error: "format" };
  const integer = rawInteger.replace(/^0+(?=\d)/, "");
  if (integer.length > MAX_INTEGER.length || (integer.length === MAX_INTEGER.length && integer > MAX_INTEGER)) {
    return { value: null, error: "max" };
  }
  return { value: `${integer}.${fraction.padEnd(2, "0")}`, error: null };
}

/** Formats a canonical API decimal string for Brazilian display, exactly. */
export function formatMoneyBRL(value: string): string {
  if (!/^\d{1,12}\.\d{2}$/.test(value)) throw new RangeError("Valor monetário canônico inválido.");
  const [integer, cents] = value.split(".");
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `R$ ${grouped},${cents}`;
}
