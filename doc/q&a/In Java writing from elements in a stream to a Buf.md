<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# In Java writing from elements in a stream to a BufferedWriter.

The last element should not be suffixed with a comma. Is there a way to figure out the last element in a stream within the foreach method? Or to undo the last suffix write to the BufferedWriter?

To write elements from a Java stream to a `BufferedWriter` without a trailing comma after the last element, use the `collect(joining(", "))` method from `java.util.stream.Collectors`. This handles the logic internally by joining all elements with the specified delimiter, omitting it after the final one.

```java
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

// Assuming you have a list of strings or similar elements
List<String> elements = List.of("apple", "banana", "cherry");

try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
    String joined = elements.stream()
                           .collect(Collectors.joining(", "));
    writer.write(joined);
    writer.newLine(); // Optional: add a line break if needed
} catch (IOException e) {
    // Handle exception
}
```

This approach avoids manual iteration and suffix management altogether, making it efficient and concise for streams. If your elements are complex objects requiring custom string conversion, apply a `map()` before collecting, such as `.map(Object::toString)`. For very large streams, this is still performant as it builds the string in a single pass .

---

# In my case for very large streams this will take up a lot of heap memory, is there no other way?

For very large streams where building a full string in memory is impractical, process elements sequentially using `forEachOrdered()` to maintain order while writing directly to the `BufferedWriter`. This avoids loading the entire stream into memory by handling one element at a time, but detecting the "last" element requires precomputing the total count (e.g., via `count()`), which works efficiently for streams from sources like databases or files that support it without full materialization.

If your stream source allows efficient counting (such as JDBC result sets or infinite streams with a known size), first determine the count, then track an index in the loop to conditionally write the comma only before non-last elements. Here's an example assuming a countable stream of strings:

```java
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Stream;

Stream<String> stream = // your large stream source, e.g., from database cursor
    Stream.generate(() -> "item").limit(1000000); // example large stream

long totalCount = stream.count(); // Efficient if source supports it; otherwise, skip this approach
Stream<String> resetStream = // recreate or reset your stream source

int index = 0;
try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
    resetStream.forEachOrdered(item -> {
        try {
            if (index > 0) {
                writer.write(", ");
            }
            writer.write(item);
            index++;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
    writer.newLine(); // Optional
} catch (IOException e) {
    // Handle exception
}
```

