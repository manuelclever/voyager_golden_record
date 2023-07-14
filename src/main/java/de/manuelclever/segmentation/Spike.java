package de.manuelclever.segmentation;

public class Spike {
    long pos;
    float amplitude;

    public Spike(long pos, float amplitude) {
        this.pos = pos;
        this.amplitude = amplitude;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(pos);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() == this.getClass()) {
            return this.hashCode() == obj.hashCode();
        }
        return false;
    }

    @Override
    public String toString() {
        return "[" + pos + "," + amplitude + "]";
    }
}
