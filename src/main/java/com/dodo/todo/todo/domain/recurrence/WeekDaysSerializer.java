package com.dodo.todo.todo.domain.recurrence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class WeekDaysSerializer extends JsonSerializer<WeekDays> {

    @Override
    public void serialize(WeekDays value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeObject(value.toStrings());
    }
}
