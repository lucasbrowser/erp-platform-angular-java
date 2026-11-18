import { canonicalMoneyInput, formatMoneyBRL } from "./money-decimal.utils";

describe("money decimal utilities", () => {
  it.each([
    ["150", "150.00"],
    ["150,5", "150.50"],
    ["150,75", "150.75"],
    ["999999999999.99", "999999999999.99"],
  ])("canonicalizes %s as %s", (input, expected) => {
    expect(canonicalMoneyInput(input)).toEqual({ value: expected, error: null });
  });

  it.each([
    ["", "required"],
    ["-0,01", "negative"],
    ["1.234", "precision"],
    ["1000000000000.00", "max"],
    ["1,2.3", "format"],
    ["abc", "format"],
  ] as const)("rejects %s with %s", (input, error) => {
    expect(canonicalMoneyInput(input)).toEqual({ value: null, error });
  });

  it.each([
    ["150.50", "R$ 150,50"],
    ["1250.00", "R$ 1.250,00"],
    ["999999999999.99", "R$ 999.999.999.999,99"],
  ])("formats %s as %s", (input, expected) => {
    expect(formatMoneyBRL(input)).toBe(expected);
  });
});
