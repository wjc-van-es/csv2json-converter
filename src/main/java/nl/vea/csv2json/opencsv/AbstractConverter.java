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
