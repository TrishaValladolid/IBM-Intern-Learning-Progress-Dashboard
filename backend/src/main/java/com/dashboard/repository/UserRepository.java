package com.dashboard.repository;

import com.dashboard.entity.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class UserRepository {

    @PersistenceContext(unitName = "dashboardPU")
    private EntityManager em;

    public User findByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u ORDER BY u.id", User.class).getResultList();
    }

    public User findById(Long id) {
        return em.find(User.class, id);
    }

    public void deleteById(Long id) {
        User user = em.find(User.class, id);
        if (user != null) {
            em.remove(user);
        }
    }

    public boolean existsByUsername(String username) {
        Long matches = em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE LOWER(u.username) = LOWER(:username)", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return matches > 0;
    }

    public User save(User user) {
        if (user.getId() == null) {
            em.persist(user);
            em.flush();
            em.refresh(user);
            return user;
        }
        User merged = em.merge(user);
        em.flush();
        em.refresh(merged);
        return merged;
    }

    public long count() {
        return em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
    }
}
