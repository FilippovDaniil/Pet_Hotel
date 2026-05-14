// Barrel-файл: реэкспортирует все типы из одного места.
// Компоненты импортируют из '../types' вместо '../types/booking', '../types/auth' и т.д.
// Это упрощает рефакторинг — если тип переместится в другой файл, менять нужно только здесь.
export * from './auth'    // Role, AuthResponse, LoginRequest, RegisterRequest, Customer
export * from './room'    // Room, RoomRequest, RoomClass
export * from './booking' // Booking, BookingRequest, BookingStatus + константы Labels/Colors
export * from './amenity' // Amenity, ServiceType + метки
export * from './dining'  // MenuItem, Order, DeliveryType
export * from './billing' // Invoice, InvoiceStatus
export * from './support' // SupportMessage, ConversationSummary
