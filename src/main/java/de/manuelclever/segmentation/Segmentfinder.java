package de.manuelclever.segmentation;

import de.manuelclever.wav.WAV;
import de.manuelclever.wav.WAVIterator;

import java.io.EOFException;
import java.io.IOException;

public class Segmentfinder {
    // change fraction for different size of segments.
    private static final int FRACTION = 100;
    // if no opposite spike was found, moveIterToNext...() tries to search for a new sample which satisfies the
    // amplitude boundary. Set frames until method ends with a false return
    private static final float AMPLITUDE_BOUNDARY = 0.1f;
    private static final int MAX_INTERMEDIATE_FRAMES_UNTIL_NEXT_BOUNDARY = 500;

    private final WAV wav;
    private final WAVIterator iter;
    private final int standardFramesPerSegment;
    private final int shrinkByFrames;

    private boolean lastSpikeWasNeg;

    public Segmentfinder(WAV wav, WAVIterator iter) {
        this.wav = wav;
        this.iter = iter;
        this.standardFramesPerSegment = wav.getSampleRate() / FRACTION;
        this.shrinkByFrames = standardFramesPerSegment / (FRACTION * 3);
    }

    public Frequency getNextSegment() throws IOException {

        while(iter.hasNext()) {
            short sample = iter.next();
            float amplitude = WAV.calculateAmplitude(sample);

//            System.out.println("trying to find next amplitude " + iter.getIndex());
            // check if next Frequency starts with high or low and if high enough to be worth calculating
            if (amplitude > AMPLITUDE_BOUNDARY || amplitude < -AMPLITUDE_BOUNDARY) {
                lastSpikeWasNeg = !(amplitude < -AMPLITUDE_BOUNDARY); // if first sample has negative amplitude, set
                // boolean false

//                System.out.println("generate segment at " + iter.getIndex());
                return generateSegment(standardFramesPerSegment, null);
            }
        }
        throw new EOFException();
    }

    private Frequency generateSegment(int framesPerSegment, Frequency frequency) throws IOException {
        iter.mark0();
        Frequency nextFrequency;
        if(frequency == null) {
            nextFrequency = getAllSpikes(framesPerSegment);
        } else {
//            iter.reset1();
            nextFrequency = getAllSpikes(framesPerSegment, frequency.getEndSpike());
        }

        if(nextFrequency == null) {
            if (framesPerSegment > shrinkByFrames * 2) {
                // reset iter to beginning of new segment and shrink frames for next segment
                iter.reset0();
//                System.out.println("\t (null) shrinking jumping back to " + iter.getIndex());
//                System.out.println("\t\t" + framesPerSegment + " - " + shrinkByFrames + " = " + (framesPerSegment - shrinkByFrames));
                return generateSegment(framesPerSegment - shrinkByFrames, frequency);

                // there is no frequency yet, jump to next
            } else if (frequency == null) {
                iter.jump(framesPerSegment);
                return generateSegment(framesPerSegment, null);

                // the frequency doesn't expand further. End the segment
            } else {
                iter.reset0(); // set index to one before next peak
//                System.out.println("end segment: " + frequency.getStartSpike() + " - " + frequency.getEndSpike() +
//                        " -> " + frequency.getPeaks() + " -> " + frequency.getHz() + "(null)");
                return frequency;
            }
        }

        nextFrequency.calculateFrequency(wav.getSampleRate());

        if (frequency == null) {
            frequency = nextFrequency;
//            System.out.println("frequency: " + frequency);
            return generateSegment(framesPerSegment, frequency);
        }

        // combine same frequencies
        if (frequency.combineWithFrequency(nextFrequency)) {
            System.out.println("\tcombining at " + nextFrequency.getEndSpike());
            // reset sample size
            return generateSegment(standardFramesPerSegment, frequency);

            // couldn't combine frequencies, take smaller samples
        } else if (framesPerSegment > shrinkByFrames * 2) {
            // reset iter to beginning of new segment and shrink frames for next segment
            iter.reset0();
//            System.out.println("\tshrinking jumping back to " + iter.getIndex());
//            System.out.println("\t\t" + framesPerSegment + " - " + shrinkByFrames + " = " + (framesPerSegment - shrinkByFrames));
            return generateSegment(framesPerSegment - shrinkByFrames, frequency);

            // the frequency doesn't expand further. End the segment
        } else {
            iter.reset0(); // set index to one before next peak
//            System.out.println("end segment: " + frequency.getStartSpike() + " - " + frequency.getEndSpike() +
//                    " -> " + frequency.getPeaks() + " -> " + frequency.getHz());
            return frequency;
        }

    }

