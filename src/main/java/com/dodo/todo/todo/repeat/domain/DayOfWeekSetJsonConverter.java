package com.dodo.todo.todo.repeat.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Converter
public class DayOfWeekSetJsonConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> DAY_OF_WEEK_LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(
                    attribute.stream()
                            .sorted()
                            .map(DayOfWeek::name)
                            .toList()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize days of week", exception);
        }
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new LinkedHashSet<>();
        }

        try {
            List<String> dayOfWeekNames = OBJECT_MAPPER.readValue(dbData, DAY_OF_WEEK_LIST_TYPE);
            Set<DayOfWeek> daysOfWeek = new LinkedHashSet<>();

            // JSON 배열 입력은 enum 집합으로 정규화해 도메인 검증에 바로 쓴다.
            for (String dayOfWeekName : dayOfWeekNames) {
                daysOfWeek.add(DayOfWeek.valueOf(dayOfWeekName));
            }

            return daysOfWeek;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to deserialize days of week", exception);
        }
    }
}
