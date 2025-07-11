package io.github.ilubenets.springtemporal;

import io.github.ilubenets.springtemporal.adapter.repository.PostgresqlObjectMapper;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.converter.JacksonJsonPayloadConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TemporalConfig {

    @Bean("mainDataConverter")
    @Primary
    public DataConverter mainDataConverter() {
        return DefaultDataConverter.newDefaultInstance()
            .withPayloadConverterOverrides(new JacksonJsonPayloadConverter(PostgresqlObjectMapper.instance()));
    }
}
