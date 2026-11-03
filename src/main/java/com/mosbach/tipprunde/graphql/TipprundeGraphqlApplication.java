package com.mosbach.tipprunde.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// KI-Log MA-15 "Bereite den GraphQL-Service vor und teile ihn nach der Reihenfolge der SPA auf."
// GraphQL-Service: gleicher Funktionsumfang wie die REST-API des Core-Service, ruft den Core nicht blockierend auf
@SpringBootApplication
public class TipprundeGraphqlApplication {

	public static void main(String[] args) {
		SpringApplication.run(TipprundeGraphqlApplication.class, args);
	}
}
