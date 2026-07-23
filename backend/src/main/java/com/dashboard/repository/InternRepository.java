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
