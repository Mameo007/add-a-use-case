package team.projectpulse.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;
    @Mock
    UserUtils userUtils;

    @InjectMocks
    StudentSecurityService studentSecurityService;

    Student john;


    @BeforeEach
    void setUp() {
        // Bingyang is the course admin of the course, but is not one of its course section's instructors
        Instructor courseAdmin = new Instructor("b.wei@abc.edu", "Bingyang", "Wei", "b.wei@abc.edu", "123456", true, "admin instructor");
        courseAdmin.setId(1);
        Instructor sectionInstructor = new Instructor("b.gates@abc.edu", "Bill", "Gates", "b.gates@abc.edu", "123456", true, "instructor");
        sectionInstructor.setId(2);

        Course course = new Course("COSC 40993 Senior Design", "Senior design");
        course.setCourseAdmin(courseAdmin);

        Section section = new Section("2023-2024", LocalDate.of(2023, 8, 14), LocalDate.of(2024, 4, 29), true, DayOfWeek.MONDAY, LocalTime.of(23, 59), DayOfWeek.TUESDAY, LocalTime.of(23, 59));
        course.addSection(section);
        section.addInstructor(sectionInstructor);

        this.john = new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", true, "student");
        this.john.setId(4);
        section.addStudent(this.john);
    }

    @Test
    @DisplayName("The course admin of the student's course is recognized even when she is not assigned to the course section")
    void courseAdminNotAssignedToTheSectionIsGranted() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.isCurrentUserCourseAdminOfStudent(4)).isTrue();
        assertThat(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(4)).isFalse();
    }

    @Test
    @DisplayName("An instructor who is not the course admin of the student's course is not recognized as her course admin")
    void instructorOfAnotherCourseIsNotCourseAdmin() {
        given(this.userUtils.getUserId()).willReturn(3);
        given(this.studentRepository.findById(4)).willReturn(Optional.of(this.john));

        assertThat(this.studentSecurityService.isCurrentUserCourseAdminOfStudent(4)).isFalse();
    }

    @Test
    @DisplayName("An id that names no student is denied rather than throwing")
    void unknownStudentIsDenied() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(99)).willReturn(Optional.empty());

        assertThat(this.studentSecurityService.isCurrentUserCourseAdminOfStudent(99)).isFalse();
    }

}
