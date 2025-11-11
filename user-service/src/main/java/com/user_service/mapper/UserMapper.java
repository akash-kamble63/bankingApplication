package com.user_service.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.user_service.dto.CreateUserRequest;
import com.user_service.dto.UserResponse;
import com.user_service.model.Profile;
import com.user_service.model.User;

@Mapper(componentModel = "spring", uses = {ProfileMapper.class})
public interface UserMapper {

	@Mapping(target = "id", ignore = true)
    @Mapping(target = "username", source = "emailId")
    @Mapping(target = "email", source = "emailId")
    @Mapping(target = "contactNo", source = "contactNumber")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "authId", ignore = true)  // Set after Keycloak creation
    @Mapping(target = "identificationNumber", expression = "java(generateIdentificationNumber())")
    @Mapping(target = "profile", source = "request", qualifiedByName = "mapProfile")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toEntity(CreateUserRequest request);
	
	@Mapping(target = "userId", source = "id")
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "contactNumber", source = "contactNo")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "emailVerified", expression = "java(user.getEmailVerifiedAt() != null)")
    UserResponse toResponse(User user);
	
	 @Named("mapProfile")
	    default Profile mapProfile(CreateUserRequest request) {
	        return Profile.builder()
	                .firstName(request.getFirstName())
	                .lastName(request.getLastName())
	                .build();
	    }
	 
	 
	 /**
	     * Generate unique identification number
	     */
	    default String generateIdentificationNumber() {
	        return "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	    }

	
}
