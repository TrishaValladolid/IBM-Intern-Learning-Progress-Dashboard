package com.dashboard.repository;

import com.dashboard.entity.Submission;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class SubmissionRepository {

    @PersistenceContext(unitName = "dashboardPU")
    private EntityManager em;

    // Persist a new submission or merge an edited one, mirroring the other
    // repositories. This lets recording a score update an existing grade
    // instead of inserting a duplicate row.
    public Submission save(Submission submission) {
        if (submission.getId() == null) {
            em.persist(submission);
            return submission;
        }
        return em.merge(submission);
    }

    public List<Submission> findByInternId(Long internId) {
        // JOIN FETCH loads the related Assignment in the same query,
        // avoiding a LazyInitializationException when callers read
        // submission.getAssignment() after this method returns.
        return em.createQuery(
                "SELECT s FROM Submission s JOIN FETCH s.assignment WHERE s.intern.id = :internId",
                Submission.class)
                .setParameter("internId", internId)
                .getResultList();
    }

    // All submissions with intern + assignment eagerly loaded, for building the
    // Assignments and Grades matrix (every intern x every assignment) in one call.
    public List<Submission> findAll() {
        return em.createQuery(
                "SELECT s FROM Submission s JOIN FETCH s.intern JOIN FETCH s.assignment",
                Submission.class)
                .getResultList();
    }

    // Locate an existing grade for one intern on one assignment, so recording a
    // score can update in place instead of inserting a duplicate row.
    public Submission findByInternIdAndAssignmentId(Long internId, Long assignmentId) {
        return em.createQuery(
                "SELECT s FROM Submission s WHERE s.intern.id = :internId "
                        + "AND s.assignment.id = :assignmentId",
                Submission.class)
                .setParameter("internId", internId)
                .setParameter("assignmentId", assignmentId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}