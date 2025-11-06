package com.user_service.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.user_service.enums.UserStatus;
import com.user_service.model.Profile;
import com.user_service.model.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class UserSpecification {

	public static Specification<User> filterUsers(
            String email,
            String firstName,
            String lastName,
            List<UserStatus> statuses,
            String contactNumber,
            Boolean emailVerified,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            String nationality,
            String occupation,
            String gender) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            //to avoid n+1 queries
            if (query != null) {
                query.distinct(true);
                root.fetch("profile", JoinType.LEFT);
            }
            // Email filter (contains)
            if (email != null && !email.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")),
                    "%" + email.toLowerCase() + "%"
                ));
            }
            
            // Status filter (multiple)
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            
            // Contact number filter
            if (contactNumber != null && !contactNumber.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    root.get("contactNo"),
                    "%" + contactNumber + "%"
                ));
            }
            
            // Email verified filter
            if (emailVerified != null) {
                if (emailVerified) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("emailVerifiedAt")));
                } else {
                    predicates.add(criteriaBuilder.isNull(root.get("emailVerifiedAt")));
                }
            }
            
            // Date range filters
            if (createdAfter != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("createdAt"), createdAfter
                ));
            }
            
            if (createdBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("createdAt"), createdBefore
                ));
            }
            
            // Profile filters (join)
            Join<User, Profile> profileJoin = root.join("profile");
            
            if (firstName != null && !firstName.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(profileJoin.get("firstName")),
                    "%" + firstName.toLowerCase() + "%"
                ));
            }
            
            if (lastName != null && !lastName.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(profileJoin.get("lastName")),
                    "%" + lastName.toLowerCase() + "%"
                ));
            }
            
            if (nationality != null && !nationality.isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    profileJoin.get("nationality"), nationality
                ));
            }
            
            if (occupation != null && !occupation.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(profileJoin.get("occupation")),
                    "%" + occupation.toLowerCase() + "%"
                ));
            }
            
            if (gender != null && !gender.isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    profileJoin.get("gender"), gender
                ));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
