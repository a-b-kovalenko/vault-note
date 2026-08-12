export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginApiResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
}
