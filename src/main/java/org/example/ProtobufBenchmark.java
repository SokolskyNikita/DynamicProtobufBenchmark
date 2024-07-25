package org.example;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.example.PersonProto.Person;

public class ProtobufBenchmark {

    private static final int ITERATIONS = 1_000_000;
    private static final String DESC_FILE_PATH = "person.desc";
    private static final String PROTO_FILE_NAME = "person.proto";
    private static final String MESSAGE_NAME = "Person";

    @SneakyThrows
    public static void main(String[] args) {
        // Load descriptor once
        Descriptors.Descriptor descriptor = loadDescriptorFromFile(DESC_FILE_PATH, PROTO_FILE_NAME, MESSAGE_NAME);

        // Benchmark serialization
        long normalSerializeTime = benchmarkNormalSerialization();
        long dynamicSerializeTime = benchmarkDynamicSerialization(descriptor);

        // Benchmark deserialization
        long normalDeserializeTime = benchmarkNormalDeserialization();
        long dynamicDeserializeTime = benchmarkDynamicDeserialization(descriptor);

        // Print results
        System.out.println("Running " + ITERATIONS + " iterations of each type of Protobuf operation");
        System.out.println("Normal Protobuf serialization time: " + normalSerializeTime + " ms");
        System.out.println("Dynamic Protobuf serialization time: " + dynamicSerializeTime + " ms");
        System.out.println("Normal Protobuf deserialization time: " + normalDeserializeTime + " ms");
        System.out.println("Dynamic Protobuf deserialization time: " + dynamicDeserializeTime + " ms");
    }

    private static Person createNormalPerson() {
        return Person.newBuilder()
            .setName("John Doe")
            .setId(123)
            .setEmail("john.doe@example.com")
            .addPhones(Person.PhoneNumber.newBuilder()
                .setNumber("555-1234")
                .setType(Person.PhoneType.MOBILE))
            .setAddress(Person.Address.newBuilder()
                .setStreet("123 Main St")
                .setCity("Anytown")
                .setCountry("USA")
                .setPostalCode("12345"))
            .setLastUpdated(System.currentTimeMillis())
            .build();
    }

    private static DynamicMessage createDynamicPerson(Descriptors.Descriptor descriptor) {
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(descriptor);
        messageBuilder.setField(descriptor.findFieldByName("name"), "John Doe");
        messageBuilder.setField(descriptor.findFieldByName("id"), 123);
        messageBuilder.setField(descriptor.findFieldByName("email"), "john.doe@example.com");

        Descriptors.Descriptor phoneDescriptor = descriptor.findNestedTypeByName("PhoneNumber");
        DynamicMessage.Builder phoneBuilder = DynamicMessage.newBuilder(phoneDescriptor);
        phoneBuilder.setField(phoneDescriptor.findFieldByName("number"), "555-1234");
        Descriptors.FieldDescriptor typeField = phoneDescriptor.findFieldByName("type");
        Descriptors.EnumValueDescriptor enumValue = typeField.getEnumType().findValueByName("MOBILE");
        phoneBuilder.setField(typeField, enumValue);
        messageBuilder.addRepeatedField(descriptor.findFieldByName("phones"), phoneBuilder.build());

        Descriptors.Descriptor addressDescriptor = descriptor.findNestedTypeByName("Address");
        DynamicMessage.Builder addressBuilder = DynamicMessage.newBuilder(addressDescriptor);
        addressBuilder.setField(addressDescriptor.findFieldByName("street"), "123 Main St");
        addressBuilder.setField(addressDescriptor.findFieldByName("city"), "Anytown");
        addressBuilder.setField(addressDescriptor.findFieldByName("country"), "USA");
        addressBuilder.setField(addressDescriptor.findFieldByName("postal_code"), "12345");
        messageBuilder.setField(descriptor.findFieldByName("address"), addressBuilder.build());

        messageBuilder.setField(descriptor.findFieldByName("last_updated"), System.currentTimeMillis());

        return messageBuilder.build();
    }

    private static long benchmarkNormalSerialization() {
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Person person = createNormalPerson();
            person.toByteArray();
        }
        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    private static long benchmarkDynamicSerialization(Descriptors.Descriptor descriptor) {
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            DynamicMessage message = createDynamicPerson(descriptor);
            message.toByteArray();
        }
        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    @SneakyThrows
    private static long benchmarkNormalDeserialization() {
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            Person person = createNormalPerson();
            String json = JsonFormat.printer().print(person);
            Person.Builder builder = Person.newBuilder();
            JsonFormat.parser().merge(json, builder);
            builder.build();
        }
        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    @SneakyThrows
    private static long benchmarkDynamicDeserialization(Descriptors.Descriptor descriptor) {
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            DynamicMessage message = createDynamicPerson(descriptor);
            String json = JsonFormat.printer().print(message);
            DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
            JsonFormat.parser().merge(json, builder);
            builder.build();
        }
        long endTime = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    private static Descriptors.Descriptor loadDescriptorFromFile(String descFilePath, String protoFileName,
        String messageName) throws IOException, Descriptors.DescriptorValidationException {
        FileInputStream fis = new FileInputStream(descFilePath);
        DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(fis);

        Map<String, DescriptorProtos.FileDescriptorProto> fileDescriptorProtoMap = new HashMap<>();
        for (DescriptorProtos.FileDescriptorProto fdp : descriptorSet.getFileList()) {
            fileDescriptorProtoMap.put(fdp.getName(), fdp);
        }

        DescriptorProtos.FileDescriptorProto fileDescriptorProto = fileDescriptorProtoMap.get(protoFileName);
        if (fileDescriptorProto == null) {
            throw new RuntimeException("Proto file not found: " + protoFileName);
        }

        Descriptors.FileDescriptor fileDescriptor = buildFileDescriptor(fileDescriptorProto, fileDescriptorProtoMap);
        return fileDescriptor.findMessageTypeByName(messageName);
    }

    private static Descriptors.FileDescriptor buildFileDescriptor(DescriptorProtos.FileDescriptorProto fdp,
        Map<String, FileDescriptorProto> fileDescriptorProtoMap) throws Descriptors.DescriptorValidationException {
        Descriptors.FileDescriptor[] dependencies = new Descriptors.FileDescriptor[fdp.getDependencyCount()];
        for (int i = 0; i < fdp.getDependencyCount(); i++) {
            String dependencyName = fdp.getDependency(i);
            if (!fileDescriptorProtoMap.containsKey(dependencyName)) {
                throw new RuntimeException("Dependency not found: " + dependencyName);
            }
            dependencies[i] = buildFileDescriptor(fileDescriptorProtoMap.get(dependencyName), fileDescriptorProtoMap);
        }
        return Descriptors.FileDescriptor.buildFrom(fdp, dependencies);
    }
}