package com.safra.safra.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.locationtech.jts.geom.Point;

import java.io.IOException;

public class PointSerializer extends StdSerializer<Point> {

    public PointSerializer() {
        super(Point.class);
    }

    @Override
    public void serialize(Point point, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (point != null) {
            gen.writeStartObject();
            gen.writeNumberField("x", point.getX()); // longitude
            gen.writeNumberField("y", point.getY()); // latitude
            gen.writeEndObject();
        } else {
            gen.writeNull();
        }
    }
}

