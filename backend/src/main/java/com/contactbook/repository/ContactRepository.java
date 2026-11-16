package com.contactbook.repository;

import com.contactbook.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query(value = """
            select distinct c from Contact c
            left join c.methods m
            where (:keyword is null
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(c.company) like lower(concat('%', :keyword, '%'))
                or lower(c.jobTitle) like lower(concat('%', :keyword, '%'))
                or lower(c.note) like lower(concat('%', :keyword, '%'))
                or lower(m.methodValue) like lower(concat('%', :keyword, '%')))
              and (:favorite is null or c.favorite = :favorite)
            """,
            countQuery = """
            select count(distinct c) from Contact c
            left join c.methods m
            where (:keyword is null
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(c.company) like lower(concat('%', :keyword, '%'))
                or lower(c.jobTitle) like lower(concat('%', :keyword, '%'))
                or lower(c.note) like lower(concat('%', :keyword, '%'))
                or lower(m.methodValue) like lower(concat('%', :keyword, '%')))
              and (:favorite is null or c.favorite = :favorite)
            """)
    Page<Contact> search(
            @Param("keyword") String keyword,
            @Param("favorite") Boolean favorite,
            Pageable pageable
    );

    @Query("""
            select distinct c from Contact c
            left join c.methods m
            where (:keyword is null
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(c.company) like lower(concat('%', :keyword, '%'))
                or lower(c.jobTitle) like lower(concat('%', :keyword, '%'))
                or lower(c.note) like lower(concat('%', :keyword, '%'))
                or lower(m.methodValue) like lower(concat('%', :keyword, '%')))
              and (:favorite is null or c.favorite = :favorite)
            order by c.updatedAt desc
            """)
    List<Contact> searchAll(
            @Param("keyword") String keyword,
            @Param("favorite") Boolean favorite
    );
}
