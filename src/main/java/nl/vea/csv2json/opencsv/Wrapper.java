package nl.vea.csv2json.opencsv;

import com.opencsv.CSVReader;

import java.util.stream.Stream;

record Wrapper(CSVReader csvReader, Stream<String[]> rows) {

}
