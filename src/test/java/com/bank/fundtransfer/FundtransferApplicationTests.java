package com.bank.fundtransfer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FundtransferApplicationTests {

	@Test
	void contextLoads() {
		// passes if Spring starts, connects to the test database, runs Flyway and validates the entities
	}
}