package com.demo.notification.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturnNotificationServiceStatus() throws Exception {
		mockMvc.perform(get("/api/notifications/status").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.isClean", is(true)))
				.andExpect(jsonPath("$.data.service", is("notification-service")))
				.andExpect(jsonPath("$.data.status", is("RUNNING")));
	}
}
