package com.gng.test;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileValidationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private static WireMockServer wireMockServer;

	@BeforeEach
	void setup() {
		if (wireMockServer == null) {
			wireMockServer = new WireMockServer(options().port(8089));
			wireMockServer.start();
			WireMock.configureFor("localhost", 8089);
		}
		wireMockServer.resetAll();
	}

	@Test
	void testIngestFile_AllowsRequest_WhenCountryNotBlocked() throws Exception {
		wireMockServer.stubFor(get(urlPathEqualTo("/json/127.0.0.1"))
				.willReturn(okJson("""
                    {
                        "countryCode": "CA",
                        "isp": "Bell Canada"
                    }
                    """)));

		// Create a sample multipart file
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"data.txt",
				"text/plain",
				"18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1".getBytes()
		);

		// Perform the request
		mockMvc.perform(multipart("/process")
						.file(file)
						.param("flag", "false")
						.header("X-Real-IP", "127.0.0.1"))
				.andExpect(status().isOk());
	}

	@Test
	void testIngestFile_Blocks_WhenCountryIsBlocked() throws Exception {
		wireMockServer.stubFor(get(urlPathMatching("/json/127.0.0.1"))
				.willReturn(okJson("""
                    {
                        "countryCode": "US",
                        "isp": "Comcast"
                    }
                """)));

		MockMultipartFile file = new MockMultipartFile(
				"file",
				"data.txt",
				"text/plain",
				("18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1").getBytes()
		);

		mockMvc.perform(multipart("/process")
						.file(file)
						.param("flag", "false")
						.header("X-Real-IP", "127.0.0.1"))
				.andExpect(status().isForbidden());
	}

	@Test
	void testIngestFile_Blocks_WhenISPBlocked() throws Exception {
		wireMockServer.stubFor(get(urlPathMatching("/json/127.0.0.1"))
				.willReturn(okJson("""
                    {
                        "countryCode": "CA",
                        "isp": "Amazon Web Services"
                    }
                """)));

		MockMultipartFile file = new MockMultipartFile(
				"file",
				"text/plain",
				"text/plain",
				("18148426-89e1-11ee-b9d1-0242ac120002|1X1D14|John Smith|Likes Apricots|Rides A Bike|6.2|12.1").getBytes()
		);

		mockMvc.perform(multipart("/process")
						.file(file)
						.param("flag", "false")
						.header("X-Real-IP", "127.0.0.1"))
				.andExpect(status().isForbidden());
	}
}