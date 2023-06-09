package ru.practicum.shareit.request.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.service.RequestService;

import javax.validation.constraints.Min;
import java.util.List;

@RestController
@Slf4j
@Validated
@RequestMapping(path = "/requests")
public class RequestController {
    private final RequestService requestService;

    @Autowired
    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ItemRequestResponseDto addRequest(@RequestBody @Validated ItemRequestRequestDto itemRequestRequestDto,
                                             @RequestHeader(value = "X-Sharer-User-Id") @Min(value = 1,
                                                     message = "User id should be more than 0") Long requesterId) {

        log.info("Request {} received from user id {}", itemRequestRequestDto, requesterId);
        itemRequestRequestDto.setRequesterId(requesterId);
        ItemRequestResponseDto savedRequest = requestService.addRequest(itemRequestRequestDto);
        log.info("Request saved: {}.", savedRequest);
        return savedRequest;
    }

    @GetMapping("/{requestId}")
    public ItemRequestResponseDto findRequest(@PathVariable Long requestId,
                                              @RequestHeader(value = "X-Sharer-User-Id") @Min(value = 1,
                                                      message = "User id should be more than 0") Long userId) {
        log.info("Looking for request id {} by user {}", requestId, userId);
        ItemRequestResponseDto itemRequestRequestDto = requestService.findRequest(requestId, userId);
        log.info("Request found: {}", itemRequestRequestDto);
        return itemRequestRequestDto;
    }

    @GetMapping
    public List<ItemRequestResponseDto> getOwnRequests(@RequestHeader(value = "X-Sharer-User-Id") @Min(value = 1,
            message = "User id should be more than 0") Long userId,
                                                       @RequestParam(defaultValue = "0") @Min(value = 0,
                                                               message = "Parameter 'from' must be more than 0") int from,
                                                       @RequestParam(defaultValue = "10") @Min(value = 0,
                                                               message = "Parameter 'size' must be more than 0") int size) {

        log.info("Looking for requests from user id {}. Paging from {}, size {}.", userId, from, size);
        List<ItemRequestResponseDto> requests = requestService.findUserRequest(userId, from, size);
        log.info("Requests found: {}.", requests);
        return requests;
    }

    @GetMapping("/all")
    public List<ItemRequestResponseDto> getRequests(
            @RequestHeader(value = "X-Sharer-User-Id") @Min(value = 1,
                    message = "User id should be more than 0") Long userId,
            @RequestParam(defaultValue = "0") @Min(value = 0,
                    message = "Parameter 'from' must be more than 0") int from,
            @RequestParam(defaultValue = "10") @Min(value = 0,
                    message = "Parameter 'size' must be more than 0") int size) {

        log.info("Looking for all requests from {}, size {}", from, size);
        List<ItemRequestResponseDto> requests = requestService.findAllRequests(userId, from, size);
        log.info("Requests found: {}", requests);
        return requests;
    }
}