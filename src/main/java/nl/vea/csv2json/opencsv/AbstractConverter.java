package nl.vea.csv2json.opencsv;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterators.spliteratorUnknownSize;

public abstract class AbstractConverter implements AutoCloseable {
    protected final CSVReader csvReader;
    protected final Stream<String[]> rows;
    protected final BufferedWriter output;

    /**
     * Creates a {@link Wrapper} that contains a {@code Stream<String[]>} {@link Wrapper#rows()} and a {@link CSVReader} that feeds it,
     * based on its arguments
     * @param input provides the elements for the Stream
     * @param separator specifies the character used for separating fields
     * @return the wrapper whose rows Stream can be processed.
     */
    public AbstractConverter(BufferedReader input, BufferedWriter output, char separator) {
        this.output = output;
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(separator)
                .withIgnoreQuotations(true)
                .build();
        csvReader = new CSVReaderBuilder(input)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();
        Iterator<String[]> it = csvReader.iterator();

        // a wrapper containing Stream & a Reader that feeds it
        rows = StreamSupport.stream(spliteratorUnknownSize(it, 0), false)
                        // make sure to close the reader when closing the stream
                        .onClose(() -> {
                            try{
                                csvReader.close();
                                System.out.println("csvReader closed");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    public void convert(){
        final AtomicBoolean first = new AtomicBoolean(true);
        final AtomicReference<String[]> firstLine = new AtomicReference<>();
        rows.forEach(row -> {
            processRow();
        });
    }

    public abstract void processRow();

    @Override
    public void close() throws Exception {
        rows.close();
    }
}