This keeps memory usage low as elements are processed and discarded immediately, with the index check ensuring no trailing comma. For uncountable streams (e.g., truly infinite or non-resettable), use an `AtomicBoolean` flag in `forEachOrdered()` to write the comma only after the first element, then manually seek back and remove the trailing comma using `writer` (though `BufferedWriter` doesn't support direct seek—flush and use `RandomAccessFile` for the underlying file if needed, or accept the trailing comma and post-process). If your stream is from a database, consider JDBC cursors with `fetchSize` for chunked processing to further optimize.[^2_1][^2_2]
<span style="display:none">[^2_10][^2_3][^2_4][^2_5][^2_6][^2_7][^2_8][^2_9]</span>

<div align="center">⁂</div>

[^2_1]: https://stackoverflow.com/questions/73606002/java-stream-apply-different-logic-to-last-element-in-a-single-pass

[^2_2]: https://dev.to/xuan_56087d315ff4f52254e6/efficiently-handling-large-result-sets-with-java-streams-and-database-cursors-2lic

[^2_3]: https://docs.oracle.com/javase/8/docs/api/java/io/BufferedWriter.html

[^2_4]: https://javanexus.com/blog/stream-data-vs-memory

[^2_5]: https://stackoverflow.com/questions/37985778/java-stream-comma-separated

[^2_6]: https://stackoverflow.com/questions/41591107/detect-last-foreach-loop-iteration

[^2_7]: https://docsallover.com/blog/general-purpose-programming/java-io-reading-writing-working-with-files/

[^2_8]: http://eliux.ca/java/tricks/use-java-8-stream-to-deal-very-large-input/

[^2_9]: https://www.geeksforgeeks.org/java/find-the-last-element-of-a-stream-in-java/

[^2_10]: https://stackoverflow.com/questions/28573392/how-to-avoid-remove-writing-the-last-comma-in-a-row


---

# My source is a BufferedReader that is used to construct an OpenCSV  CSVReader From this I create a Stream<String[]> by using the CSVReader.iterator() combined with StreamSupport and java.util.Spliterators.spliteratorUnknownSize.

Is there a way to reset the BufferedReader or CSVReader to first determine the number of rows and then reread all to produce the stream like I did already?

`BufferedReader` and `CSVReader` are forward-only readers; there is no general, reliable way to “rewind” them and re‑create the same stream without going back to the underlying data source or buffering everything yourself in memory.[^3_1][^3_6][^3_7]

## Why reset does not help

- `BufferedReader.mark()`/`reset()` only work within the read‑ahead limit and only from the last mark, so they cannot be used to read the whole CSV once to count rows and then go back to the beginning. If you read past the buffer or the read‑ahead limit, the mark becomes invalid.[^3_7][^3_1]
- OpenCSV’s `CSVReader` wraps a `Reader` but does not provide a “rewind” mechanism; it consumes the underlying `Reader` linearly (via `readNext()`, iterator, etc.).[^3_6][^3_10]

So with your current setup (a `BufferedReader` that is already being consumed by `CSVReader`), you cannot “reset” and re‑read everything just to know the total row count.

## Practical options

Given you want to avoid storing all rows in memory and avoid a trailing comma when writing:

1) Use a simple state flag instead of knowing the last row
You do not actually need to know the last element if you use a “first element” flag and write the delimiter before each element except the first:
    - Initialize `boolean first = true;` outside the loop.
    - For each `String[] row` from your `Stream<String[]>`:
        - If `first` is false, write `","` (or `", "`).
        - Then write the current row.
        - Set `first = false`.

This pattern works perfectly with a single forward pass over the CSV stream, does not need a row count, and uses O(1) extra memory.
2) If you really need the count, re-open the source
    - If the CSV ultimately comes from a file, path, or other re‑openable source, do two passes by creating two independent readers:
        - First pass: open a new `BufferedReader`/`CSVReader`, iterate to count rows, then close it.
        - Second pass: open a fresh `BufferedReader`/`CSVReader`, build your `Stream<String[]>`, and write with index logic if you insist on knowing “last element”.
    - This is the only robust way to “reset” for large content without caching everything in memory.
3) Avoid trying to undo writes
    - You cannot reliably “undo” the last write on a `BufferedWriter`. It has no seek capability; you would need to work on something like a `RandomAccessFile` and manage positions manually, which complicates things and does not fit well with streaming CSV rows.[^3_1]

In practice, for your CSV stream case, the usual and memory‑safe solution is option 1: a single pass with a boolean “first element” flag and writing the separator before all but the first row, without ever needing to reset the `BufferedReader` or `CSVReader`.
<span style="display:none">[^3_2][^3_3][^3_4][^3_5][^3_8][^3_9]</span>

<div align="center">⁂</div>

[^3_1]: https://stackoverflow.com/questions/10870132/how-can-i-reset-the-position-of-the-bufferedreader

[^3_2]: https://opencsv.sourceforge.net/jacoco/com.opencsv/CSVReader.java.html

[^3_3]: https://opencsv.sourceforge.net/xref/com/opencsv/CSVReader.html

[^3_4]: https://sourceforge.net/p/opencsv/bugs/206/

[^3_5]: https://www.geeksforgeeks.org/java/bufferedreader-mark-method-in-java-with-examples/

[^3_6]: https://www.baeldung.com/opencsv

[^3_7]: https://docs.oracle.com/javase/8/docs/api/java/io/BufferedReader.html

[^3_8]: https://coderanch.com/t/266362/certification/mark-reset-working-BufferedReader

[^3_9]: https://sourceforge.net/p/opencsv/bugs/109/

[^3_10]: https://javadoc.io/doc/com.opencsv/opencsv/5.7.1/com/opencsv/CSVReader.html

