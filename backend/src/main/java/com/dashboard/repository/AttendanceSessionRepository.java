package com.dashboard.repository;

import com.dashboard.entity.AttendanceSession;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;

@Stateless
public class AttendanceSessionRepository {

    @PersistenceContext(unitName = "dashboardPU")
    private EntityManager em;

    // Locate the existing session for a training/date so records can be reused
    // (edited) rather than duplicated. Returns null when none exists yet.
    public AttendanceSession findByBatchAndDate(String batch, LocalDate date) {
        try {
            return em.createQuery(
                    "SELECT s FROM AttendanceSession s WHERE s.batch = :batch AND s.date = :date",
                    AttendanceSession.class)
                    .setParameter("batch", batch)
                    .setParameter("date", date)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public AttendanceSession save(AttendanceSession session) {
        if (session.getId() == null) {
            em.persist(session);
            return session;
        } else {
            return em.merge(session);
        }
    }
}
