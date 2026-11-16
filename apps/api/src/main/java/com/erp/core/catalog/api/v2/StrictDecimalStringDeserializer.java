package com.erp.core.catalog.api.v2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/** Prevents Jackson's default scalar coercion from accepting JSON numbers for v2 monetary strings. */
public final class StrictDecimalStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(parser, "price must be a JSON decimal string");
        }
        return parser.getText();
    }
}
