package com.dashboard.repository;

import com.dashboard.entity.Training;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class TrainingRepository {

    @PersistenceContext(unitName = "dashboardPU")
    private EntityManager em;

    // All trainings for one intern, ordered by id so newest additions sit last.
    public List<Training> findByInternId(Long internId) {
        return em.createQuery(
                "SELECT t FROM Training t WHERE t.intern.id = :internId ORDER BY t.id",
                Training.class)
                .setParameter("internId", internId)
                .getResultList();
    }

    // Distinct training names across every intern, for populating the
    // "assign this assignment to a training" dropdown. Case-insensitive de-dup
    // keeps one label per training even if casing drifted between batches.
    public List<String> findDistinctTrainingNames() {
        return em.createQuery(
                "SELECT DISTINCT t.trainingName FROM Training t "
                        + "WHERE t.trainingName IS NOT NULL AND t.trainingName <> '' "
                        + "ORDER BY t.trainingName",
                String.class)
                .getResultList();
    }

    public Training findById(Long id) {
        return em.find(Training.class, id);
    }

    public Training save(Training training) {
        if (training.getId() == null) {
            em.persist(training);
            return training;
        } else {
            return em.merge(training);
        }
    }

    public void deleteById(Long id) {
        Training training = em.find(Training.class, id);
        if (training != null) {
            em.remove(training);
        }
    }
}
