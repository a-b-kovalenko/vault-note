import type {
  ApiErrorResponse,
  CurrentUserResponse as OpenApiCurrentUserResponse,
  LoginRequest,
  LoginResponse as OpenApiLoginResponse,
  ValidationViolation,
} from '../api/generated';

export type { ApiErrorResponse, LoginRequest, ValidationViolation };

export type LoginApiResponse = OpenApiLoginResponse;

export type CurrentUserApiResponse = OpenApiCurrentUserResponse;

export type CurrentUserResponse = {
  userId: NonNullable<OpenApiCurrentUserResponse['user_id']>;
  roles: NonNullable<OpenApiCurrentUserResponse['roles']>;
};

/**
 * Application-facing login response. Its fields are derived from the
 * OpenAPI response model; the naming conversion stays at the API boundary.
 */
export type LoginResponse = {
  accessToken: OpenApiLoginResponse['access_token'];
  tokenType: OpenApiLoginResponse['token_type'];
  expiresIn: OpenApiLoginResponse['expires_in'];
};
