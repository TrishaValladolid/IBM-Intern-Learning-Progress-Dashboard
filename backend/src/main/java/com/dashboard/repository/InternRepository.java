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

    // All interns in a training (batch), ordered by name for the roster sheet.
    public List<Intern> findByBatch(String batch) {
        return em.createQuery(
                "SELECT i FROM Intern i WHERE i.batch = :batch ORDER BY i.name", Intern.class)
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
