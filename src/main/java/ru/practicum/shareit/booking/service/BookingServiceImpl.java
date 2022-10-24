package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingReturnDto;
import ru.practicum.shareit.exception.EntityNotAvailableException;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserMapper;
import ru.practicum.shareit.user.service.UserService;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemService itemService;
    private final UserService userService;

    @Override
    @Transactional
    public BookingReturnDto addBooking(BookingDto bookingDto, Long userId) {
        validateBookingTime(bookingDto);
        Item item = itemService.getItemById(bookingDto.getItemId());
        if (!item.getAvailable()) {
            throw new EntityNotAvailableException("Предмет недоступен");
        }
        if (item.getUserId().equals(userId)) {
            throw new EntityNotFoundException("Невозможно забронировать свой предмет");
        }
        UserDto booker = userService.getUserById(userId);
        Booking booking = BookingMapper.toBooking(bookingDto);
        booking.setItem(item);
        booking.setBooker(UserMapper.toUser(booker));
        userService.getUserById(userId);
        booking.setStatus(BookingStatus.WAITING);
        return BookingMapper.toBookingReturnDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingReturnDto patchBooking(Long bookingId, Long userId, boolean approved) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> {
            throw new EntityNotFoundException("Бронирование не найдено");
        });

        if (approved) {
            if (!booking.getItem().getUserId().equals(userId)) {
                throw new EntityNotFoundException("статус бронирования может менять только владелец вещи");
            }
            if (booking.getStatus().equals(BookingStatus.APPROVED)) {
                throw new IllegalStateException("Бронирование уже подтверждено");
            }
            booking.setStatus(BookingStatus.APPROVED);
        }
        if (booking.getItem().getUserId().equals(userId) && !approved) {
            booking.setStatus(BookingStatus.REJECTED);
        }
        return BookingMapper.toBookingReturnDto(bookingRepository.save(booking));
    }

    @Override
    public BookingReturnDto getBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> {
            throw new EntityNotFoundException("Бронирование не найдено");
        });

        if (!booking.getItem().getUserId().equals(userId) && !booking.getBooker().getId().equals(userId)) {
            throw new EntityNotFoundException("Пользователь не является владельцем вещи или автором бронирования");
        }
        return BookingMapper.toBookingReturnDto(booking);
    }

    @Override
    public List<BookingReturnDto> getUserBookingList(Long userId, String state) {
        userService.getUserById(userId);
        switch (state) {
            case "FUTURE":
                return bookingRepository.findFutureBookingsByBooker(userId, LocalDateTime.now())
                        .stream()
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "ALL":
                return bookingRepository.findBookingsByBooker(userId).stream()
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "WAITING":
                return bookingRepository
                        .findBookingsByBookerAndStatus(userId, BookingStatus.WAITING).stream()
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "REJECTED":
                return bookingRepository
                        .findBookingsByBookerAndStatus(userId, BookingStatus.REJECTED).stream()
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "CURRENT":
                return bookingRepository.findCurrentBookingForUser(userId, LocalDateTime.now()).stream()
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "PAST":
                return bookingRepository.findPastBookingForUser(userId, LocalDateTime.now()).stream()
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            default:
                throw new EntityNotAvailableException("Unknown state: UNSUPPORTED_STATUS");
        }
    }

    @Override
    public List<BookingReturnDto> getOwnerBookingList(Long userId, String state) {
        userService.getUserById(userId);
        Stream<Long> itemsId = itemService.getItems(userId).stream().map(ItemDto::getId);
        switch (state) {
            case "FUTURE":
                return itemsId.flatMap((id) ->
                                bookingRepository.findFutureBookingsByItem(
                                        id, LocalDateTime.now()).stream())
                        .map(BookingMapper::toBookingReturnDto)
                        .sorted((t1, t2) -> -t1.getStart().compareTo(t2.getStart()))
                        .collect(Collectors.toList());

            case "ALL":
                return itemsId.flatMap((id) -> bookingRepository.findBookingsByItem(id).stream())
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "WAITING":
                return itemsId.flatMap((id) -> bookingRepository
                                .findBookingsByItemAndStatus(id, BookingStatus.WAITING).stream())
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "REJECTED":
                return itemsId.flatMap((id) -> bookingRepository
                                .findBookingsByItemAndStatus(id, BookingStatus.REJECTED).stream())
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "CURRENT":
                return itemsId.flatMap((id) -> bookingRepository
                                .findCurrentBookingForOwner(id, LocalDateTime.now()).stream())
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            case "PAST":
                return itemsId.flatMap((id) -> bookingRepository
                                .findPastBookingForOwner(id, LocalDateTime.now()).stream())
                        .map(BookingMapper::toBookingReturnDto)
                        .collect(Collectors.toList());
            default:
                throw new EntityNotAvailableException("Unknown state: UNSUPPORTED_STATUS");
        }
    }

    private void validateBookingTime(BookingDto bookingDto) {
        if (bookingDto.getStart().isAfter(bookingDto.getEnd())) {
            throw new IllegalStateException("Дата начала бронирования не может быть позже даты завершения");
        }
    }
}
