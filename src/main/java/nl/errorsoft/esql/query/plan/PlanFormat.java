package nl.errorsoft.esql.query.plan;

import java.util.Locale;

/** Numbers of a plan as text: rows and costs with thousands separators, times in ms. */
public final class PlanFormat {
	private PlanFormat() {
	}

	/** 98000 as "98,000", 0.25 as "0.25", 101.81 as "101.81". */
	public static String number(double value) {
		if (value == Math.rint(value) || Math.abs(value) >= 1000) {
			return String.format(Locale.ROOT, "%,d", Math.round(value));
		}
		return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	/** A time in ms: "0.94 ms", "1,203 ms". */
	public static String millis(double value) {
		return (value < 10 ? String.format(Locale.ROOT, "%.2f", value) : number(value)) + " ms";
	}
}
