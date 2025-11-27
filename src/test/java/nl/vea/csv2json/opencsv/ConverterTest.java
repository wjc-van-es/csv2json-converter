package nl.vea.csv2json.opencsv;

import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static nl.vea.csv2json.opencsv.Converter.read;
import static nl.vea.csv2json.opencsv.Converter.readAllLines;
import static org.junit.jupiter.api.Assertions.*;

public class ConverterTest {

    static final Path TEST_CSV;

    static {
        try {
            TEST_CSV = Paths.get(
                    ClassLoader.getSystemResource("Periodic_Table_Of_Elements.csv").toURI()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testReadAllLines() throws URISyntaxException, IOException, CsvException {
        try (BufferedReader bf = Files.newBufferedReader(TEST_CSV)) {
            List<String[]> result = readAllLines(bf, ',');
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(119, result.size());
            assertEquals(28, result.getFirst().length);
            assertEquals(28, result.getLast().length);
        }
    }


    @Test
    void testRead() throws IOException, CsvException {
        try (BufferedReader bf = Files.newBufferedReader(TEST_CSV)) {
            Wrapper result = read(bf, ',');
            assertNotNull(result);

            // assigning the stream reference in a try with resources will hopefully call its close() method
            //
            try(Stream<String[]> elements = result.records()) {
                final AtomicBoolean first = new AtomicBoolean(true);
                final AtomicReference<String[]> firstLine = new AtomicReference<>();
                var au = elements.peek(line -> {
                            if (first.get()) {
                                firstLine.set(line);

                                first.set(false);
                            }
                        })
                        .filter(line -> line[0].equals("79")) // does filter affect peek() as well?
                        .findFirst()
                        .orElseThrow();
                Arrays.stream(firstLine.get()).forEach(System.out::println);
                Arrays.stream(au).forEach(System.out::println);
                assertEquals(28, au.length);
                assertEquals("Gold", au[1]);
            }
        }
    }
}
