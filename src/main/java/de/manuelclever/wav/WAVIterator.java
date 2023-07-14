package de.manuelclever.wav;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;

public class WAVIterator implements Iterator<Short> {
    // scuffed iterator with methods no iterator should have

    private final WAV wav;
    private final BufferedInputStream datastream;
    private final int channel;
    private long index;
    private int indexSinceMark0;
    private int indexSinceMark1;
    private final long EOF;

    public WAVIterator(WAV wav, int channel) throws IOException {
        this.wav = wav;
        this.datastream = wav.getDataStream();
        this.channel = channel;
        this.index = -1;
        this.EOF = wav.getSampleLengthOfData();
    }

    public long getIndex() {
        return index;
    }

    public void mark0() {
        datastream.mark(wav.getSampleRate());
//        System.out.println("\u001B[31m" + "\t" + index + "\u001B[0m");
        indexSinceMark0 = 0;
        indexSinceMark1 = -1;
    }

//    /**
//     * Custom marker after {@link #mark0()}. Set marker accordingly to numeration. Only works after {@link #mark0()}
//     * has been set and only if not further away from {@link #mark0()} than sampleRate of wav.
//     */
//    public void mark1() {
////        System.out.println("\u001B[31m" + "\t" + index + "\u001B[0m");
//        indexSinceMark1 = 0;
//    }

    public boolean reset0() {
        try {
            datastream.reset();
            index -= indexSinceMark0;
            indexSinceMark0 = 0;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

//    /**
//     * Reset {@link #mark1()} to ONE BEFORE last position.
//     */
//    public boolean reset1() {
//        try {
//            datastream.reset();
//            index -= indexSinceMark0;
//            indexSinceMark0 -= indexSinceMark1;
//            datastream.readNBytes(indexSinceMark0 - 1);
//            indexSinceMark1 = 0;
//            return true;
//        } catch (IOException e) {
//            return false;
//        }
//    }

    public void jump(int samples) throws IOException {
        if(index + samples < EOF) {
            datastream.readNBytes(samples);
            index += samples;
            mark0();
        } else {
            throw new EOFException();
        }
    }

    @Override
    public boolean hasNext() {
        return index + 1< EOF - 1;
    }

    @Override
    public Short next() {
        try {
//            if(index >= 19677070) {
//                System.out.print("-" + index);
//            }
            short sample =  wav.getNextDataSample(datastream, channel);
            index++;
            indexSinceMark0++;
            return sample;
        } catch (IOException e) {
            return null;
        }
    }
}
