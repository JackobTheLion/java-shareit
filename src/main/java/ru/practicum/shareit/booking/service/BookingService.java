package ru.practicum.shareit.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exceptions.BookingNotAloudException;
import ru.practicum.shareit.booking.exceptions.BookingNotFoundException;
import ru.practicum.shareit.booking.exceptions.ItemNotAvailableException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exceptions.ValidationException;
import ru.practicum.shareit.item.exceptions.ItemNotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.exceptions.UserNotFoundException;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.shareit.booking.model.Status.*;

@Service
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, ItemRepository itemRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public BookingDto createBooking(BookingDto bookingDto, Long bookerId) {
        log.info("Adding booking: {} by user {}", bookingDto, bookerId);
        Booking booking = BookingMapper.mapFromDto(bookingDto, bookerId, WAITING);
        log.info("Booking mapped: {}.", booking);

        if (booking.getEndDate().before(booking.getStartDate()) || booking.getStartDate().equals(booking.getEndDate())) {
            log.error("Booking start date should be before booking end date");
            throw new ValidationException("Booking start date should be before booking end date");
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        if (booking.getEndDate().before(now) || booking.getStartDate().before(now)) {
            log.error("Booking cannot start or end in past");
            throw new ValidationException("Booking cannot start or end in past");
        }

        Item item = itemRepository.findById(booking.getItem().getId()).orElseThrow(() -> {
            log.error("Item id {} not found", booking.getItem().getId());
            return new ItemNotFoundException(String.format("Item id %s not found", booking.getItem().getId()));
        });

        if (!item.getIsAvailable()) {
            log.error("Item id {} not available", booking.getItem().getId());
            throw new ItemNotAvailableException(String.format("Item id %s not available", booking.getItem().getId()));
        }

        if (item.getOwnerId().equals(booking.getBooker().getId())) {
            log.error("Booking own item is not aloud.");
            throw new BookingNotAloudException("Booking own item is not aloud.");
        }

        User user = getUser(booking.getBooker().getId());

        if (!isAvailableToBook(booking)) {
            throw new ItemNotAvailableException("Item is already booked for this period.");
        }

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking saved: {}", savedBooking);
        BookingDto savedBookingDto = BookingMapper.mapToDto(savedBooking, user, item);
        log.info("Booking mapped to DTO: {}", savedBookingDto);
        return savedBookingDto;
    }

    public BookingDto findBooking(Long bookingId, Long userId) {
        log.info("Looking for booking id {} by user id {}", bookingId, userId);
        getUser(userId);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> {
            log.error("Booking id {} not found.", bookingId);
            return new BookingNotFoundException(String.format("Booking id %s not found.", bookingId));
        });
        log.info("Booking found: {}.", booking);

        if (!(booking.getItem().getOwnerId().equals(userId) || booking.getBooker().getId().equals(userId))) {
            log.error("User id {} has no access to booking id {}", userId, booking);
            throw new BookingNotFoundException(String.format("Booking id %s not found.", bookingId));
        }
        BookingDto bookingDto = BookingMapper.mapToDto(booking);
        log.info("Booking mapped to DTO: {}", bookingDto);
        return bookingDto;
    }

    public List<BookingDto> getUserBookings(Long userId, String state, int from, int size) {
        log.info("Looking for bookings of user {} with status {}", userId, state);
        getUser(userId);
        Page<Booking> bookings;
        PageRequest page = PageRequest.of(from > 0 ? from / size : 0, size);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        log.info("Now is: {}.", now);
        switch (state) {
            case "ALL":
                bookings = bookingRepository.findByBookerIdOrderByStartDateDesc(userId, page);
                break;
            case "CURRENT":
                bookings = bookingRepository.findByBookerIdAndStartDateBeforeAndEndDateAfterOrderByStartDateDesc(userId,
                        now, now, page);
                break;
            case "PAST":
                bookings = bookingRepository.findByBookerIdAndEndDateBeforeOrderByStartDateDesc(userId, now, page);
                break;
            case "FUTURE":
                bookings = bookingRepository.findByBookerIdAndStartDateAfterOrderByStartDateDesc(userId, now, page);
                break;
            case "WAITING":
                bookings = bookingRepository.findByBookerIdAndStatusEqualsOrderByStartDateDesc(userId, WAITING, page);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByBookerIdAndStatusEqualsOrderByStartDateDesc(userId, REJECTED, page);
                break;
            default:
                log.error("Incorrect 'state' value: {}", state);
                throw new ValidationException("Unknown state: UNSUPPORTED_STATUS");
        }
        return bookings.map(BookingMapper::mapToDto).getContent();
    }

    public List<BookingDto> getOwnerBooking(Long userId, String state, int from, int size) {
        log.info("Looking for bookings of owner {} with status {}", userId, state);
        getUser(userId);
        Page<Booking> bookings;
        PageRequest page = PageRequest.of(from > 0 ? from / size : 0, size);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        log.info("Now is: {}.", now);
        switch (state) {
            case "ALL":
                bookings = bookingRepository.findByItemOwnerIdOrderByStartDateDesc(userId, page);
                break;
            case "CURRENT":
                bookings = bookingRepository.findByItemOwnerIdAndStartDateBeforeAndEndDateAfterOrderByStartDateDesc(
                        userId, now, now, page);
                break;
            case "PAST":
                bookings = bookingRepository.findByItemOwnerIdAndEndDateBeforeOrderByStartDateDesc(userId, now, page);
                break;
            case "FUTURE":
                bookings = bookingRepository.findByItemOwnerIdAndStartDateAfterOrderByStartDateDesc(userId, now, page);
                break;
            case "WAITING":
                bookings = bookingRepository.findByItemOwnerIdAndStatusEqualsOrderByStartDateDesc(userId, WAITING, page);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByItemOwnerIdAndStatusEqualsOrderByStartDateDesc(userId, REJECTED, page);
                break;
            default:
                log.error("Incorrect state value: {}", state);
                throw new ValidationException("Unknown state: UNSUPPORTED_STATUS");
        }
        return bookings.map(BookingMapper::mapToDto).getContent();
    }

    @Transactional
    public BookingDto approveBooking(Long userId, Boolean approved, Long bookingId) {
        log.info("Updating booking id {} as {} by user id {}", bookingId, approved, userId);
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> {
            log.error("Booking id {} not found.", bookingId);
            return new BookingNotFoundException(String.format("Booking id %s not found.", bookingId));
        });
        if (!booking.getItem().getOwnerId().equals(userId)) {
            log.error("User id {} has no access to booking id {}", userId, booking);
            throw new BookingNotFoundException(String.format("Booking id %s not found.", bookingId));
        }

        if (!booking.getStatus().equals(APPROVED)) {
            if (approved) {
                booking.setStatus(APPROVED);
                log.info("Booking status set APPROVED. Item is no longer available");
            } else {
                booking.setStatus(REJECTED);
                log.info("Booking status set rejected. Item is no longer available");
            }
        } else {
            log.error("Booking id {} already approved", bookingId);
            throw new ItemNotAvailableException(String.format("Booking id %s already approved", bookingId));
        }
        return BookingMapper.mapToDto(booking);
    }

    private boolean isAvailableToBook(Booking booking) {
        List<Booking> bookings = bookingRepository.findByItemId(booking.getItem().getId());
        for (Booking b : bookings) {
            if (!(booking.getEndDate().before(b.getStartDate()) || booking.getStartDate().after(b.getEndDate()))) {
                log.info("Booking not available. Overlap with {}", b);
                return false;
            }
        }
        return true;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> {
            log.error("User id {} not found", userId);
            return new UserNotFoundException(String.format("User id %s not found", userId));
        });
    }
}
