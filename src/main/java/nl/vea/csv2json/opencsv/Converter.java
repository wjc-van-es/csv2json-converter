package nl.vea.csv2json.opencsv;


import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Spliterators.spliteratorUnknownSize;

public class Converter {
    private static final String PREFIX = """
            {
                "title" : "periodic table of elements",
                "elements" : [
                
            """;
    private static final String SUFFIX = """
                ]
            }
            """;
    private static final String FIELD_TEMPLATE = "\t\t\t\"%s\" : \"%s\"";

    public static void convert(Path input, Path output, char separator, long precalculatedRowSize) throws IOException {
        try(var reader = Files.newBufferedReader(input);
            var writer = Files.newBufferedWriter(output)){
            convert(reader, writer, separator, precalculatedRowSize);
        }
    }
    public static long countRows(Path input, char separator) throws IOException {
        try(var reader = Files.newBufferedReader(input)){
            return countRows(reader, separator);
        }
    }

    public static long countRows(BufferedReader input, char separator) throws IOException {
        long rowCount = 0;
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(separator)
                //.withEscapeChar('"')
                .withQuoteChar('"')
                .withStrictQuotes(false) // fields will only have quotes to escape the separator
                //.withIgnoreQuotations(false)
                .build();
        try (CSVReader csvReader = new CSVReaderBuilder(input)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build()) {
            var it = csvReader.iterator();
            while (it.hasNext()) {
                it.next();
                rowCount++;
            }
            return rowCount;
        }
    }

    public static void convert(BufferedReader input,
                               BufferedWriter output,
                               char separator,
                               long precalculatedRowSize) throws IOException {
        output.write(PREFIX);
        Wrapper wrapper = setup(input, separator);
        final AtomicBoolean first = new AtomicBoolean(true);
        final AtomicReference<String[]> firstLine = new AtomicReference<>();
        final AtomicLong count = new AtomicLong(0L);
        wrapper.rows().forEach(row -> {
            long currentRow = count.incrementAndGet();
            if (first.get()) {
                firstLine.set(row);
                first.set(false);
            } else {
                procesRow(firstLine, row, output, currentRow == precalculatedRowSize);
            }
        });
        output.write(SUFFIX);
    }

    private static void procesRow(final AtomicReference<String[]> firstLine,
                                  String[] row,
                                  BufferedWriter output,
                                  boolean isLastRow) {
        String[] headers = firstLine.get();
        if (headers.length != row.length) {
            System.err.printf("We got a row with length %s, which will be skipped", row.length);
            System.err.printf("Its content %s", Arrays.stream(row).collect(Collectors.joining(",")));
           // throw new IllegalStateException("Each row must contain the same number of fields as the header");
            // or maybe add an argument to indicate more leniency by just skipping the row and log a warning
        } else {
            try {
                output.write("\t\t{\n");
                for (int i = 0; i < headers.length; i++) {
                    output.write(FIELD_TEMPLATE.formatted(headers[i], row[i]));
                    if (i < headers.length - 1) {
                        output.write(",\n"); //skip comma
                    } else {
                        output.write("\n");
                    }
                }
                output.write("\t\t}");
                if(isLastRow){
                    output.write("\n"); // no comma
                } else {
                    output.write(",\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static List<String[]> readAllLines(BufferedReader input, char separator) throws IOException, CsvException {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(separator)
                //.withEscapeChar('"')
                .withQuoteChar('"')
                .withStrictQuotes(false) // fields will only have quotes to escape the separator
                //.withIgnoreQuotations(false)
                .build();
        try (CSVReader csvReader = new CSVReaderBuilder(input)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build()) {
            return csvReader.readAll();
        }
    }

    /**
     * Creates a {@link Wrapper} that contains a {@code Stream<String[]>} {@link Wrapper#rows()} and a {@link CSVReader} that feeds it,
     * based on its arguments
     *
     * @param input     provides the elements for the Stream
     * @param separator specifies the character used for separating fields
     * @return the wrapper whose rows Stream can be processed.
     */
    public static Wrapper setup(BufferedReader input, char separator) {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(separator)
                .withQuoteChar('"')
                .withStrictQuotes(false)
                .withIgnoreQuotations(false)
                .build();
        CSVReader csvReader = new CSVReaderBuilder(input)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();
        Iterator<String[]> it = csvReader.iterator();

        // a wrapper containing Stream & a Reader that feeds it
        return new Wrapper(csvReader,
                StreamSupport.stream(spliteratorUnknownSize(it, 0), false)
                        // make sure to close the reader when closing the stream
                        .onClose(() -> {
                            try {
                                csvReader.close();
                                System.out.println("csvReader closed");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
    }
}
