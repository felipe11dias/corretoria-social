package com.corretoria.mapper;

import com.corretoria.entity.Address;
import com.corretoria.vo.v1.request.AddressRequest;
import com.corretoria.vo.v1.response.AddressResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AddressMapper {

    // id é do banco; user é a referência de volta, ligada pelo agregado User.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Address toEntity(AddressRequest request);

    List<Address> toEntities(List<AddressRequest> requests);

    AddressResponse toResponse(Address address);

    List<AddressResponse> toResponses(List<Address> addresses);
}
