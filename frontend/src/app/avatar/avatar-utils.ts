import { UserProfile } from '../auth/auth.models';

export function getInitials(profile: UserProfile | null): string {
  const value = profile?.displayName.trim() || profile?.email.split('@')[0] || '?';
  const words = value.split(/\s+/).filter(Boolean);

  if (words.length > 1) {
    return `${words[0][0]}${words.at(-1)?.[0] ?? ''}`.toUpperCase();
  }

  return value.slice(0, 2).toUpperCase();
}
