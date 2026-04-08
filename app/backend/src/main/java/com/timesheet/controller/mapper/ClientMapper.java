package com.timesheet.controller.mapper;

import com.timesheet.dto.ClientResponse;
import com.timesheet.model.Client;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientResponse toClientResponse(Client client);

    List<ClientResponse> toClientResponseList(List<Client> clients);
}
