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

import static nl.vea.csv2json.opencsv.Converter.*;
import static org.junit.jupiter.api.Assertions.*;

public class ConverterTest {

    static final Path TEST_CSV;
    static final Path TEST_OUTPUT;

    static {
        try {
            TEST_CSV = Paths.get(
                    ClassLoader.getSystemResource("Periodic_Table_Of_Elements.csv").toURI()
            );
            TEST_OUTPUT = Paths.get("out", TEST_CSV.getFileName().toString().split("\\.")[0] + ".json");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testConvert() throws IOException {
        // convert is lightweight depending on processing each row as an element in a Stream
        // when processing the last row we shouldn't add a comma for the corresponding last element of the json array
        // the only reliable way is to go over the csv twice: once to count its rows and once to actually convert
        // to json with the established row count
        long size = countRows(TEST_CSV, ',');
        convert(TEST_CSV, TEST_OUTPUT, ',', size);
    }

    @Test
    void testSetupAllLines() throws IOException, CsvException {
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
    void testSetup() throws IOException {
        try (BufferedReader bf = Files.newBufferedReader(TEST_CSV)) {
            Wrapper result = setup(bf, ',');
            assertNotNull(result);

            // assigning the stream reference in a try with resources will hopefully call its close() method
            //
            try(Stream<String[]> elements = result.rows()) {
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
