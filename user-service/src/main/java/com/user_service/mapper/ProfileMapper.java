package com.user_service.mapper;

import org.mapstruct.Mapper;

import com.user_service.dto.ProfileDto;
import com.user_service.model.Profile;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

	Profile maptoEntity(ProfileDto profileDto);
	Profile mapToDto(Profile profile);
}
