package com.dodo.todo.todo.domain.recurrence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RecurrenceRuleConverter implements AttributeConverter<RecurrenceRule, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(RecurrenceRule attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(RecurrenceRuleError.INVALID_RECURRENCE_RULE.message(), exception);
        }
    }

    @Override
    public RecurrenceRule convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(dbData, RecurrenceRule.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(RecurrenceRuleError.INVALID_RECURRENCE_RULE.message(), exception);
        }
    }
}
