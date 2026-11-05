package team.projectpulse.student;

import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.EntityManager;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.StatusCode;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration tests for Student API endpoints")
class StudentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    EntityManager entityManager;

    String adminToken;

    String instructorBillToken;

    String studentJohnToken;

    String studentEricToken;

    String adminTimToken;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    @BeforeEach
    void setUp() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.wei@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        MvcResult mvcResult = resultActions.andDo(print()).andReturn();
        String contentAsString = mvcResult.getResponse().getContentAsString();
        JSONObject json = new JSONObject(contentAsString);
        this.adminToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.gates@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.instructorBillToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentJohnToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("e.hudson@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentEricToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("t.cook@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.adminTimToken = "Bearer " + json.getJSONObject("data").getString("token");
    }

    @Test
    void testFindStudentsByCriteria() throws Exception {
        // Given
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("teamName", "Team1");
        String json = this.jsonMapper.writeValueAsString(searchCriteria);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "0");
        requestParams.add("size", "30");
        requestParams.add("sort", "lastName,asc");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students/search").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students successfully"))
                .andExpect(jsonPath("$.data.content", Matchers.hasSize(3)));
    }

    @Test
    void testInstructorBillFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testAdminTimFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testStudentJohnFindOwnInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testStudentJohnFindAnotherStudentsInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/5").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testAddStudent() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "Abc123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Add student successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value("lucas"))
                .andExpect(jsonPath("$.data.firstName").value("Lucas"))
                .andExpect(jsonPath("$.data.lastName").value("Santos"))
                .andExpect(jsonPath("$.data.email").value("l.santos@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void testAddStudentWithInvalidUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "marcos");
        studentDto.put("firstName", "Marcos");
        studentDto.put("lastName", "Grant");
        studentDto.put("email", "m.grant@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "invalidRegistrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("m.grant@abc.edu is not invited to register. Please contact the course admin."));
    }

    @Test
    void testAddStudentWithNoWrongUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "instructor"); // This is wrong, Lucas should be a student.

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("You are not allowed to register as instructor for email: l.santos@abc.edu"));
    }

    @Test
    void testStudentJohnUpdatesOwnInfo() throws Exception {
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "john");
        studentDto.put("firstName", "John (updated)");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");
        studentDto.put("enabled", false); // Student cannot change their enabled status.
        studentDto.put("roles", "instructor"); // Student cannot change their role.

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/4").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Update student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("john"))
                .andExpect(jsonPath("$.data.firstName").value("John (updated)"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.enabled").value(true)) // The enabled status should not be changed.
                .andExpect(jsonPath("$.data.roles").value("student")); // The role should not be changed.
    }

    @Test
    void testInstructorEditWithEnabledFalseDoesNotDeactivateStudent() throws Exception {
        // The generic edit no longer deactivates anyone; deactivation is UC-STU-deactivate-student's own flow
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "j.smith@abc.edu");
        studentDto.put("firstName", "John");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");
        studentDto.put("enabled", false);

        this.mockMvc.perform(put(this.baseUrl + "/students/4").contentType(MediaType.APPLICATION_JSON).content(this.jsonMapper.writeValueAsString(studentDto)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.teamName").value("Team1"));
    }

    @Test
    void testInstructorEditWithEnabledMissingLeavesStatusUnchanged() throws Exception {
        // StudentDto.enabled is a primitive boolean, and Jackson 3 refuses a missing primitive creator property
        // (FAIL_ON_NULL_FOR_PRIMITIVES), so the edit is rejected as malformed and nothing changes
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "j.smith@abc.edu");
        studentDto.put("firstName", "John (updated)");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");

        this.mockMvc.perform(put(this.baseUrl + "/students/4").contentType(MediaType.APPLICATION_JSON).content(this.jsonMapper.writeValueAsString(studentDto)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT));

        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void testStudentJohnUpdatesAnotherStudentsInfo() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "eric");
        studentDto.put("firstName", "Eric (updated)");
        studentDto.put("lastName", "Hudson");
        studentDto.put("email", "e.hudson@abc.edu");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/5").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    // UC-STU-deactivate-student

    @Test
    void testCourseAdminDeactivatesStudent() throws Exception {
        // Main success scenario: John is active and on Team1
        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.teamId").doesNotExist())
                .andExpect(jsonPath("$.data.teamName").doesNotExist());

        // A re-fetch agrees, and Team1 no longer includes him (BR-deactivation-team-removal)
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.teamName").doesNotExist());
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.data", Matchers.hasSize(2)));

        // He can no longer log in, and the token he already held is refused on its next request (FR-SEC-active-account)
        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void testSectionInstructorDeactivatesStudent() throws Exception {
        // Bill is assigned to John's course section but is not the course admin
        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void testCourseAdminNotAssignedToTheSectionDeactivatesStudent() throws Exception {
        // Given: Bingyang stays course admin of John's course but is no longer assigned to his course section, so only
        // the course-admin branch of the route rule can admit him
        Instructor bingyang = this.entityManager.createQuery("select i from Instructor i where i.email = 'b.wei@abc.edu'", Instructor.class).getSingleResult();
        Student john = this.entityManager.find(Student.class, 4);
        Section section = john.getSection();
        section.removeInstructor(bingyang);
        this.entityManager.flush();
        assertThat(section.getInstructors()).doesNotContain(bingyang);
        assertThat(section.getCourse().getCourseAdmin().getId()).isEqualTo(bingyang.getId());

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void testStudentCannotDeactivateHerself() throws Exception {
        // 1a
        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
        assertJohnStillActiveOnTeam1();
    }

    @Test
    void testInstructorOfAnotherCourseCannotDeactivateStudent() throws Exception {
        // 1a: Tim is course admin of course3 and instructor of section3 only
        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
        assertJohnStillActiveOnTeam1();
    }

    @Test
    void testDeactivateNonexistentStudentIsForbidden() throws Exception {
        // The route rule refuses an id that names no student before the service runs
        this.mockMvc.perform(put(this.baseUrl + "/students/999/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeactivateAlreadyDeactivatedStudent() throws Exception {
        // 6a
        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"));

        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Student is already deactivated"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void testInstructorReactivatesDeactivatedStudentThroughTheTemporaryPath() throws Exception {
        this.mockMvc.perform(put(this.baseUrl + "/students/4/deactivation").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(jsonPath("$.data.enabled").value(false));

        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "j.smith@abc.edu");
        studentDto.put("firstName", "John");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");
        studentDto.put("enabled", true);

        this.mockMvc.perform(put(this.baseUrl + "/students/4").contentType(MediaType.APPLICATION_JSON).content(this.jsonMapper.writeValueAsString(studentDto)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.teamName").doesNotExist()); // Her team is not restored
    }

    private void assertJohnStillActiveOnTeam1() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.teamName").value("Team1"));
    }

//    @Test
//    void testAdminDeletesStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/3").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
//                .andExpect(jsonPath("$.flag").value(true))
//                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
//                .andExpect(jsonPath("$.message").value("Delete instructor successfully"));
//    }
//
//    @Test
//    void testStudentDeletesAnotherStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentToken))
//                .andExpect(jsonPath("$.flag").value(false))
//                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
//                .andExpect(jsonPath("$.message").value("No permission."))
//                .andExpect(jsonPath("$.data").value("Access Denied"));
//    }

    @Test
    void testStudentJohnFindStudentsInOwnTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students by team id successfully"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
    }

    @Test
    void testStudentJohnFindStudentsInAnotherTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/2").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

}
