package com.microfocus.octane.websocket;

import java.util.function.Supplier;

public class WSTestsUtils {

	private WSTestsUtils() {
	}

	/**
	 * This function will safely wait a stated amount of millis
	 *
	 * @param millisToWait amount of millis to delay
	 */
	public static void delay(long millisToWait) {
		long started = System.currentTimeMillis();
		while (System.currentTimeMillis() - started < millisToWait) {
			try {
				Thread.sleep(millisToWait);
			} catch (InterruptedException e) {
				//
			}
		}
	}

	/**
	 * This function will attempt to evaluate the provided condition until it is resolved or until __millisToWait__ millis has timed out
	 * Condition will be considered as resolved only when it'll return NON-NULL value (any value, so returning FALSE will also be considered as positive resolution)
	 *
	 * @param millisToWait max time to wait in millis
	 * @param condition    custom condition logic
	 * @param <T>          expected type of the result of the condition evaluation
	 * @return condition evaluation result, only in case it is NON-NULL (otherwise will continue to wait and eventually will throw)
	 */
	public static <T> T waitAtMostFor(long millisToWait, Supplier<T> condition) {
		if (millisToWait < 1000) {
			throw new IllegalArgumentException("for less than 1 sec await please use the GeneralUtils.sleep method");
		}

		T conditionEvaluationResult;
		long started = System.currentTimeMillis(),
				sleepPeriod = 300;
		while (System.currentTimeMillis() - started < millisToWait) {
			conditionEvaluationResult = condition.get();
			if (conditionEvaluationResult != null) {
				return conditionEvaluationResult;
			} else {
				delay(sleepPeriod);
			}
		}
		throw new IllegalStateException(millisToWait + "ms passed away, but condition failed to resolve to NON-NULL value");
	}
}
