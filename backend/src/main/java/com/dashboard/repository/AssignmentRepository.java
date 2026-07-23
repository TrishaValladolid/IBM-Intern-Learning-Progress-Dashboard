package com.dashboard.repository;

import com.dashboard.entity.Assignment;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class AssignmentRepository {

    @PersistenceContext(unitName = "dashboardPU")
    private EntityManager em;

    public List<Assignment> findAll() {
        return em.createQuery("SELECT a FROM Assignment a", Assignment.class).getResultList();
    }

    public Assignment findById(Long id) {
        return em.find(Assignment.class, id);
    }

    public Assignment save(Assignment assignment) {
        if (assignment.getId() == null) {
            em.persist(assignment);
            return assignment;
        } else {
            return em.merge(assignment);
        }
    }

    public void deleteById(Long id) {
        Assignment assignment = em.find(Assignment.class, id);
        if (assignment != null) {
            em.remove(assignment);
        }
    }
}
