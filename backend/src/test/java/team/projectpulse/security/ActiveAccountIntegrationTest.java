package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account: an account deactivated after its token was issued loses access on its next request.
 */
@DisplayName("Integration tests for the per-request active-account check")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    private String login(String username) throws Exception {
        String body = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(username, "123456")))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + new JSONObject(body).getJSONObject("data").getString("token");
    }

    private void deactivate(Integer userId) {
        this.userRepository.findById(userId).orElseThrow().setEnabled(false);
        this.userRepository.flush();
    }

    @Test
    void testTokenOfActiveStudentStillWorks() throws Exception {
        String johnToken = login("j.smith@abc.edu");

        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, johnToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS));
    }

    @Test
    void testTokenIssuedBeforeStudentWasDeactivatedIsRefused() throws Exception {
        String johnToken = login("j.smith@abc.edu");
        deactivate(4);

        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, johnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void testTokenIssuedBeforeInstructorWasDeactivatedIsRefused() throws Exception {
        String billToken = login("b.gates@abc.edu");
        deactivate(2);

        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, billToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void testDeactivatedStudentCannotLogIn() throws Exception {
        deactivate(4);

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

}
