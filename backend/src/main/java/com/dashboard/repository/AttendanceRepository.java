package com.dashboard.repository;

import com.dashboard.entity.Attendance;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.List;

@Stateless
public class AttendanceRepository {

    @PersistenceContext(unitName = "dashboardPU")
    private EntityManager em;

    // JOIN FETCH loads the related Intern in the same query, avoiding a
    // LazyInitializationException when callers read attendance.getIntern()
    // after this method returns.
    public List<Attendance> findAll() {
        return em.createQuery(
                "SELECT a FROM Attendance a JOIN FETCH a.intern ORDER BY a.date DESC, a.id DESC",
                Attendance.class)
                .getResultList();
    }

    public List<Attendance> findByInternId(Long internId) {
        return em.createQuery(
                "SELECT a FROM Attendance a JOIN FETCH a.intern WHERE a.intern.id = :internId "
                        + "ORDER BY a.date DESC, a.id DESC",
                Attendance.class)
                .setParameter("internId", internId)
                .getResultList();
    }

    public Attendance findById(Long id) {
        return em.find(Attendance.class, id);
    }

    // Existing records for a training on a given date, used to prefill the roster
    // and to update in place (rather than duplicate) on save.
    public List<Attendance> findByBatchAndDate(String batch, LocalDate date) {
        return em.createQuery(
                "SELECT a FROM Attendance a JOIN FETCH a.intern "
                        + "WHERE a.intern.batch = :batch AND a.date = :date",
                Attendance.class)
                .setParameter("batch", batch)
                .setParameter("date", date)
                .getResultList();
    }

    // Duplicate prevention: one record per intern per calendar date.
    public boolean existsByInternAndDate(Long internId, LocalDate date) {
        Long count = em.createQuery(
                "SELECT COUNT(a) FROM Attendance a WHERE a.intern.id = :internId AND a.date = :date",
                Long.class)
                .setParameter("internId", internId)
                .setParameter("date", date)
                .getSingleResult();
        return count != null && count > 0;
    }

    // Dynamic summary counts, computed in the database.
    public long countByStatus(Attendance.Status status) {
        return em.createQuery(
                "SELECT COUNT(a) FROM Attendance a WHERE a.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    public long countByInternIdAndStatus(Long internId, Attendance.Status status) {
        return em.createQuery(
                "SELECT COUNT(a) FROM Attendance a WHERE a.intern.id = :internId AND a.status = :status",
                Long.class)
                .setParameter("internId", internId)
                .setParameter("status", status)
                .getSingleResult();
    }

    public Attendance save(Attendance attendance) {
        if (attendance.getId() == null) {
            em.persist(attendance);
            return attendance;
        } else {
            return em.merge(attendance);
        }
    }
}
