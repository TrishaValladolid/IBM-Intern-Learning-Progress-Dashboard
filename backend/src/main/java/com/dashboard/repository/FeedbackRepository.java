package com.dashboard.repository;

import com.dashboard.entity.Feedback;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class FeedbackRepository {

    @PersistenceContext(unitName = "dashboardPU")
    private EntityManager em;

    // Newest first: the profile page shows the most recent feedback at the top.
    // JOIN FETCH the intern so the resource can apply canView()/ownership checks
    // without tripping a LazyInitializationException after the transaction ends.
    public List<Feedback> findByInternId(Long internId) {
        return em.createQuery(
                        "SELECT f FROM Feedback f JOIN FETCH f.intern "
                                + "WHERE f.intern.id = :internId "
                                + "ORDER BY f.createdAt DESC, f.id DESC",
                        Feedback.class)
                .setParameter("internId", internId)
                .getResultList();
    }

    public Feedback findById(Long id) {
        return em.find(Feedback.class, id);
    }

    public Feedback save(Feedback feedback) {
        if (feedback.getId() == null) {
            em.persist(feedback);
            return feedback;
        } else {
            return em.merge(feedback);
        }
    }

    public void deleteById(Long id) {
        Feedback feedback = em.find(Feedback.class, id);
        if (feedback != null) {
            em.remove(feedback);
        }
    }
}
