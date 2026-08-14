export interface Product {
  id: string;
  title: string;
  description: string;
  price: number;
  discountPrice?: number;
  imageUrl: string;
  galleryImages: string[];
  rating: number;
  reviewCount: number;
  categoryId: string;
  isAvailable: boolean;
  availableQuantity: number;
  sizes?: string[];
  colors?: string[];
}

export interface Subcategory {
  id: string;
  name: string;
  categoryId: string;
}

export interface Category {
  id: string;
  name: string;
  imageUrl: string;
  subcategories: Subcategory[];
}

export interface Banner {
  id: string;
  title: string;
  subtitle: string;
  imageUrl: string;
  targetCategoryId?: string;
}

export interface ShippingAddress {
  fullName: string;
  phone: string;
  city: string;
  addressLine: string;
}

export type DeliveryMethod = "STANDARD" | "EXPRESS";

export interface QuoteItemRequest {
  productId: string;
  quantity: number;
}

export interface CheckoutQuoteRequest {
  items: QuoteItemRequest[];
  shippingAddress: ShippingAddress;
  deliveryMethod?: DeliveryMethod;
}

export interface QuoteItemSnapshot {
  productId: string;
  title: string;
  unitPrice: number;
  quantity: number;
  totalPrice: number;
}

export interface CheckoutQuote {
  quoteId: string;
  items: QuoteItemSnapshot[];
  itemSubtotal: number;
  discountTotal: number;
  deliveryFee: number;
  finalTotal: number;
  currency: string;
  quoteExpiry: string;
  deliveryMethod: DeliveryMethod;
}

export type MockPaymentMethod = "MOCK_CARD" | "CASH_ON_DELIVERY";

export interface CreateOrderRequest {
  quoteId: string;
  mockPaymentMethod: MockPaymentMethod;
  shippingAddress: ShippingAddress;
}

export interface Order {
  orderId: string;
  orderReference: string;
  quoteId: string;
  items: QuoteItemSnapshot[];
  itemSubtotal: number;
  discountTotal: number;
  deliveryMethod: DeliveryMethod;
  deliveryFee: number;
  finalTotal: number;
  currency: string;
  shippingAddress: ShippingAddress;
  mockPaymentMethod: MockPaymentMethod;
  mockPaymentStatus: "SIMULATED_SUCCESS";
  status: "CONFIRMED";
  createdAt: string;
}

export type ErrorCode =
  | "VALIDATION_ERROR"
  | "PRODUCT_NOT_FOUND"
  | "OUT_OF_STOCK"
  | "PRICE_CHANGED"
  | "QUOTE_EXPIRED"
  | "CHECKOUT_CHANGED"
  | "DUPLICATE_REQUEST"
  | "SERVICE_UNAVAILABLE"
  | "INVALID_CREDENTIALS"
  | "EMAIL_ALREADY_EXISTS"
  | "UNAUTHORIZED";

export interface ApiErrorResponse {
  status: "error";
  code: ErrorCode;
  message: string;
}

export interface User {
  id: string;
  fullName: string;
  email: string;
  mobileNumber?: string;
  address?: string;
  createdAt: string;
}

export interface UserRecord extends User {
  passwordHash: string;
}

export interface AuthSession {
  tokenHash: string;
  userId: string;
  createdAt: string;
  expiresAt: string;
}

export interface SignUpRequest {
  fullName: string;
  email: string;
  password: string;
  mobileNumber?: string;
  address?: string;
}

export interface SignInRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  user: User;
  token: string;
}
