package de.manuelclever.segmentation;

public class Frequency {
    private int hz;
    private final Spike startSpike;
    private Spike endSpike;
    private int peaks;

    private final static double VARIANCE = 0.15; //variance in percent between the frequencies to allow combining

    public Frequency(Spike startSpike) {
        this.startSpike = startSpike;
    }

    public Frequency(int hz, Spike startSpike, Spike endSpike, int peaks) {
        this.hz = hz;
        this.startSpike = startSpike;
        this.endSpike = endSpike;
        this.peaks = peaks;
    }

    public int getHz() {
        return hz;
    }

    public Spike getStartSpike() {
        return startSpike;
    }

    public Spike getEndSpike() {
        return endSpike;
    }

    public void setEndSpike(Spike endSpike) {
        this.endSpike = endSpike;
    }

    public int getPeaks() {
        return peaks;
    }

    public void addPeak() {
        this.peaks += 1;
    }

    public boolean combineWithFrequency(Frequency otherFrequency) {
        if(frequencyVarianceAllowed(otherFrequency.hz) && (this.endSpike == otherFrequency.startSpike)) {
            System.out.println("\t\ttrue: " + this + " with " + otherFrequency);
            long thisSample = this.endSpike.pos - this.startSpike.pos;
            long otherSample = otherFrequency.endSpike.pos - otherFrequency.startSpike.pos;

            float thisPercentage = (float) thisSample / (thisSample + otherSample);
            float otherPercentage = (float) otherSample / (thisSample + otherSample);

            //calculates new frequency
            this.hz = Math.round(this.hz * thisPercentage + otherFrequency.hz * otherPercentage);
            this.endSpike = otherFrequency.endSpike;
            this.peaks += otherFrequency.peaks;

            return true;
        }
        return false;
    }

    private boolean frequencyVarianceAllowed(int hz) {
        if(this.hz == hz) {
            return true;
        }
        return ( ( ((float) Math.min(this.hz,hz)) / ((float) Math.max(this.hz,hz)) ) ) > (1 - VARIANCE);
    }

    public void calculateFrequency(int sampleRate) {
        //sampleRate / ((spikeEndSample - spikeStartSample) / (SpikePairOfThrees) ) )
//        System.out.println("\t\tcalculating frequency: " + sampleRate + " / ( ( " + spikes[2] + " - " + spikes[1] +
//                " / " + spikes[0] + " ) )" );
        this.hz =  (int) (sampleRate / ((endSpike.pos - startSpike.pos) / peaks ) );
    }

    @Override
    public int hashCode() {
        return (hz * 7) + startSpike.hashCode() + endSpike.hashCode() + (peaks * 18);
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
        return "[" + hz + ", " + startSpike + ", " + endSpike + ", " + peaks + "]";
    }
}
