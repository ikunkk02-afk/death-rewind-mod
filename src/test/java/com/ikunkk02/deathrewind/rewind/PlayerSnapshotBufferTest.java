package com.ikunkk02.deathrewind.rewind;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSnapshotBufferTest {
	private record SampleSnapshot(long gameTime, String label, int generation) {
	}

	private static final int TEST_GEN = 0;

	@Test
	void keepsOnlyConfiguredCapacity() {
		PlayerSnapshotBuffer<SampleSnapshot> buffer = new PlayerSnapshotBuffer<>(3, SampleSnapshot::gameTime, SampleSnapshot::generation);

		buffer.add(new SampleSnapshot(20, "first", TEST_GEN));
		buffer.add(new SampleSnapshot(40, "second", TEST_GEN));
		buffer.add(new SampleSnapshot(60, "third", TEST_GEN));
		buffer.add(new SampleSnapshot(80, "fourth", TEST_GEN));

		assertEquals(3, buffer.size());
		assertEquals("second", buffer.oldest().orElseThrow().label());
	}

	@Test
	void returnsNewestSnapshotAtOrBeforeTargetTime() {
		PlayerSnapshotBuffer<SampleSnapshot> buffer = new PlayerSnapshotBuffer<>(5, SampleSnapshot::gameTime, SampleSnapshot::generation);

		buffer.add(new SampleSnapshot(20, "first", TEST_GEN));
		buffer.add(new SampleSnapshot(40, "second", TEST_GEN));
		buffer.add(new SampleSnapshot(60, "third", TEST_GEN));

		Optional<SampleSnapshot> snapshot = buffer.snapshotAtOrBefore(50, TEST_GEN);

		assertTrue(snapshot.isPresent());
		assertEquals("second", snapshot.orElseThrow().label());
	}

	@Test
	void filtersByGeneration() {
		PlayerSnapshotBuffer<SampleSnapshot> buffer = new PlayerSnapshotBuffer<>(5, SampleSnapshot::gameTime, SampleSnapshot::generation);

		buffer.add(new SampleSnapshot(20, "gen0_old", 0));
		buffer.add(new SampleSnapshot(40, "gen0_new", 0));
		buffer.add(new SampleSnapshot(20, "gen1_old", 1));
		buffer.add(new SampleSnapshot(40, "gen1_new", 1));

		Optional<SampleSnapshot> gen0 = buffer.snapshotAtOrBefore(50, 0);
		assertTrue(gen0.isPresent());
		assertEquals("gen0_new", gen0.orElseThrow().label());

		Optional<SampleSnapshot> gen1 = buffer.snapshotAtOrBefore(50, 1);
		assertTrue(gen1.isPresent());
		assertEquals("gen1_new", gen1.orElseThrow().label());

		Optional<SampleSnapshot> gen2 = buffer.snapshotAtOrBefore(50, 2);
		assertTrue(gen2.isEmpty());
	}
}
