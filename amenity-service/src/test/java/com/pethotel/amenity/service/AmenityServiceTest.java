package com.pethotel.amenity.service;

import com.pethotel.amenity.dto.AmenityDto;
import com.pethotel.amenity.dto.AmenityRequest;
import com.pethotel.amenity.entity.Amenity;
import com.pethotel.amenity.repository.AmenityRepository;
import com.pethotel.common.enums.ServiceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Unit-тест AmenityService: все зависимости замокированы.
// MultipartFile также мокируется — не нужен реальный файл на диске.
@ExtendWith(MockitoExtension.class)
class AmenityServiceTest {

    @Mock AmenityRepository amenityRepository;
    @Mock MultipartFile multipartFile; // Spring-абстракция загружаемого файла; мокируем для uploadImage-тестов
    @InjectMocks AmenityService amenityService;

    // ── getAll / getById / getByType ─────────────────────────────────────────────

    // getAll маппит entities → DTO, сохраняет порядок и тип.
    @Test
    void getAll_returnsMappedList() {
        when(amenityRepository.findAll()).thenReturn(List.of(
                amenity(1L, "Sauna", ServiceType.SAUNA),
                amenity(2L, "Pool", ServiceType.POOL)));

        List<AmenityDto> result = amenityService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(ServiceType.SAUNA);
        assertThat(result.get(1).getType()).isEqualTo(ServiceType.POOL);
    }

    @Test
    void getById_returnsDto() {
        when(amenityRepository.findById(1L)).thenReturn(Optional.of(amenity(1L, "Sauna", ServiceType.SAUNA)));

        AmenityDto result = amenityService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Sauna");
    }

