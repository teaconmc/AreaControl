package org.teacon.areacontrol.test.impl;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.teacon.areacontrol.impl.ChunkPosRange;

import java.util.ArrayList;
import java.util.List;

public class ChunkPosRangeTest {

    @Test
    public void testChunkRange() {
        final ChunkPos start = new ChunkPos(-1, -1);
        final ChunkPos end = new ChunkPos(1, 1);
        List<ChunkPos> expected = new ArrayList<>(9);
        for (ChunkPos pos : ChunkPosRange.of(start, end)) {
            expected.add(pos);
        }
        Assertions.assertEquals(9, expected.size());
        Assertions.assertTrue(expected.contains(new ChunkPos(-1, -1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(-1, 0)));
        Assertions.assertTrue(expected.contains(new ChunkPos(-1, 1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(0, -1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(0, 0)));
        Assertions.assertTrue(expected.contains(new ChunkPos(0, 1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(1, -1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(1, 0)));
        Assertions.assertTrue(expected.contains(new ChunkPos(1, 1)));
    }

    @Test
    public void testChunkRangeInverse() {
        final ChunkPos start = new ChunkPos(1, 1);
        final ChunkPos end = new ChunkPos(-1, -1);
        List<ChunkPos> expected = new ArrayList<>(9);
        for (ChunkPos pos : ChunkPosRange.of(start, end)) {
            expected.add(pos);
        }
        Assertions.assertEquals(9, expected.size());
        Assertions.assertTrue(expected.contains(new ChunkPos(-1, -1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(-1, 0)));
        Assertions.assertTrue(expected.contains(new ChunkPos(-1, 1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(0, -1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(0, 0)));
        Assertions.assertTrue(expected.contains(new ChunkPos(0, 1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(1, -1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(1, 0)));
        Assertions.assertTrue(expected.contains(new ChunkPos(1, 1)));
    }

    @Test
    public void testChunkRangeWithSameStartAndEnd() {
        List<ChunkPos> expected = new ArrayList<>(1);
        for (ChunkPos pos : ChunkPosRange.of(ChunkPos.ZERO, ChunkPos.ZERO)) {
            expected.add(pos);
        }
        Assertions.assertEquals(1, expected.size());
        Assertions.assertTrue(expected.contains(new ChunkPos(0, 0)));
    }

    @Test
    public void testLinerChunkRange1() {
        final ChunkPos start = new ChunkPos(-2, -2);
        final ChunkPos end = new ChunkPos(-2, 2);
        List<ChunkPos> expected = new ArrayList<>(5);
        for (ChunkPos pos : ChunkPosRange.of(start, end)) {
            expected.add(pos);
        }
        Assertions.assertEquals(5, expected.size());
        Assertions.assertTrue(expected.contains(new ChunkPos(-2, -2)));
        Assertions.assertTrue(expected.contains(new ChunkPos(-2, -1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(-2, 0)));
        Assertions.assertTrue(expected.contains(new ChunkPos(-2, 1)));
        Assertions.assertTrue(expected.contains(new ChunkPos(-2, 2)));
    }

    @Test
    public void testLinerChunkRange2() {
        final ChunkPos start = new ChunkPos(-2, -2);
        final ChunkPos end = new ChunkPos(2, -2);
        List<ChunkPos> expected = new ArrayList<>(5);
        for (ChunkPos pos : ChunkPosRange.of(start, end)) {
            expected.add(pos);
        }
        Assertions.assertEquals(5, expected.size());
        Assertions.assertTrue(expected.contains(new ChunkPos( -2, -2)));
        Assertions.assertTrue(expected.contains(new ChunkPos( -1, -2)));
        Assertions.assertTrue(expected.contains(new ChunkPos( 0, -2)));
        Assertions.assertTrue(expected.contains(new ChunkPos( 1, -2)));
        Assertions.assertTrue(expected.contains(new ChunkPos( 2, -2)));
    }
}
