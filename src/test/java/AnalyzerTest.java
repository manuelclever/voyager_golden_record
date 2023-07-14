import de.manuelclever.Analyzer;
import de.manuelclever.segmentation.Frequency;
import de.manuelclever.segmentation.Spike;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AnalyzerTest {
    private static List<File> files;

    @BeforeAll
    private static void initialize() {
        files = new ArrayList<>();
        String path = Path.of("src", "test", "resources").toString();

        files.add(new File(path + File.separator + "test1.wav"));
    }

    @ParameterizedTest
    @MethodSource("validFrequencies")
    public void frequencies(Map.Entry<File, List<Frequency>> entry) {
        File file = entry.getKey();
        List<Frequency> expectedFrequencies = entry.getValue();

        Analyzer analyzer = new Analyzer(file);
        List<Frequency> frequencies = analyzer.analyze();

        Assertions.assertEquals(expectedFrequencies, frequencies);
    }

    public static Stream<Map.Entry<File, List<Frequency>>> validFrequencies() {
        Map<File, List<Frequency>> map = new HashMap<>();

        List<Frequency> test1 = new ArrayList<>();
        test1.add(new Frequency(50, new Spike(221,0), new Spike(8157, 0), 9));
        test1.add(new Frequency(98, new Spike(8600,0), new Spike(17529, 0), 20));
        test1.add(new Frequency(50, new Spike(17860,0), new Spike(25799, 0), 9));
        map.put(files.get(0), test1);

        return map.entrySet().stream();

    }


}
