import type {
  ApiErrorResponse,
  LoginRequest,
  LoginResponse as OpenApiLoginResponse,
  ValidationViolation,
} from '../api/generated';

export type { ApiErrorResponse, LoginRequest, ValidationViolation };

export type LoginApiResponse = OpenApiLoginResponse;

/**
 * Application-facing login response. Its fields are derived from the
 * OpenAPI response model; the naming conversion stays at the API boundary.
 */
export type LoginResponse = {
  accessToken: OpenApiLoginResponse['access_token'];
  tokenType: OpenApiLoginResponse['token_type'];
  expiresIn: OpenApiLoginResponse['expires_in'];
};
