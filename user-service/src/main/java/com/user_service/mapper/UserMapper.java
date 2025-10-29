package com.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.user_service.dto.CreateUser;
import com.user_service.dto.UserSignUpRequestDTO;
import com.user_service.model.Users;

@Mapper(componentModel = "spring", uses = {ProfileMapper.class})
public interface UserMapper {
	UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdOn", expression = "java(java.time.LocalDate.now())")
	@Mapping(target = "userProfile", source = "userProfile")
	Users mapUser(CreateUser createUser);
	UserSignUpRequestDTO mapToDto(Users entity);
	
}
