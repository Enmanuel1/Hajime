package com.hajime.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TimeConfiguration {
  @Bean
  Clock applicationClock() {
    return Clock.systemUTC();
  }
}
