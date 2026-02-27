<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# example for validating json with json schema in java

JSON schema validation in Java can be accomplished using several libraries, with the Everit JSON Schema Validator and the NetworkNT JSON Schema Validator being the most popular options.[^1_1][^1_2]

## Using Everit JSON Schema Validator

The Everit library works with the `org.json` API and provides straightforward validation. Here's a basic example:[^1_1]

```java
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

try (InputStream inputStream = getClass().getResourceAsStream("/path/to/your/schema.json")) {
  JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
  Schema schema = SchemaLoader.load(rawSchema);
  schema.validate(new JSONObject("{\"hello\" : \"world\"}")); // throws ValidationException if invalid
}
```

For more control over validation behavior, you can use the `Validator` class:[^1_3][^1_1]

```java
import org.everit.json.schema.Validator;

Validator validator = Validator.builder()
    .failEarly()
    .build();
validator.performValidation(schema, input);
```

The `Validator` class is immutable and thread-safe, so you can configure it once and reuse it across multiple validations.[^1_3]

## Handling Validation Errors

To catch validation errors, wrap the validation call in a try-catch block:[^1_4][^1_5]

```java
try {
  schema.validate(jsonData);
} catch (ValidationException e) {
  // Handle validation error
}
```


## Using NetworkNT JSON Schema Validator

If you're already using Jackson in your project, the NetworkNT library is a better fit as it integrates with Jackson and supports multiple JSON Schema draft versions (V4, V6, V7, V2019-09, V2020-12).[^1_2][^1_6]
<span style="display:none">[^1_10][^1_7][^1_8][^1_9]</span>

<div align="center">⁂</div>

[^1_1]: https://github.com/everit-org/json-schema

[^1_2]: https://github.com/networknt/json-schema-validator

[^1_3]: https://github.com/erosb/everit-json-schema

[^1_4]: https://stackoverflow.com/questions/71326806/how-to-validate-json-schema-structure-with-another-json-schema-with-java

[^1_5]: https://www.innominds.com/blog/validating-json-document-against-schema

[^1_6]: https://stackoverflow.com/questions/61136401/library-for-json-schema-and-validation-in-java

[^1_7]: https://camel.apache.org/components/4.10.x/json-validator-component.html

[^1_8]: https://github.com/harrel56/json-schema

[^1_9]: https://erosb.github.io/everit-json-schema/javadoc/1.11.0/org/everit/json/schema/Schema.html

[^1_10]: https://stackoverflow.com/questions/71761633/how-to-get-error-message-in-json-schema-validation-using-everit-in-java


---

# Validate JSON with Jackson based validator example

The NetworkNT JSON Schema Validator integrates seamlessly with Jackson and provides comprehensive JSON schema validation support. Here are practical examples:[^2_1][^2_2]

## Basic Validation Example

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.Set;

public class JsonSchemaValidation {
    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        
        try (InputStream jsonStream = inputStreamFromClasspath("example.json");
             InputStream schemaStream = inputStreamFromClasspath("example-schema.json")) {
            
            JsonNode json = objectMapper.readTree(jsonStream);
            JsonSchema schema = schemaFactory.getSchema(schemaStream);
            Set<ValidationMessage> validationResult = schema.validate(json);
            
            if (validationResult.isEmpty()) {
                System.out.println("no validation errors :-)");
            } else {
                validationResult.forEach(vm -> System.out.println(vm.getMessage()));
            }
        }
    }
    
    private static InputStream inputStreamFromClasspath(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }
}
```

The `JsonSchemaFactory` requires a `VersionFlag` to specify the JSON Schema version (V4, V6, V7, V201909, or V202012).[^2_2][^2_3]

## Validating JsonNode Objects

If you already have `JsonNode` objects parsed from your application, you can validate them directly:[^2_4]

```java
JsonNode schemaNode = objectMapper.valueToTree(schemaObject);
JsonNode jsonNode = objectMapper.readTree(jsonString);

JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
JsonSchema jsonSchema = jsonSchemaFactory.getSchema(schemaNode);

Set<ValidationMessage> validationErrors = jsonSchema.validate(jsonNode);
```


## Loading Schema from String or Classpath

You can load schemas from various sources:[^2_5]

```java
// From String (not recommended for schemas with relative $ref)
JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
JsonSchema schema = factory.getSchema("{\"enum\":[1, 2, 3, 4]}");

// From classpath
JsonSchema schemaFromClasspath = factory.getSchema(SchemaLocation.of("classpath:schema/example-ref.json"));
schemaFromClasspath.initializeValidators();
```


## Schema Mapping for Better \$ref Resolution

For production use, configure schema mappers to properly resolve `$id` and `$ref`:[^2_5]

```java
JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, 
    builder -> builder.schemaMappers(schemaMappers -> 
        schemaMappers.mapPrefix("https://www.example.com/schema", "classpath:schema")));

JsonSchema schema = factory.getSchema(SchemaLocation.of("https://www.example.com/schema/example-ref.json"));
```


## Validating String Input with ExecutionContext

You can validate JSON strings directly and configure execution options:[^2_3][^2_5]

```java
Set<ValidationMessage> errors = schema.validate("{\"id\": \"2\"}", InputFormat.JSON,
    executionContext -> executionContext.getExecutionConfig().setFormatAssertionsEnabled(true));
```

The library is thread-safe and performant, making it suitable for Spring applications and high-throughput scenarios.[^2_1][^2_3]
<span style="display:none">[^2_10][^2_11][^2_6][^2_7][^2_8][^2_9]</span>

<div align="center">⁂</div>

[^2_1]: https://www.mscharhag.com/java/json-schema-validation

[^2_2]: https://github.com/everit-org/json-schema

[^2_3]: https://github.com/networknt/json-schema-validator

[^2_4]: https://stackoverflow.com/questions/75063085/how-to-validate-array-item-types-with-networknt-json-schema-validator

[^2_5]: https://camel.apache.org/components/4.10.x/json-validator-component.html

[^2_6]: https://www.javatips.net/api/com.github.fge.jsonschema.core.report.processingreport

[^2_7]: https://javarevisited.blogspot.com/2022/12/how-to-validate-json-in-java-jackson.html

[^2_8]: https://github.com/java-json-tools/json-schema-validator/blob/master/src/main/java/com/github/fge/jsonschema/main/JsonSchemaFactory.java

[^2_9]: https://stackoverflow.com/questions/31179086/validate-json-schema-compliance-with-jackson-against-an-external-schema-file

[^2_10]: https://stackoverflow.com/questions/70129741/jsonschema-and-validation

[^2_11]: https://github.com/networknt/json-schema-validator/blob/master/doc/quickstart.md

