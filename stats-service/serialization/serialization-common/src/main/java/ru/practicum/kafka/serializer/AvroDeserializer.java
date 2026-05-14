package ru.practicum.kafka.serializer;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class AvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final Class<T> targetClass;
    private final DatumReader<T> reader;
    private BinaryDecoder decoder;

    public AvroDeserializer(Class<T> targetClass) {
        this.targetClass = targetClass;
        try {
            this.reader = new SpecificDatumReader<>(targetClass.newInstance().getSchema());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create Avro deserializer for " + targetClass, e);
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            decoder = decoderFactory.binaryDecoder(data, decoder);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new SerializationException("Ошибка десериализации данных из топика " + topic, e);
        }
    }
}