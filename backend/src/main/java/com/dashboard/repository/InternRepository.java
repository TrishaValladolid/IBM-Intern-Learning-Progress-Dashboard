package com.dashboard.repository;

import com.dashboard.entity.Intern;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class InternRepository {

    @PersistenceContext(unitName = "dashboardPU")
    private EntityManager em;

    public List<Intern> findAll() {
        return em.createQuery("SELECT i FROM Intern i", Intern.class).getResultList();
    }

    // Only ACTIVE interns, for the trainer-facing views. Legacy rows with a NULL
    // status predate the column and are treated as ACTIVE.
    public List<Intern> findAllActive() {
        return em.createQuery(
                "SELECT i FROM Intern i WHERE i.status = com.dashboard.entity.Intern$Status.ACTIVE OR i.status IS NULL",
                Intern.class)
                .getResultList();
    }

    // All interns in a training (batch), ordered by name for the roster sheet.
    public List<Intern> findByBatch(String batch) {
        return em.createQuery(
                "SELECT i FROM Intern i WHERE i.batch = :batch ORDER BY i.name", Intern.class)
                .setParameter("batch", batch)
                .getResultList();
    }

    // Active interns in a batch, ordered by name, for trainer rosters.
    public List<Intern> findByBatchActive(String batch) {
        return em.createQuery(
                "SELECT i FROM Intern i WHERE i.batch = :batch"
                        + " AND (i.status = com.dashboard.entity.Intern$Status.ACTIVE OR i.status IS NULL)"
                        + " ORDER BY i.name",
                Intern.class)
                .setParameter("batch", batch)
                .getResultList();
    }

    // Distinct trainings (batches) to populate the "Select Training" dropdown.
    public List<String> findDistinctBatches() {
        return em.createQuery(
                "SELECT DISTINCT i.batch FROM Intern i WHERE i.batch IS NOT NULL ORDER BY i.batch",
                String.class)
                .getResultList();
    }

    // Distinct batches limited to active interns, for the trainer dropdowns.
    public List<String> findDistinctActiveBatches() {
        return em.createQuery(
                "SELECT DISTINCT i.batch FROM Intern i WHERE i.batch IS NOT NULL"
                        + " AND (i.status = com.dashboard.entity.Intern$Status.ACTIVE OR i.status IS NULL)"
                        + " ORDER BY i.batch",
                String.class)
                .getResultList();
    }

    public Intern findById(Long id) {
        return em.find(Intern.class, id);
    }

    public Intern save(Intern intern) {
        if (intern.getId() == null) {
            em.persist(intern);
            return intern;
        } else {
            return em.merge(intern);
        }
    }

    public void deleteById(Long id) {
        Intern intern = em.find(Intern.class, id);
        if (intern != null) {
            em.remove(intern);
        }
    }
}
