package com.mosbach.tipprunde.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// GraphQL-Service: gleicher Funktionsumfang wie die REST-API des Core-Service, ruft den Core nicht blockierend auf
@SpringBootApplication
public class TipprundeGraphqlApplication {

	public static void main(String[] args) {
		SpringApplication.run(TipprundeGraphqlApplication.class, args);
	}
}
