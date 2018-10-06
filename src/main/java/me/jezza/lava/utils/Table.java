package me.jezza.lava.utils;

import java.util.HashMap;
import java.util.Map;

import me.jezza.lava.Strings;

/**
 * @author Jezza
 */
public final class Table<K, V> {
	private final Map<K, V> map;
	
	private Table<K, V> fallback;

	public Table() {
		map = new HashMap<>();
	}

	public Table(int cap) {
		map = new HashMap<>(cap);
	}

//	public Table<K, V> fallback() {
//		return fallback;
//	}

	public Table<K, V> fallback(Table<K, V> value) {
		this.fallback = value;
		return this;
	}
	
	public V get(K key) {
		V value = map.get(key);
		return value == null && fallback != null
				? fallback.get(key)
				: value;
	}

	public V get(K key, V defaultValue) {
		V v = get(key);
		return v != null
				? v
				: defaultValue;
	}
	
	public V set(K key, V value) {
		return map.put(key, value);
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		if (fallback == null) {
			return Strings.format("Table{}",
					map);
		}
		return Strings.format("Table{map={}, fallback={}}",
				map,
				fallback);
	}
}
