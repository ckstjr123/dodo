package com.dodo.todo.todo.domain.recurrence;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.List;
import net.fortuna.ical4j.model.WeekDay;

public class WeekDaysDeserializer extends JsonDeserializer<WeekDays> {

    @Override
    public WeekDays deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<String> values = p.readValueAs(new TypeReference<>() {
        });
        return WeekDays.from(values.stream().map(WeekDay::new).toList());
    }
}
