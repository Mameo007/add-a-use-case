package team.projectpulse.student;

/**
 * The outcome of deactivating a student (UC-STU-deactivate-student): the student as she now stands, and whether she
 * was already deactivated before the request (extension 6a), in which case nothing was changed.
 */
public record DeactivationResult(Student student, boolean alreadyDeactivated) {
}
