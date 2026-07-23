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
