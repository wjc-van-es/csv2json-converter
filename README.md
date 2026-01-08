<style>
body {
  font-family: "Spectral", "Gentium Basic", Cardo , "Linux Libertine o", "Palatino Linotype", Cambria, serif;
  font-size: 100% !important;
  padding-right: 12%;
}
code {
  padding: 0.25em;
	
  white-space: pre;
  font-family: "Tlwg mono", Consolas, "Liberation Mono", Menlo, Courier, monospace;
	
  background-color: #ECFFFA;
  //border: 1px solid #ccc;
  //border-radius: 3px;
}

kbd {
  display: inline-block;
  padding: 3px 5px;
  font-family: "Tlwg mono", Consolas, "Liberation Mono", Menlo, Courier, monospace;
  line-height: 10px;
  color: #555;
  vertical-align: middle;
  background-color: #ECFFFA;
  border: solid 1px #ccc;
  border-bottom-color: #bbb;
  border-radius: 3px;
  box-shadow: inset 0 -1px 0 #bbb;
}

h1,h2,h3,h4,h5 {
  color: #269B7D; 
  font-family: "fira sans", "Latin Modern Sans", Calibri, "Trebuchet MS", sans-serif;
}

</style>

# Read me

## Context
This is a very quick & dirty proof of concept of using the _OpenCSV_ library to read a csv file and transforming it
into a json format that would work when the files would be large.
Therefore, in this example I tried to keep the use of heap memory low by using lazy Streams rather than Collections.
Of course this makes the code a little more complex.
For more about the trial and error development decisions see [doc/tweaking.md](doc/tweaking.md)

## The Code
The main class is [`nl.vea.csv2json.opencsv.Converter`](src/main/java/nl/vea/csv2json/opencsv/Converter.java), which
has two overloaded public static methods `convert` that converts csv format input into a simple json format output.
Because this is done with Stream processing to save memory in case the input data is large, we need to know the
number of rows in the input in advance. This is obtained by two further overloaded public static methods named
`countRows`. Reading the csv file twice is a perfectly acceptable strategy when optimizing for memory use and won't take
up much extra time. 

## Run Test
- Run the JUnit tests at
  [`nl.vea.csv2json.opencsv.ConverterTest`](src/test/java/nl/vea/csv2json/opencsv/ConverterTest.java)
- As input, we use [src/test/resources/Periodic_Table_Of_Elements.csv](src/test/resources/Periodic_Table_Of_Elements.csv)
- The converted output will be created at [output/Periodic_Table_Of_Elements.json](output/Periodic_Table_Of_Elements.json)
  (You will only be able to open this link after you have run the test)

