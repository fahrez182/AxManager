// GitHub API reads shared by the download page and the home page, made in the browser.
// Unauthenticated calls get 60 an hour per IP, so each answer is kept for a while and reused
// across pages and reloads; a call in flight is shared too.

export const OWNER = "fahrez182";
export const REPO = `${OWNER}/AxManager`;
export const REPO_PAGE = `https://github.com/${REPO}`;
export const RELEASES_PAGE = `${REPO_PAGE}/releases`;

const API = "https://api.github.com";
const CACHE_MS = 10 * 60 * 1000;

export interface Asset {
  name: string;
  size: number;
  download_count: number;
  browser_download_url: string;
}
export interface Release {
  tag_name: string;
  name: string;
  html_url: string;
  prerelease: boolean;
  draft: boolean;
  published_at: string;
  body_html?: string;
  assets: Asset[];
}
export interface Repo {
  stargazers_count: number;
  forks_count: number;
  license?: { spdx_id: string };
}
export interface User {
  login: string;
  name?: string;
  bio?: string;
  followers: number;
  public_repos: number;
}

const inFlight = new Map<string, Promise<unknown>>();

async function cachedJson<T>(path: string, accept = "application/vnd.github+json"): Promise<T> {
  const key = `ax-gh:${path}`;
  try {
    const cached = JSON.parse(sessionStorage.getItem(key) ?? "null");
    if (cached && Date.now() - cached.at < CACHE_MS) return cached.data;
  } catch {}
  let pending = inFlight.get(key) as Promise<T> | undefined;
  if (!pending) {
    pending = fetch(`${API}${path}`, { headers: { Accept: accept } })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json() as Promise<T>;
      })
      .then((data) => {
        try {
          sessionStorage.setItem(key, JSON.stringify({ at: Date.now(), data }));
        } catch {}
        return data;
      })
      .finally(() => inFlight.delete(key));
    inFlight.set(key, pending);
  }
  return pending;
}

/** Published releases, newest first; body_html is the notes as GitHub renders (and sanitizes) them. */
export async function releases(): Promise<Release[]> {
  const list = await cachedJson<Release[]>(`/repos/${REPO}/releases?per_page=100`, "application/vnd.github.html+json");
  return list.filter((r) => !r.draft);
}

export function repo(): Promise<Repo> {
  return cachedJson<Repo>(`/repos/${REPO}`);
}

export function author(): Promise<User> {
  return cachedJson<User>(`/users/${OWNER}`);
}

/** The newest release that isn't a pre-release, or the newest of all if every one is. */
export function latestOf(list: Release[]): Release | undefined {
  return list.find((r) => !r.prerelease) ?? list[0];
}
