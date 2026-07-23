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

    public Submission save(Submission submission) {
        em.persist(submission);
        return submission;
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
}