import type {
  ApiErrorResponse,
  LoginRequest,
  LoginResponse as OpenApiLoginResponse,
  RegisterUserResponse as OpenApiRegisterUserResponse,
  UpdateUserProfileRequest as OpenApiUpdateUserProfileRequest,
  UserProfileDto as OpenApiUserProfileDto,
  ValidationViolation,
} from '../api/generated';

export type { ApiErrorResponse, LoginRequest, ValidationViolation };

/**
 * The generated schema exposes the login response in camelCase, while the
 * runtime API uses the application's snake_case wire format.
 */
export type LoginApiResponse = Omit<
  OpenApiLoginResponse,
  'accessToken' | 'tokenType' | 'expiresIn'
> & {
  access_token: OpenApiLoginResponse['accessToken'];
  token_type: OpenApiLoginResponse['tokenType'];
  expires_in: OpenApiLoginResponse['expiresIn'];
};

/**
 * The generated schema currently exposes the profile fields in camelCase,
 * while the runtime API uses the application's snake_case wire format.
 */
export type UserProfileApiResponse = Omit<
  OpenApiUserProfileDto,
  'displayName' | 'emailVerified'
> & {
  display_name?: OpenApiUserProfileDto['displayName'];
  email_verified?: OpenApiUserProfileDto['emailVerified'];
};

export type UserProfile = {
  id: NonNullable<OpenApiUserProfileDto['id']>;
  email: NonNullable<OpenApiUserProfileDto['email']>;
  displayName: NonNullable<UserProfileApiResponse['display_name']>;
  emailVerified: NonNullable<UserProfileApiResponse['email_verified']>;
  roles: NonNullable<OpenApiUserProfileDto['roles']>;
};

export type UpdateUserProfileRequest = OpenApiUpdateUserProfileRequest;

export type RegisterUserRequest = {
  email: string;
  displayName: string;
  password: string;
};

export type PasswordResetRequest = {
  email: string;
};

export type PasswordResetConfirmRequest = {
  token: string;
  newPassword: string;
};

export type RegisterUserApiResponse = OpenApiRegisterUserResponse;

export type RegisterUserResponse = {
  userId: NonNullable<OpenApiRegisterUserResponse['userId']>;
};

/**
 * Application-facing login response. Its fields are derived from the
 * OpenAPI response model; the naming conversion stays at the API boundary.
 */
export type LoginResponse = {
  accessToken: LoginApiResponse['access_token'];
  tokenType: LoginApiResponse['token_type'];
  expiresIn: LoginApiResponse['expires_in'];
};
