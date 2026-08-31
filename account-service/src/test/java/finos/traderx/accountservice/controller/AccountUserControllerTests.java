package finos.traderx.accountservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.service.AccountUserService;

@WebMvcTest(AccountUserController.class)
class AccountUserControllerTests {

    private static final String PEOPLE_SERVICE_URL = "http://people-service:18089";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountUserController accountUserController;

    @MockitoBean
    AccountUserService accountUserService;

    private MockRestServiceServer peopleService;

    @BeforeEach
    void bindPeopleService() {
        RestTemplate restTemplate = new RestTemplate();
        peopleService = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(accountUserController, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(accountUserController, "peopleServiceAddress", PEOPLE_SERVICE_URL);
    }

    @Test
    void createsAccountUserWhenPersonExists() throws Exception {
        peopleService.expect(requestTo(PEOPLE_SERVICE_URL + "/People/GetPerson?LogonId=jsmith"))
                .andRespond(withSuccess("{\"id\":1,\"logonId\":\"jsmith\"}", MediaType.APPLICATION_JSON));

        AccountUser saved = new AccountUser();
        saved.setAccountId(1);
        saved.setUsername("jsmith");
        given(accountUserService.upsertAccountUser(any(AccountUser.class))).willReturn(saved);

        mockMvc.perform(post("/accountuser/")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":1,\"username\":\"jsmith\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jsmith"));

        peopleService.verify();
    }

    @Test
    void returnsNotFoundWhenPeopleServiceReturns404() throws Exception {
        peopleService.expect(requestTo(PEOPLE_SERVICE_URL + "/People/GetPerson?LogonId=nobody"))
                .andRespond(withResourceNotFound());

        mockMvc.perform(post("/accountuser/")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":1,\"username\":\"nobody\"}"))
                .andExpect(status().isNotFound());

        peopleService.verify();
    }
}
