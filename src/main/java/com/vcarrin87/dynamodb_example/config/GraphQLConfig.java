package com.vcarrin87.dynamodb_example.config;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

@Configuration
public class GraphQLConfig {

    /**
     * Registers custom GraphQL runtime wiring.
     *
     * @return runtime wiring configurer
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(uuidScalar());
    }

    /**
     * Builds a UUID scalar implementation.
     *
     * @return UUID scalar type
     */
    private static GraphQLScalarType uuidScalar() {
        return GraphQLScalarType.newScalar()
                .name("UUID")
                .description("A UUID scalar")
                .coercing(new Coercing<UUID, String>() {
                    /**
                     * Serializes Java values into GraphQL UUID strings.
                     */
                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof UUID uuid) {
                            return uuid.toString();
                        }
                        if (dataFetcherResult instanceof String string) {
                            return string;
                        }
                        throw new CoercingSerializeException("Unable to serialize value as UUID: " + dataFetcherResult);
                    }

                    /**
                     * Parses variable input into UUID values.
                     */
                    @Override
                    public UUID parseValue(Object input) {
                        if (input instanceof UUID uuid) {
                            return uuid;
                        }
                        if (input instanceof String string) {
                            try {
                                return UUID.fromString(string);
                            } catch (IllegalArgumentException ex) {
                                throw new CoercingParseValueException("Invalid UUID string: " + string, ex);
                            }
                        }
                        throw new CoercingParseValueException("Expected a UUID string");
                    }

                    /**
                     * Parses AST literal input into UUID values.
                     */
                    @Override
                    public UUID parseLiteral(Object input) {
                        if (input instanceof StringValue stringValue) {
                            try {
                                return UUID.fromString(stringValue.getValue());
                            } catch (IllegalArgumentException ex) {
                                throw new CoercingParseLiteralException("Invalid UUID literal: " + stringValue.getValue(), ex);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected AST type 'StringValue' for UUID scalar");
                    }
                })
                .build();
    }
}
