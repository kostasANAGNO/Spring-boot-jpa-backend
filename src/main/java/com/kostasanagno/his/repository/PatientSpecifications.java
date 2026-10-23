package com.kostasanagno.his.repository;

import com.kostasanagno.his.dto.PatientSearchCriteria;
import com.kostasanagno.his.entity.Patient;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PatientSpecifications {

    private static final char ESCAPE = '\\';

    private PatientSpecifications() {
    }

    /** Active patients matching every non-blank criterion. */
    public static Specification<Patient> from(PatientSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (criteria != null) {
                if (StringUtils.hasText(criteria.name())) {
                    // every word must match the first or the last name
                    for (String token : criteria.name().trim().split("\\s+")) {
                        predicates.add(cb.or(
                                contains(cb, root.get("firstName"), token),
                                contains(cb, root.get("lastName"), token)));
                    }
                }
                if (StringUtils.hasText(criteria.nationalId())) {
                    predicates.add(contains(cb, root.get("nationalId"), criteria.nationalId().trim()));
                }
                if (StringUtils.hasText(criteria.phone())) {
                    predicates.add(contains(cb, root.get("phone"), criteria.phone().trim()));
                }
                if (StringUtils.hasText(criteria.email())) {
                    predicates.add(contains(cb, root.get("email"), criteria.email().trim()));
                }
                if (criteria.gender() != null) {
                    predicates.add(cb.equal(root.get("gender"), criteria.gender()));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate contains(CriteriaBuilder cb, Expression<String> field, String term) {
        String escaped = term.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return cb.like(cb.lower(field), "%" + escaped + "%", ESCAPE);
    }
}
