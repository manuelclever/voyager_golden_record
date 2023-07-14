package de.manuelclever;

import java.io.File;
import java.nio.file.Path;

public class AnalyzeFile {

    public static void main(String[] args) {
        File voyager = new File(
                Path.of("src", "main", "resources", "voyager_golden_record_image_data_fixed_mono_left.wav").toString());

        Analyzer analyzer = new Analyzer(voyager);

        analyzer.analyze();
    }
}
