package de.manuelclever;

import de.manuelclever.segmentation.Frequency;
import de.manuelclever.segmentation.Segmentfinder;
import de.manuelclever.wav.WAV;
import de.manuelclever.wav.WAVIterator;

import javax.naming.LimitExceededException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Analyzer {
    File file;

    public Analyzer(File file) {
        this.file = file;
    }

    public List<Frequency> analyze() {

        try {
            WAV wav = new WAV(file);

            System.out.println();
            System.out.println(wav);
            System.out.println();

            List<Frequency> frequencies = findSegments(wav);

            System.out.println("Frequencies found:");
            frequencies.forEach(System.out::println);
            return frequencies;
        } catch (LimitExceededException e) {
            System.out.println(file.getName() + e.getMessage());
        }
        return null;
    }

    private List<Frequency> findSegments(WAV wav) {
        WAVIterator iter = (WAVIterator) wav.iterator(1);
        Segmentfinder segmentfinder = new Segmentfinder(wav, iter);

        List<Frequency> frequencies = new ArrayList<>();
        while(iter.hasNext()) {
            try {
                Frequency segment = segmentfinder.getNextSegment();
                frequencies.add(segment);
            } catch (IOException e) {
                break;
            }
        }

        return frequencies;
    }

}
