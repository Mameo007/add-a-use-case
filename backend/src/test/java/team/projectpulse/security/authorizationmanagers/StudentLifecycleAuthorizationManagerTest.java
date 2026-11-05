package team.projectpulse.security.authorizationmanagers;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.student.Student;
import team.projectpulse.student.StudentRepository;
import team.projectpulse.student.StudentSecurityService;
import team.projectpulse.system.UserUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The route guard for {@code PUT /students/{studentId}/deactivation}, run over the real
 * {@link StudentSecurityService} so the fixture's course admin, who is not assigned to the course section, is decided
 * by the same code the route rule calls (UC-STU-deactivate-student, BR-student-lifecycle).
 */
@ExtendWith(MockitoExtension.class)
class StudentLifecycleAuthorizationManagerTest {

    private static final int COURSE_ADMIN_ID = 1;
    private static final int SECTION_INSTRUCTOR_ID = 2;
    private static final int OTHER_COURSE_INSTRUCTOR_ID = 3;
    private static final int STUDENT_ID = 4;

    @Mock
    StudentRepository studentRepository;
    @Mock
    UserUtils userUtils;

    StudentLifecycleAuthorizationManager manager;

    Student john;


    @BeforeEach
    void setUp() {
        this.manager = new StudentLifecycleAuthorizationManager(new StudentSecurityService(this.studentRepository, this.userUtils));

        // The course admin owns the course but is deliberately not one of its course section's instructors
        Instructor courseAdmin = new Instructor("b.wei@abc.edu", "Bingyang", "Wei", "b.wei@abc.edu", "123456", true, "admin instructor");
        courseAdmin.setId(COURSE_ADMIN_ID);
        Instructor sectionInstructor = new Instructor("b.gates@abc.edu", "Bill", "Gates", "b.gates@abc.edu", "123456", true, "instructor");
        sectionInstructor.setId(SECTION_INSTRUCTOR_ID);

        Course course = new Course("COSC 40993 Senior Design", "Senior design");
        course.setCourseAdmin(courseAdmin);
        Section section = new Section("2023-2024", LocalDate.of(2023, 8, 14), LocalDate.of(2024, 4, 29), true, DayOfWeek.MONDAY, LocalTime.of(23, 59), DayOfWeek.TUESDAY, LocalTime.of(23, 59));
        course.addSection(section);
        section.addInstructor(sectionInstructor);

        Student john = new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", true, "student");
        john.setId(STUDENT_ID);
        section.addStudent(john);
        this.john = john;

        assertThat(section.getInstructors()).doesNotContain(courseAdmin);
    }

    private AuthorizationResult authorizeAs(int callerId) {
        given(this.userUtils.getUserId()).willReturn(callerId);
        given(this.studentRepository.findById(STUDENT_ID)).willReturn(Optional.of(this.john));
        return this.manager.authorize(() -> null,
                new RequestAuthorizationContext(mock(HttpServletRequest.class), Map.of("studentId", String.valueOf(STUDENT_ID))));
    }

    @Test
    @DisplayName("Grants the course admin of the student's course who is not assigned to her course section")
    void grantsCourseAdminNotAssignedToTheSection() {
        AuthorizationResult result = authorizeAs(COURSE_ADMIN_ID);

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    @DisplayName("Grants an instructor assigned to the student's course section")
    void grantsSectionInstructor() {
        AuthorizationResult result = authorizeAs(SECTION_INSTRUCTOR_ID);

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    @DisplayName("Denies an instructor who neither teaches her course section nor owns her course (1a)")
    void deniesInstructorOfAnotherCourse() {
        AuthorizationResult result = authorizeAs(OTHER_COURSE_INSTRUCTOR_ID);

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    @DisplayName("Denies the student herself (1a)")
    void deniesTheStudentHerself() {
        AuthorizationResult result = authorizeAs(STUDENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    @DisplayName("Denies a non-numeric studentId without asking who the student is")
    void deniesNonNumericStudentId() {
        AuthorizationResult result = this.manager.authorize(() -> null,
                new RequestAuthorizationContext(mock(HttpServletRequest.class), Map.of("studentId", "abc")));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
        verifyNoInteractions(this.studentRepository, this.userUtils);
    }

}