    // Несуществующая услуга → NoSuchElementException с id в сообщении → 404 через GlobalExceptionHandler.
    @Test
    void getById_notFound_throwsNoSuchElement() {
        when(amenityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> amenityService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // getByType — вызывает кастомный метод репозитория findByType(type).
    @Test
    void getByType_filtersByType() {
        when(amenityRepository.findByType(ServiceType.MASSAGE))
                .thenReturn(List.of(amenity(3L, "Massage", ServiceType.MASSAGE)));

        List<AmenityDto> result = amenityService.getByType(ServiceType.MASSAGE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(ServiceType.MASSAGE);
    }

    // ── create / update / delete ─────────────────────────────────────────────────

    // create: thenAnswer имитирует Hibernate-генерацию id при INSERT.
    // isEqualByComparingTo — BigDecimal без учёта scale (2000 == 2000.00).
    @Test
    void create_savesAndReturnsDto() {
        when(amenityRepository.save(any())).thenAnswer(inv -> {
            Amenity a = inv.getArgument(0);
            a.setId(1L); // имитируем присвоение id базой данных
            return a;
        });

        AmenityDto result = amenityService.create(
                amenityRequest("Bath", ServiceType.BATH, new BigDecimal("2000"), 60));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Bath");
        assertThat(result.getType()).isEqualTo(ServiceType.BATH);
        assertThat(result.getDefaultPrice()).isEqualByComparingTo("2000");
    }

    // update: перезаписывает все поля; проверяем, что новые значения отражены в DTO.
    @Test
    void update_updatesFieldsAndReturnsDto() {
        when(amenityRepository.findById(1L))
                .thenReturn(Optional.of(amenity(1L, "Old Name", ServiceType.SAUNA)));
        when(amenityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0)); // возвращаем изменённый объект

        AmenityDto result = amenityService.update(1L,
                amenityRequest("New Sauna", ServiceType.SAUNA, new BigDecimal("2500"), 90));

        assertThat(result.getName()).isEqualTo("New Sauna");
        assertThat(result.getDefaultPrice()).isEqualByComparingTo("2500");
        assertThat(result.getMaxDurationMinutes()).isEqualTo(90);
    }

    @Test
    void update_notFound_throwsNoSuchElement() {
        when(amenityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> amenityService.update(99L,
                amenityRequest("X", ServiceType.POOL, BigDecimal.ONE, 30)))
                .isInstanceOf(NoSuchElementException.class);
    }

    // delete: сначала проверяет existsById → потом вызывает deleteById.
    // verify проверяет, что deleteById действительно был вызван с нужным аргументом.
    @Test
    void delete_callsRepository() {
        when(amenityRepository.existsById(1L)).thenReturn(true);

        amenityService.delete(1L);

        verify(amenityRepository).deleteById(1L);
    }

    // delete несуществующей услуги → NoSuchElementException ДО deleteById (deleteById не вызывается).
    @Test
    void delete_notFound_throwsNoSuchElement() {
        when(amenityRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> amenityService.delete(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── available / description ──────────────────────────────────────────────────

    // Проверяем, что поля description и available из AmenityRequest корректно переносятся в entity и DTO.
    @Test
    void create_setsAvailableAndDescription() {
        when(amenityRepository.save(any())).thenAnswer(inv -> {
            Amenity a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AmenityRequest req = amenityRequest("Sauna", ServiceType.SAUNA, new BigDecimal("2000"), 120);
        req.setDescription("Finnish sauna"); // дополнительное описание
        req.setAvailable(false);             // услуга недоступна (на обслуживании)

        AmenityDto result = amenityService.create(req);

        assertThat(result.getDescription()).isEqualTo("Finnish sauna");
        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void update_updatesAvailabilityAndDescription() {
        when(amenityRepository.findById(1L))
                .thenReturn(Optional.of(amenity(1L, "Sauna", ServiceType.SAUNA)));
        when(amenityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AmenityRequest req = amenityRequest("Sauna", ServiceType.SAUNA, new BigDecimal("2000"), 120);
        req.setDescription("Updated desc");
        req.setAvailable(false);

        AmenityDto result = amenityService.update(1L, req);

        assertThat(result.getDescription()).isEqualTo("Updated desc");
        assertThat(result.isAvailable()).isFalse();
    }

    // hasImage: маппинг imageData → булевый флаг для фронтенда.
    // Фронтенд проверяет dto.hasImage перед загрузкой изображения.
    @Test
    void getAll_mapsHasImageCorrectly() {
        Amenity withImage = amenity(1L, "Pool", ServiceType.POOL);
        withImage.setImageData(new byte[]{1, 2, 3}); // не null и не пустой массив
        Amenity noImage = amenity(2L, "Sauna", ServiceType.SAUNA); // imageData = null

        when(amenityRepository.findAll()).thenReturn(List.of(withImage, noImage));

        List<AmenityDto> result = amenityService.getAll();

        assertThat(result.get(0).isHasImage()).isTrue();  // есть байты → true
        assertThat(result.get(1).isHasImage()).isFalse(); // null → false
    }

    // ── image upload ─────────────────────────────────────────────────────────────

    // Успешная загрузка: метод должен сохранить bytes и MIME-тип в entity.
    // argThat(predicate) — verify с кастомным условием: проверяем поля сохранённого объекта.
    @Test
    void uploadImage_validFile_savesImageData() throws IOException {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        when(multipartFile.getSize()).thenReturn((long) imageBytes.length); // 4 байта < 2 МБ
        when(multipartFile.getBytes()).thenReturn(imageBytes);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(amenityRepository.findById(1L))
                .thenReturn(Optional.of(amenity(1L, "Sauna", ServiceType.SAUNA)));
        when(amenityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        amenityService.uploadImage(1L, multipartFile);

        // argThat: проверяем, что save() был вызван с entity, у которой правильные imageData и imageMimeType.
        verify(amenityRepository).save(argThat(a ->
                a.getImageData() != null && a.getImageData().length == 4 &&
                "image/jpeg".equals(a.getImageMimeType())));
    }

    // Файл 3 МБ > лимита 2 МБ → IllegalArgumentException до getBytes() (нет IO-операции).
    @Test
    void uploadImage_oversizedFile_throwsIllegalArgument() {
        when(multipartFile.getSize()).thenReturn(3L * 1024 * 1024); // 3 МБ

        assertThatThrownBy(() -> amenityService.uploadImage(1L, multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2MB");
    }

    // IOException при getBytes() → оборачивается в RuntimeException с понятным сообщением.
    // Сценарий: проблема с потоком чтения (редкая, но возможная ошибка).
    @Test
    void uploadImage_ioError_throwsRuntimeException() throws IOException {
        when(multipartFile.getSize()).thenReturn(100L); // прошло ограничение размера
        when(multipartFile.getBytes()).thenThrow(new IOException("disk error")); // но IO упала
        when(amenityRepository.findById(1L))
                .thenReturn(Optional.of(amenity(1L, "Sauna", ServiceType.SAUNA)));

        assertThatThrownBy(() -> amenityService.uploadImage(1L, multipartFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read image data");
    }

    // ── image retrieval ──────────────────────────────────────────────────────────

    // getImage: бинарные данные возвращаются с корректным Content-Type.
    @Test
    void getImage_withData_returnsOkResponse() {
        Amenity a = amenity(1L, "Sauna", ServiceType.SAUNA);
        a.setImageData(new byte[]{1, 2, 3});
        a.setImageMimeType("image/png");
        when(amenityRepository.findById(1L)).thenReturn(Optional.of(a));

        ResponseEntity<byte[]> response = amenityService.getImage(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
        assertThat(response.getHeaders().getContentType())
                .hasToString("image/png"); // ContentType установлен корректно
    }

    // Если imageData == null (изображение ещё не загружено) → 404.
    @Test
    void getImage_noData_returns404() {
        when(amenityRepository.findById(1L))
                .thenReturn(Optional.of(amenity(1L, "Sauna", ServiceType.SAUNA))); // imageData = null по умолчанию

        ResponseEntity<byte[]> response = amenityService.getImage(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Если imageMimeType == null → fallback "image/jpeg".
    // Страховка для старых записей без сохранённого MIME-типа.
    @Test
    void getImage_fallbackMimeType_usesJpeg() {
        Amenity a = amenity(1L, "Sauna", ServiceType.SAUNA);
        a.setImageData(new byte[]{1});
        a.setImageMimeType(null); // MIME-тип не сохранён

        when(amenityRepository.findById(1L)).thenReturn(Optional.of(a));

        ResponseEntity<byte[]> response = amenityService.getImage(1L);

        assertThat(response.getHeaders().getContentType())
                .hasToString("image/jpeg"); // применён fallback
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    // Минимальная заглушка Amenity: imageData = null, available = true (значение по умолчанию).
    private Amenity amenity(Long id, String name, ServiceType type) {
        return Amenity.builder()
                .id(id).name(name).type(type)
                .defaultPrice(new BigDecimal("1000"))
                .maxDurationMinutes(60)
                .build();
    }

    private AmenityRequest amenityRequest(String name, ServiceType type,
                                          BigDecimal price, int maxMinutes) {
        AmenityRequest req = new AmenityRequest();
        req.setName(name);
        req.setType(type);
        req.setDefaultPrice(price);
        req.setMaxDurationMinutes(maxMinutes);
        return req;
    }
}
