package org.teacon.areacontrol.impl;

import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public final class ChunkPosRange implements Iterable<ChunkPos> {

    public static ChunkPosRange of(ChunkPos start, ChunkPos end) {
        return new ChunkPosRange(start, end);
    }

    private final ChunkPos start;
    private final ChunkPos end;

    public ChunkPosRange(ChunkPos start, ChunkPos end) {
        this.start = start;
        this.end = end;
    }

    @NotNull
    @Override
    public Iterator<ChunkPos> iterator() {
        final int deltaX = this.start.x() < this.end.x() ? 1 : -1;
        final int deltaZ = this.start.z() < this.end.z() ? 1 : -1;
        return new Iterator<>() {
            private ChunkPos current = null;
            @Override
            public boolean hasNext() {
                return this.current == null || this.current.x() != end.x() || this.current.z() != end.z();
            }

            @Override
            public ChunkPos next() {
                if (this.current == null) {
                    return this.current = start;
                }
                if (this.current.x() == end.x()) {
                    if (this.current.z() == end.z()) {
                        return null;
                    }
                    return this.current = new ChunkPos(start.x(), this.current.z() + deltaZ);
                } else {
                    return this.current = new ChunkPos(this.current.x() + deltaX, this.current.z());
                }
            }
        };
    }
}
