package com.pethotel.dining.entity;

// Способ доставки заказа из буфета.
// Хранится как строка в колонке delivery_type (EnumType.STRING в Order).
public enum DeliveryType {
    ROOM_DELIVERY,  // доставка в номер
    DINING_ROOM     // самовывоз из ресторана / обеденного зала
}
