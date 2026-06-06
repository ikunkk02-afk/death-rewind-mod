package com.ikunkk02.deathrewind.rewind;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public final class PlayerSnapshotBuffer<T> {
	private final int capacity;
	private final ToLongFunction<T> gameTimeReader;
	private final ToIntFunction<T> generationReader;
	private final Deque<T> snapshots = new ArrayDeque<>();

	public PlayerSnapshotBuffer(int capacity, ToLongFunction<T> gameTimeReader, ToIntFunction<T> generationReader) {
		if (capacity < 1) {
			throw new IllegalArgumentException("capacity must be positive");
		}

		this.capacity = capacity;
		this.gameTimeReader = gameTimeReader;
		this.generationReader = generationReader;
	}

	public void add(T snapshot) {
		snapshots.addLast(snapshot);

		while (snapshots.size() > capacity) {
			snapshots.removeFirst();
		}
	}

	public Optional<T> oldest() {
		return Optional.ofNullable(snapshots.peekFirst());
	}

	public Optional<T> latest() {
		return Optional.ofNullable(snapshots.peekLast());
	}

	public Optional<T> snapshotAtOrBefore(long targetGameTime, int requiredGeneration) {
		T candidate = null;
		Iterator<T> iterator = snapshots.iterator();

		while (iterator.hasNext()) {
			T snapshot = iterator.next();

			if (generationReader.applyAsInt(snapshot) != requiredGeneration) {
				continue;
			}

			if (gameTimeReader.applyAsLong(snapshot) <= targetGameTime) {
				candidate = snapshot;
			} else {
				break;
			}
		}

		if (candidate != null) {
			return Optional.of(candidate);
		}

		return Optional.empty();
	}

	/**
	 * Finds the latest snapshot whose gameTime ≤ targetGameTime, across ALL generations.
	 * If no snapshot is old enough (e.g., player died before buffer filled enough),
	 * returns the oldest available snapshot as a best-effort fallback.
	 */
	public Optional<T> oldestAtOrBefore(long targetGameTime) {
		T candidate = null;
		for (T snapshot : snapshots) {
			if (gameTimeReader.applyAsLong(snapshot) <= targetGameTime) {
				candidate = snapshot;
			} else {
				break;
			}
		}
		if (candidate != null) {
			return Optional.of(candidate);
		}
		// Best-effort fallback: return the oldest snapshot we have
		return oldest();
	}

	public int size() {
		return snapshots.size();
	}

	public boolean isEmpty() {
		return snapshots.isEmpty();
	}

	public void clear() {
		snapshots.clear();
	}
}
