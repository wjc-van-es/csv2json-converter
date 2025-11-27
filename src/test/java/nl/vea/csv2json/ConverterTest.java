package nl.vea.csv2json;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.vea.csv2json.Converter.convert;

public class ConverterTest {

    @Test
    void test() throws IOException {
        Path input = Paths.get("src/test/resources/Periodic_Table_Of_Elements.csv");
        Path output = Paths.get("out", input.getFileName() + ".json");
        convert(input, output, ",");
    }
}