    private Frequency getAllSpikes(int withinFrames) {
        Spike spike;
        try {
            if (lastSpikeWasNeg) {
                spike = getSpikePos();
                lastSpikeWasNeg = false;
            } else {
                spike = getSpikeNeg();
                lastSpikeWasNeg = true;
            }
        } catch (Exception e) {
            return null;
        }
        return getAllSpikes(withinFrames, spike);
    }

    private Frequency getAllSpikes(int withinFrames, Spike lastSpike) {
        Frequency spikes = new Frequency(lastSpike);
        lastSpikeWasNeg = lastSpike.amplitude < 0;
        long startSample = iter.getIndex();

        for(long i = 0; i < withinFrames;) {

            try {
                if (lastSpikeWasNeg) {
                    getSpikePos();
                    spikes.setEndSpike(getSpikeNeg());

//                    lastSpikeWasNeg = true;

                } else {
                    getSpikeNeg();
                    spikes.setEndSpike(getSpikePos());

//                    lastSpikeWasNeg = false;
                }
                spikes.addPeak();
            } catch (Exception e) {
                return null;
            }

//            if(tempSpike == -1 || spikes[2] == -1) {
//                break;
//            }

            i = iter.getIndex() - startSample;
        }
//        iter.setLastIndex(spikes[2] - 1);
//        lastSpikeWasNeg = !lastSpikeWasNeg; // flipping boolean, because next spike sequence starts with spike[2]
//        System.out.println("\tspikes: " + spikes);
        return spikes;
    }

    private Spike getSpikePos() throws Exception {
        return getSpikePos(Short.MIN_VALUE, iter.getIndex());
    }

    private Spike getSpikePos(short startSample, long index) throws Exception {
        short spike = startSample;
        Spike lastSpike = new Spike(index, 0);

        if(iter.hasNext()) {
            short sample = iter.next();
            float amplitude = WAV.calculateAmplitude(sample);

            if(amplitude > AMPLITUDE_BOUNDARY) {
                while (sample > 0) { //iter.hasNext() &&

                    if (spike < sample) {
                        spike = sample;
                        lastSpike.pos = iter.getIndex();
                        lastSpike.amplitude = amplitude;
//                        iter.mark1();
                    }

                    if (iter.hasNext()) {
                        sample = iter.next();
                    } else {
                        break;
                    }
                }
            } else {
                short nextSample = getNextPositiveBigEnough();
                if(nextSample != -1) {
                    return getSpikePos(nextSample, iter.getIndex());
                } else {
                    throw new Exception("No new spike could be found.");
                }
            }
            return lastSpike;
        }
        throw new EOFException();
    }

    private Spike getSpikeNeg() throws Exception {
        return getSpikeNeg(Short.MAX_VALUE, iter.getIndex());
    }

    private Spike getSpikeNeg(short startSample, long index) throws Exception {
        short spike = startSample;
        Spike lastSpike = new Spike(index, 0 );

        if(iter.hasNext()) {
            short sample = iter.next();
            float amplitude = WAV.calculateAmplitude(sample);

            if(amplitude < -AMPLITUDE_BOUNDARY) {

                while (sample < 0) {
                    if (spike > sample) {
                        spike = sample;
                        lastSpike.pos = iter.getIndex();
                        lastSpike.amplitude = amplitude;
//                        iter.mark1();
                    }
                    if (iter.hasNext()) {
                        sample = iter.next();
                    } else {
                        break;
                    }
                }
            } else {
                short nextSample = getNextNegativeBigEnough();
                if(nextSample != -1) {
                    return getSpikeNeg(nextSample, iter.getIndex());
                } else {
                    throw new Exception("No new spike could be found.");
                }
            }
            return lastSpike;
        }
        throw new EOFException();
    }

    private short getNextPositiveBigEnough()  {
        short sample = iter.next();
        float amplitude = WAV.calculateAmplitude(sample);

        for(int i = 0; i < MAX_INTERMEDIATE_FRAMES_UNTIL_NEXT_BOUNDARY; i++) {
            if(amplitude > AMPLITUDE_BOUNDARY) {
                return sample;
            } else {
                sample = iter.next();
                amplitude = WAV.calculateAmplitude(sample);
            }
        }
        return -1;
    }

    private short getNextNegativeBigEnough()  {
        short sample = iter.next();
        float amplitude = WAV.calculateAmplitude(sample);

        for(int i = 0; i < MAX_INTERMEDIATE_FRAMES_UNTIL_NEXT_BOUNDARY; i++) {

            if (amplitude < -AMPLITUDE_BOUNDARY) {
                return sample;
            } else {
                sample = iter.next();
                amplitude = WAV.calculateAmplitude(sample);
            }

        }
        return -1;
    }
}
