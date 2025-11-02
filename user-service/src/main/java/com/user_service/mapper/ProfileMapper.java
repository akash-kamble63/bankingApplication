package com.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.user_service.dto.ProfileDto;
import com.user_service.model.Profile;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

	/**
     * Map ProfileDto to Profile entity
     */
    @Mapping(target = "id", ignore = true)
    Profile toEntity(ProfileDto profileDto);
    
    /**
     * Map Profile entity to ProfileDto
     */
    ProfileDto toDto(Profile profile);
}
