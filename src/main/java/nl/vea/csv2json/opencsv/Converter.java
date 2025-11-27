package nl.vea.csv2json.opencsv;


import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.exceptions.CsvException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterators.spliteratorUnknownSize;

public class Converter {

    public static void convert() {
        HeaderColumnNameMappingStrategy strat = new HeaderColumnNameMappingStrategy();
    }

    public static void convert(Path input, Path output, String separator) throws IOException {
        convert(Files.newBufferedReader(input), Files.newBufferedWriter(output), separator);
    }

    public static void convert(BufferedReader input, BufferedWriter output, String separator) {

    }

    public static List<String[]> readAllLines(BufferedReader input, char separator) throws IOException, CsvException {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(separator)
                .withIgnoreQuotations(true)
                .build();
        try (CSVReader csvReader = new CSVReaderBuilder(input)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build()) {
            return csvReader.readAll();
        }
    }

    public static Wrapper read(BufferedReader input, char separator) throws IOException, CsvException {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(true)
                .build();
        CSVReader csvReader = new CSVReaderBuilder(input)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();
        Iterator<String[]> it = csvReader.iterator();
        return new Wrapper(csvReader,
                StreamSupport.stream(spliteratorUnknownSize(it, 0), false)
                        .onClose(() -> {
                            try{
                                csvReader.close();
                                System.out.println("csvReader closed");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        }));
    }
}
