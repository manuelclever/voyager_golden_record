package de.manuelclever.wav;

import javax.naming.LimitExceededException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

public class WAV implements IterableByChannel<Short> {
    // built my own (scuffed) WAV decoder, because I didn't like/ couldn't handle the sound api decoder (AudioFormat)

    private final static int DATA_START = 44;

    File file;
    String fileName;
    BufferedInputStream inputStream;

    private String chunkID;
    private long chunkSize;
    private String format;

    private String subchunk1ID;
    private long subchunk1Size;
    private int audioFormat;
    private int numChannels;
    private int sampleRate;
    private int byteRate;
    private int blockAlign;
    private int bitsPerSample;

    private String subchunk2ID;
    private long subchunk2Size;
//    private short[] data;

    public WAV(File file) throws LimitExceededException {
        try {
            this.file = file;
            this.fileName = file.getName();
            this.inputStream = new BufferedInputStream(new FileInputStream(file));
            System.out.println("building RIFF Chunk...");
            buildRIFFChunk();
            System.out.println("building FMT Chunk...");
            buildFMTSubchunk();
            System.out.println("building Data Chunk...");
            buildDataSubchunk();
            System.out.println("WAV was build successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildRIFFChunk() throws IOException {
        chunkID = buildASCII(readBytes(4));
        chunkSize = buildLong(littleEndian(readBytes(4)));
        format = buildASCII(readBytes(4));
    }

    private void buildFMTSubchunk() throws IOException {
        subchunk1ID = buildASCII(readBytes(4));
        subchunk1Size = buildLong(littleEndian(readBytes(4)));
        audioFormat = buildInteger(littleEndian(readBytes(2)));
        numChannels = buildInteger(littleEndian(readBytes(2)));
        sampleRate = buildInteger(littleEndian(readBytes(4)));
        byteRate = buildInteger(littleEndian(readBytes(4)));
        blockAlign = buildInteger(littleEndian(readBytes(2)));
        bitsPerSample = buildInteger(littleEndian(readBytes(2)));
    }

    private void buildDataSubchunk() throws IOException {
        subchunk2ID = buildASCII(readBytes(4));
        subchunk2Size = buildLong(littleEndian(readBytes(4)));
    }

    public short getNextDataSample(BufferedInputStream datastream, int channel) throws IOException {
        short sample;

        if(channel == 1) {
            sample = buildShort(littleEndian(datastream.readNBytes(blockAlign)));
            datastream.readNBytes((numChannels-1) * blockAlign);
        } else {
            datastream.readNBytes((channel-1) * blockAlign);
            sample = buildShort(littleEndian(datastream.readNBytes(blockAlign)));
            datastream.readNBytes((numChannels - channel) * blockAlign);
        }
        return sample;
    }

    private short getDataForChannel(int i, byte[] sample) {
        int bytesPerChannel = blockAlign / numChannels;
        byte[] channelSample = new byte[bytesPerChannel];

        int index = 0;
        for(int j = i * bytesPerChannel; j < (i * bytesPerChannel) + bytesPerChannel; j++) {
            channelSample[index++] = sample[j];
        }
        return buildShort(littleEndian(channelSample));
    }

    private boolean checkAudioSize() {
        return subchunk2Size < Integer.MAX_VALUE;
    }

    // ------ Utils ------ //

    private byte[] readBytes(int len) throws IOException {
        return inputStream.readNBytes(len);
    }

    private byte[] littleEndian(byte[] bytes) {
        byte[] turned = new byte[bytes.length];

        for(int i = 0; i < bytes.length; i++) {
            turned[bytes.length - 1 - i] = bytes[i];
        }
        return turned;
    }

    private String buildASCII(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for(byte b : arr) {
            sb.append((char) Byte.toUnsignedInt(b));
        }
        return sb.toString();
    }

    private int buildInteger(byte[] arr) {
        StringBuilder sb = new StringBuilder();

        for(byte b : arr) {
            String hex = Integer.toHexString(Byte.toUnsignedInt(b));

            if(hex.length() < 2) {
                sb.append(0).append(hex);
            } else {
                sb.append(hex);
            }
        }
        return Integer.parseInt(sb.toString(), 16);
    }

    private short buildShort(byte[] arr) {
        StringBuilder sb = new StringBuilder();

        for(byte b : arr) {
            String hex = Integer.toHexString(Byte.toUnsignedInt(b));

            if(hex.length() < 2) {
                sb.append(0).append(hex);
            } else {
                sb.append(hex);
            }
        }
        return (short) Integer.parseInt(sb.toString(),16);
    }

    private long buildLong(byte[] arr) {
        StringBuilder sb = new StringBuilder("0X");

        for(byte i : arr) {
            String hex = Integer.toHexString(Byte.toUnsignedInt(i));

            if(hex.length() < 2) {
                sb.append(0).append(hex);
            } else {
                sb.append(hex);
            }
        }
        return Long.decode(sb.toString());
    }

    // ------ Getter ------ //


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getChunkID() {
        return chunkID;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public String getFormat() {
        return format;
    }

    public String getSubchunk1ID() {
        return subchunk1ID;
    }

    public long getSubchunk1Size() {
        return subchunk1Size;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getByteRate() {
        return byteRate;
    }

    public int getBlockAlign() {
        return blockAlign;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public String getSubchunk2ID() {
        return subchunk2ID;
    }

    public long getSubchunk2Size() {
        return subchunk2Size;
    }

    public long getSampleLengthOfData() {
        return subchunk2Size / blockAlign;
    }

//    public short[] getData() {
//        return data;
//    }
//
//    public short getData(int pos) {
//        return data[pos];
//    }


    public BufferedInputStream getDataStream() throws IOException {
        BufferedInputStream datastream = new BufferedInputStream(new FileInputStream(file));
        datastream.readNBytes(DATA_START);
        datastream.mark(sampleRate * numChannels);
        return datastream;
    }

    public static float calculateAmplitude(short value) {
        if(value > 0) {
            return (float) value / Short.MAX_VALUE;
        } else if(value < 0) {
            return (float) value / (-1 * Short.MIN_VALUE);
        } else {
            return 0;
        }
    }

    @Override
    public Iterator<Short> iterator(int channel) {
        try {
            return new WAVIterator(this, channel);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Don't use this method! Use iterator(int channel) instead.
    */
    @Override
    public Iterator<Short> iterator() {
        return null;
    }

    @Override
    public String toString() {
        return "file:\t\t" + fileName + "\n\n" +

                "chunkID:\t\t" + chunkID + "\n" +
                "chunkSize:\t\t" + chunkSize + "\n" +
                "format:\t\t\t" + format + "\n\n" +

                "subchunk1ID:\t" + subchunk1ID + "\n" +
                "subchunk1Size:\t" + subchunk1Size + "\n" +
                "audioFormat:\t" + audioFormat + "\n" +
                "channels:\t\t" + numChannels + "\n" +
                "sampleRate:\t\t" + sampleRate + "\n" +
                "byteRate:\t\t" + byteRate + "\n" +
                "blockAlign:\t\t" + blockAlign + "\n" +
                "bitsPerSample:\t" + bitsPerSample + "\n\n" +

                "subchunk2ID:\t" + subchunk2ID + "\n" +
                "subchunk2Size:\t" + subchunk2Size;
    }
}
