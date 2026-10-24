package com.kostasanagno.his.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

/**
 * Id column plus id based, Hibernate-proxy safe equals/hashCode. Extend {@link BaseEntity} for regular
 * entities (adds audit columns); extend this directly only for tiny lookup tables without audit columns.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class IdentifiedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || effectiveClass(this) != effectiveClass(o)) {
            return false;
        }
        IdentifiedEntity that = (IdentifiedEntity) o;
        return id != null && Objects.equals(id, that.getId());
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    private static Class<?> effectiveClass(Object o) {
        return o instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
    }
}
