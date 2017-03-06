package me.jezza.lava;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;

/**
 * @author Jezza
 */
public final class Times {
	private static final Map<String, Times> map = new HashMap<>();

	private final String name;
	private final int initialSize;
	private final List<Long> data;

	public Times(String name, int size) {
		this.name = name;
		initialSize = size;
		data = new ArrayList<>(size);
		map.put(name, this);
	}

	public String name() {
		return name;
	}

	public void add(long time) {
		data.add(time);
	}

	@Override
	public String toString() {
		int count = data.size();
		LongSummaryStatistics base = new LongSummaryStatistics();
		data.forEach(base::accept);
		long sum = base.getSum();
		long max = base.getMax();
		long min = base.getMin();
		double average = base.getAverage();
		List<Long> copy = new ArrayList<>(data);
		copy.sort(Long::compareTo);
		long median = count > 0 ? copy.get(count / 2) : 0;

		return Strings.format(name + "{{}{}|Sum:{}ms, Min:{}ms, Max:{}ms, Average:{}ms, Median:{}ms}", count, count > initialSize ? "|OVER:" + initialSize : "", String.format("%.4f", sum / 1_000_000D), String.format("%.4f", min / 1_000_000D), String.format("%.4f", max / 1_000_000D), String.format("%.4f", average / 1_000_000D), String.format("%.4f", median / 1_000_000D));
	}

	public static void print() {
		List<Times> times = new ArrayList<>(map.values());
		times.sort(Comparator.comparing(Times::name));
		times.forEach(System.out::println);
	}
}