package de.manuelclever.wav;

import java.util.Iterator;

public interface IterableByChannel<T> extends Iterable<T> {
    Iterator<T> iterator(int channel);
}
